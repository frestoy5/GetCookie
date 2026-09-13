package top.aryun.token.ui

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import top.aryun.token.data.CapturedToken
import top.aryun.token.data.CustomSite
import top.aryun.token.data.SiteDraft
import top.aryun.token.data.SiteTarget
import top.aryun.token.data.TokenRecord
import top.aryun.token.data.TokenStore
import top.aryun.token.data.customSiteTarget
import top.aryun.token.data.draftTarget
import top.aryun.token.data.normalizeUrl
import java.util.UUID

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TokenStore(context) }
    val snackbarHost = remember { SnackbarHostState() }

    var records by remember { mutableStateOf(store.loadRecords()) }
    var customs by remember { mutableStateOf(store.loadCustomSites()) }
    var tab by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<SiteTarget?>(null) }
    var draft by remember { mutableStateOf<SiteDraft?>(null) }
    var result by remember { mutableStateOf<CapturedToken?>(null) }
    var detail by remember { mutableStateOf<TokenRecord?>(null) }
    var returnToDraft by remember { mutableStateOf(false) }

    fun message(text: String) {
        scope.launch { snackbarHost.showSnackbar(text) }
    }

    fun persistRecords(next: List<TokenRecord>) {
        records = next
        store.saveRecords(next)
    }

    fun persistCustoms(next: List<CustomSite>) {
        customs = next
        store.saveCustomSites(next)
    }

    fun copyText(label: String, text: String) {
        copyToClipboard(context, label, text)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) message("已复制到剪贴板")
    }

    fun saveToLocal(record: TokenRecord) {
        val path = store.exportToFile(record)
        store.exportAll(records)
        message(if (path == null) "保存失败，请检查存储空间" else "已保存到本地：$path")
    }

    fun storeToken(captured: CapturedToken) {
        val record = TokenRecord(
            id = UUID.randomUUID().toString(),
            site = captured.site,
            url = captured.url,
            key = captured.key,
            value = captured.value,
            createdAt = System.currentTimeMillis(),
        )
        persistRecords(TokenStore.upsert(records, record))
    }

    fun handleCaptured(captured: CapturedToken) {
        storeToken(captured)
        if (returnToDraft) {
            draft = draft?.copy(token = captured)
            returnToDraft = false
        } else {
            result = captured
        }
        target = null
    }

    val currentTarget = target
    if (currentTarget != null) {
        BrowserScreen(
            target = currentTarget,
            snackbarHost = snackbarHost,
            onClose = {
                target = null
                returnToDraft = false
            },
            onCaptured = { handleCaptured(it) },
            onMessage = { message(it) },
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("首页") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("我的") },
                    )
                }
            },
            floatingActionButton = {
                if (tab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { draft = SiteDraft(UUID.randomUUID().toString(), "", "", null) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("新增网站") },
                    )
                }
            },
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(
                    customs = customs,
                    insets = padding,
                    onConnect = { target = it },
                    onAddCustom = { draft = SiteDraft(UUID.randomUUID().toString(), "", "", null) },
                    onRemoveCustom = { site ->
                        persistCustoms(customs.filterNot { it.id == site.id })
                        message("已删除「${site.name}」")
                    },
                )

                else -> MineScreen(
                    records = records,
                    insets = padding,
                    onCopy = { copyText(it.key, it.value) },
                    onSave = { saveToLocal(it) },
                    onDelete = { record ->
                        persistRecords(records.filterNot { item -> item.id == record.id })
                        message("已删除「${record.site}」的 Token")
                    },
                    onOpenDetail = { detail = it },
                    onExportAll = {
                        val path = store.exportAll(records)
                        message(if (path == null) "导出失败" else "已导出全部 Token：$path")
                    },
                )
            }
        }
    }

    val currentDraft = draft
    if (currentDraft != null && currentTarget == null) {
        AddCustomSiteDialog(
            draft = currentDraft,
            onNameChange = { draft = currentDraft.copy(name = it) },
            onUrlChange = { draft = currentDraft.copy(url = it) },
            onConnect = {
                val url = normalizeUrl(currentDraft.url)
                if (url == null) {
                    message("请先填写正确的网页登录链接")
                } else {
                    returnToDraft = true
                    target = draftTarget(currentDraft.copy(url = url))
                }
            },
            onCopyToken = { copyText("token", it) },
            onConfirm = {
                val url = normalizeUrl(currentDraft.url)
                if (url != null && currentDraft.name.isNotBlank()) {
                    val site = CustomSite(
                        id = currentDraft.id.ifBlank { UUID.randomUUID().toString() },
                        name = currentDraft.name.trim(),
                        url = url,
                    )
                    persistCustoms(listOf(site) + customs.filterNot { it.id == site.id })
                    draft = null
                    returnToDraft = false
                    message("已添加「${site.name}」")
                }
            },
            onDismiss = {
                draft = null
                returnToDraft = false
            },
        )
    }

    val currentResult = result
    if (currentResult != null) {
        TokenResultDialog(
            token = currentResult,
            onDismiss = { result = null },
            onCopy = { copyText(currentResult.key, it) },
            onSave = {
                val record = records.firstOrNull {
                    it.site == currentResult.site && it.key.equals(currentResult.key, ignoreCase = true)
                }
                if (record == null) message("保存失败") else saveToLocal(record)
            },
        )
    }

    val currentDetail = detail
    if (currentDetail != null) {
        RecordDetailDialog(
            record = currentDetail,
            onDismiss = { detail = null },
            onCopy = { copyText(currentDetail.key, currentDetail.value) },
            onSave = { saveToLocal(currentDetail) },
            onDelete = {
                persistRecords(records.filterNot { it.id == currentDetail.id })
                detail = null
                message("已删除「${currentDetail.site}」的 Token")
            },
        )
    }
}
