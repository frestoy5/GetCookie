package top.aryun.token.data

import java.util.UUID

/** 自定义网站默认尝试的字段名。 */
val CUSTOM_SITE_KEYS: List<String> = listOf(
    "token", "access_token", "accessToken", "refresh_token", "refreshToken",
    "auth_token", "authToken", "id_token", "idToken", "Authorization", "authorization",
    "jwt", "ticket", "sso", "sso_token", "user_token", "api_token", "apiToken",
    "session", "sessionid", "session_id", "sid", "SID", "SESSDATA", "cookie",
    "uid", "uin", "userid", "userId", "user_id", "bearer", "Bearer",
)

/** 内置站点。 */
val SITE_PRESETS: List<SiteTarget> = listOf(
    SiteTarget(
        id = "netease",
        name = "网易云音乐",
        url = "https://music.163.com/",
        accent = 0xFFC20C0C,
        badge = "网",
        keys = listOf("MUSIC_U", "MUSIC_A", "__csrf", "NMTID", "_ntes_nuid", "JSESSIONID-WYYY"),
        // 登录后才会出现，用来判断登录是否完成
        strongKeys = listOf("MUSIC_U", "MUSIC_A"),
    ),
    SiteTarget(
        id = "kugou",
        name = "酷狗音乐",
        url = "https://www.kugou.com/",
        accent = 0xFF2B7CE5,
        badge = "酷",
        keys = listOf("token", "KuGoo", "userid", "vip_token", "kg_mid", "kg_dfid", "UserName"),
        strongKeys = listOf("KuGoo", "userid", "vip_token"),
    ),
    SiteTarget(
        id = "qqmusic",
        name = "QQ音乐",
        url = "https://y.qq.com/",
        accent = 0xFF31C27C,
        badge = "Q",
        keys = listOf(
            "qm_keyst", "qqmusic_key", "qqmusic_uin", "psrf_qqunionid",
            "wxuin", "uin", "p_uin", "p_skey", "skey",
        ),
        strongKeys = listOf("qm_keyst", "qqmusic_key", "qqmusic_uin"),
    ),
    SiteTarget(
        id = "bilibili",
        name = "哔哩哔哩",
        url = "https://passport.bilibili.com/login",
        accent = 0xFF00A1D6,
        badge = "B",
        keys = listOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5", "sid", "buvid3"),
        strongKeys = listOf("SESSDATA", "bili_jct", "DedeUserID"),
    ),
    SiteTarget(
        id = "douyin",
        name = "抖音",
        url = "https://www.douyin.com/",
        accent = 0xFFFE2C55,
        badge = "抖",
        keys = listOf(
            "sessionid", "sessionid_ss", "sid_tt", "sid_guard", "uid_tt",
            "uid_tt_ss", "passport_csrf_token", "ttwid", "odin_tt",
        ),
        strongKeys = listOf("sessionid", "sessionid_ss", "sid_tt", "sid_guard"),
    ),
    SiteTarget(
        id = "kuaishou",
        name = "快手",
        url = "https://www.kuaishou.com/",
        accent = 0xFFFF5A1E,
        badge = "快",
        keys = listOf(
            "kuaishou.server.web_st", "kuaishou.server.web_ph",
            "kuaishou.server.webdaytime", "kpn", "did", "userId", "kuaishou.web.cp.api_st",
        ),
        strongKeys = listOf("kuaishou.server.web_st", "userId"),
    ),
    SiteTarget(
        id = "youtube",
        name = "YouTube",
        url = "https://www.youtube.com/",
        accent = 0xFFFF0000,
        badge = "Y",
        keys = listOf(
            "LOGIN_INFO", "SID", "HSID", "SSID", "SAPISID", "APISID", "__Secure-1PSID",
            "__Secure-3PSID", "__Secure-1PAPISID", "__Secure-3PAPISID", "SIDCC",
        ),
        strongKeys = listOf("LOGIN_INFO", "SID", "HSID", "SSID"),
    ),
    SiteTarget(
        id = "pan123",
        name = "123云盘",
        url = "https://www.123pan.com/",
        accent = 0xFF1E88E5,
        badge = "1",
        keys = listOf(
            "token", "authorization", "Authorization", "access_token", "accessToken",
            "Login-Token", "loginToken", "passport",
        ),
        strongKeys = listOf("token", "authorization", "access_token"),
    ),
)

/** 内置站点里出现过的自定义默认色。 */
const val CUSTOM_SITE_ACCENT: Long = 0xFF7A5C3E
const val CUSTOM_SITE_BADGE: String = "自"

fun customSiteTarget(site: CustomSite): SiteTarget = SiteTarget(
    id = site.id,
    name = site.name,
    url = site.url,
    accent = CUSTOM_SITE_ACCENT,
    badge = CUSTOM_SITE_BADGE,
    keys = CUSTOM_SITE_KEYS,
)

fun draftTarget(draft: SiteDraft): SiteTarget = SiteTarget(
    id = draft.id.ifBlank { UUID.randomUUID().toString() },
    name = draft.name.ifBlank { "自定义网站" },
    url = draft.url,
    accent = CUSTOM_SITE_ACCENT,
    badge = CUSTOM_SITE_BADGE,
    keys = CUSTOM_SITE_KEYS,
)

/** 补齐协议头，返回 null 表示无法识别的地址。 */
fun normalizeUrl(input: String): String? {
    val raw = input.trim()
    if (raw.isEmpty()) return null
    val withScheme = when {
        raw.startsWith("http://", true) || raw.startsWith("https://", true) -> raw
        raw.startsWith("//") -> "https:$raw"
        else -> "https://$raw"
    }
    val host = withScheme
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
    if (host.isEmpty() || !host.contains('.')) return null
    return withScheme
}

fun hostOf(url: String): String = url
    .substringAfter("://", url)
    .substringBefore('/')
    .substringBefore('?')
