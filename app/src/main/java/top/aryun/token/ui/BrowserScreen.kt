package top.aryun.token.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import top.aryun.token.data.CapturedToken
import top.aryun.token.data.Harvest
import top.aryun.token.data.SiteTarget
import top.aryun.token.data.TokenExtractor
import top.aryun.token.data.hostOf

private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    target: SiteTarget,
    snackbarHost: SnackbarHostState,
    onClose: () -> Unit,
    onCaptured: (CapturedToken) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var desktop by remember { mutableStateOf(false) }
    var autoGet by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var settled by remember { mutableStateOf(false) }
    var pageUrl by remember { mutableStateOf(target.url) }

    val webState = remember { mutableStateOf<WebView?>(null) }
    val popupState = remember { mutableStateOf<WebView?>(null) }
    // 强字段名 -> 归一化后的值，首页首次采集时确定，之后只用来判断“是否变了”
    val baseline = remember { mutableStateOf<Map<String, String>?>(null) }
    val finished = remember { mutableStateOf(false) }

    val mobileUa = remember { WebSettings.getDefaultUserAgent(context).replace("; wv", "") }

    fun closePopup() {
        val popup = popupState.value
        popupState.value = null
        if (popup != null) {
            (popup.parent as? ViewGroup)?.removeView(popup)
            runCatching {
                popup.stopLoading()
                popup.destroy()
            }
        }
    }

    fun fire(token: CapturedToken) {
        if (finished.value) return
        finished.value = true
        onCaptured(token)
    }

    fun runCapture(manual: Boolean) {
        val web = webState.value ?: return
        val urls = listOfNotNull(web.url, target.url).distinct()
        harvest(web, urls, target.keys, deep = manual) { result ->
            if (manual) {
                val found = TokenExtractor.resolve(target, result, allowFallback = true)
                if (found == null) {
                    onMessage("没有找到 Token，请确认已经登录完成")
                } else {
                    fire(found)
                }
                return@harvest
            }

            val hits = TokenExtractor.strongHits(target, result)
            val base = baseline.value
            if (base == null) {
                // 第一次采集：记下当前状态
                baseline.value = hits.associate { it.key.lowercase() to it.value }
                // 如果最强字段已经存在，说明本来就已经登录了，直接返回
                val primary = target.strongKeys.firstOrNull()
                val alreadyLoggedIn = hits.firstOrNull { primary != null && it.key.equals(primary, true) }
                if (alreadyLoggedIn != null) {
                    fire(TokenExtractor.tokenFor(target, alreadyLoggedIn.key, alreadyLoggedIn.value, result))
                }
                return@harvest
            }

            // 之后只要任一强字段是新出现的或者值变了，就认为刚刚登录成功
            val changed = hits.firstOrNull { base[it.key.lowercase()] != it.value }
            if (changed != null) {
                fire(TokenExtractor.tokenFor(target, changed.key, changed.value, result))
            }
        }
    }

    BackHandler {
        if (popupState.value != null) {
            closePopup()
            return@BackHandler
        }
        val web = webState.value
        if (web != null && web.canGoBack()) web.goBack() else onClose()
    }

    LaunchedEffect(loading) {
        if (!loading) {
            delay(1200L)
            settled = true
        }
    }

    LaunchedEffect(autoGet, settled) {
        if (!autoGet || !settled) return@LaunchedEffect
        while (true) {
            delay(1500L)
            if (finished.value) break
            runCapture(manual = false)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = hostOf(pageUrl),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { runCapture(manual = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "获取 Token")
                    }
                    TextButton(onClick = { desktop = !desktop }) {
                        if (desktop) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text("电脑模式")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LinearProgressIndicator(
                progress = { if (loading) (progress.coerceIn(0, 100)) / 100f else 0f },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "登录完成后自动获取",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = autoGet, onCheckedChange = { autoGet = it })
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val web = WebView(ctx)
                        web.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        web.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = mobileUa
                        }
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(web, true)
                        }
                        web.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val uri = request.url
                                val scheme = uri.scheme?.lowercase()
                                if (scheme == "http" || scheme == "https") return false
                                return runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }.isSuccess
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                loading = true
                                url?.let { pageUrl = it }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                loading = false
                                url?.let { pageUrl = it }
                                CookieManager.getInstance().flush()
                            }
                        }
                        web.webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                                loading = newProgress < 100
                            }

                            override fun onCreateWindow(
                                view: WebView,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message,
                            ): Boolean {
                                val popup = WebView(view.context)
                                popup.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    setSupportMultipleWindows(true)
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = view.settings.userAgentString
                                }
                                CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true)
                                popup.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        popupView: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean {
                                        val scheme = request.url.scheme?.lowercase()
                                        if (scheme == "http" || scheme == "https") return false
                                        closePopup()
                                        return true
                                    }
                                }
                                popup.webChromeClient = object : WebChromeClient() {
                                    override fun onCloseWindow(window: WebView) {
                                        closePopup()
                                    }
                                }
                                (resultMsg.obj as? WebView.WebViewTransport)?.webView = popup
                                resultMsg.sendToTarget()
                                popupState.value = popup
                                return true
                            }
                        }
                        web.loadUrl(target.url)
                        webState.value = web
                        web
                    },
                    onRelease = { web ->
                        // 关掉页面后不再接受迟到的 JS 回调，避免误关掉新开的会话
                        finished.value = true
                        webState.value = null
                        runCatching {
                            web.stopLoading()
                            web.loadUrl("about:blank")
                            web.destroy()
                        }
                    },
                    update = { web ->
                        val wanted = if (desktop) DESKTOP_UA else mobileUa
                        if (web.settings.userAgentString != wanted) {
                            web.settings.userAgentString = wanted
                            web.settings.useWideViewPort = true
                            web.settings.loadWithOverviewMode = !desktop
                            web.setInitialScale(if (desktop) 100 else 0)
                            web.reload()
                        }
                    },
                )

                if (popupState.value != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            }
                        },
                        update = { frame ->
                            val popup = popupState.value
                            if (popup != null && popup.parent !== frame) {
                                (popup.parent as? ViewGroup)?.removeView(popup)
                                frame.addView(
                                    popup,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                    ),
                                )
                            }
                        },
                        onRelease = { frame -> frame.removeAllViews() },
                    )

                    Surface(
                        onClick = { closePopup() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭弹出的页面",
                            modifier = Modifier.padding(9.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 读取 Cookie（含 HttpOnly）再取页面里的存储，合并后回调。 */
private fun harvest(
    web: WebView,
    urls: List<String>,
    keys: List<String>,
    deep: Boolean,
    onResult: (Harvest) -> Unit,
) {
    val manager = CookieManager.getInstance()
    val native = LinkedHashMap<String, String>()
    urls.forEach { url ->
        TokenExtractor.parseCookieHeader(manager.getCookie(url)).forEach { (key, value) ->
            native[key] = value
        }
    }
    web.evaluateJavascript(TokenExtractor.collectJs(keys, deep)) { raw ->
        onResult(TokenExtractor.harvest(raw, native))
    }
}
