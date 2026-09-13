package top.aryun.token.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.aryun.token.data.CapturedToken
import top.aryun.token.data.SiteDraft
import top.aryun.token.data.TokenRecord
import top.aryun.token.data.TokenStore
import top.aryun.token.data.normalizeUrl

private val DialogShape
    @Composable get() = MaterialTheme.shapes.extraLarge

@Composable
private fun DialogSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.93f)
            .wrapContentHeight()
            .imePadding(),
        shape = DialogShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = content,
    )
}

@Composable
fun TokenResultDialog(
    token: CapturedToken,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onSave: () -> Unit,
) {
    var showAll by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogSurface {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("获取成功", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SiteBadge(badge = badgeFor(token.site), accent = accentFor(token.site), size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(token.site, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "字段：${token.key}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "获取的 Token",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))

                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        TokenValueBox(token.value)
                    }
                }

                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "收起全部字段" else "查看全部字段（${token.pairs.size}）")
                }

                if (showAll) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 170.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Column {
                            token.pairs.entries.forEach { (key, value) ->
                                Text(
                                    text = "$key = $value",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(3.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSave) { Text("保存到本地") }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = { onCopy(token.value) }) { Text("复制") }
                }
            }
        }
    }
}

@Composable
fun RecordDetailDialog(
    record: TokenRecord,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogSurface {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(record.site, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${TokenStore.formatTime(record.createdAt)} · ${record.url}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "获取的 Token · ${record.key}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))

                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        TokenValueBox(record.value)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSave) { Text("保存到本地") }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = onCopy) { Text("复制") }
                }
            }
        }
    }
}

@Composable
fun AddCustomSiteDialog(
    draft: SiteDraft,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
    onCopyToken: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val normalized = normalizeUrl(draft.url)
    val urlInvalid = draft.url.isNotBlank() && normalized == null
    val canConfirm = draft.name.isNotBlank() && normalized != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogSurface {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("新增网站", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "填写备注和网页登录链接，就能像内置站点一样获取 Token",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("备注") },
                    placeholder = { Text("例如：我的网站") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft.url,
                        onValueChange = onUrlChange,
                        label = { Text("网页登录链接") },
                        placeholder = { Text("https://example.com/login") },
                        singleLine = true,
                        isError = urlInvalid,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = onConnect, enabled = normalized != null) { Text("连接") }
                }

                if (urlInvalid) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "链接格式不正确，请填写完整域名，例如 https://example.com/login",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                val captured = draft.token
                if (captured != null) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("已获取的 Token", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(2.dp))
                            Text("字段：${captured.key}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            SelectionContainer {
                                Box(
                                    modifier = Modifier
                                        .heightIn(max = 120.dp)
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    TokenValueBox(captured.value)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { onCopyToken(captured.value) }) { Text("复制") }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = onConfirm, enabled = canConfirm) { Text("确定") }
                }
            }
        }
    }
}
