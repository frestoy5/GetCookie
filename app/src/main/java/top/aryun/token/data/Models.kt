package top.aryun.token.data

/** 一个可以获取 Token 的站点（内置或自定义）。 */
data class SiteTarget(
    val id: String,
    val name: String,
    val url: String,
    val accent: Long,
    val badge: String,
    val keys: List<String>,
    /** 只在登录后才出现的字段，用于「登录后自动返回」，避免把访客 Cookie 当成 Token。 */
    val strongKeys: List<String> = keys.take(2),
)

/** 用户自己添加的网站。 */
data class CustomSite(
    val id: String,
    val name: String,
    val url: String,
)

/** 我的页面里保存的一条 Token。 */
data class TokenRecord(
    val id: String,
    val site: String,
    val url: String,
    val key: String,
    val value: String,
    val createdAt: Long,
)

/** 一次采集结果：Cookie、localStorage、sessionStorage。 */
data class Harvest(
    val cookie: Map<String, String>,
    val local: Map<String, String>,
    val session: Map<String, String>,
) {
    val merged: Map<String, String> = LinkedHashMap<String, String>().apply {
        putAll(cookie)
        putAll(session)
        putAll(local)
    }

    val cookieHeader: String
        get() = cookie.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

/** 成功抓到的 Token。 */
data class CapturedToken(
    val site: String,
    val url: String,
    val key: String,
    val value: String,
    val pairs: Map<String, String>,
)

/** 首页“新增网站”对话框的临时草稿。 */
data class SiteDraft(
    val id: String,
    val name: String,
    val url: String,
    val token: CapturedToken?,
)
