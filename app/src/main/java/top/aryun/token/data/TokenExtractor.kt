package top.aryun.token.data

import org.json.JSONArray
import org.json.JSONObject

/** 在页面里发现的一个“登录后才会出现”的字段。 */
data class StrongHit(val key: String, val value: String)

object TokenExtractor {

    /**
     * 生成采集脚本。
     *
     * [deep] 为 false 时只回传 [keys] 里关心的字段（自动获取用，避免每次轮询
     * 把整个 localStorage 搬到主线程）；为 true 时全量回传（手动获取用，方便模糊兜底）。
     */
    fun collectJs(keys: List<String>, deep: Boolean): String {
        val keysJson = JSONArray(keys).toString()
        return """(function(){
  var KEYS = $keysJson;
  var DEEP = $deep;
  var want = {};
  for (var i = 0; i < KEYS.length; i++) { want[String(KEYS[i]).toLowerCase()] = KEYS[i]; }
  var out = { c: {}, l: {}, s: {} };
  function push(bag, name, value) {
    if (name == null || value == null) { return; }
    var lower = String(name).toLowerCase();
    if (!lower.length) { return; }
    if (DEEP || Object.prototype.hasOwnProperty.call(want, lower)) {
      bag[Object.prototype.hasOwnProperty.call(want, lower) ? want[lower] : String(name)] = String(value);
    }
  }
  try {
    var raw = document.cookie || '';
    var parts = raw.split(';');
    for (var i = 0; i < parts.length; i++) {
      var p = parts[i];
      var idx = p.indexOf('=');
      if (idx > 0) { push(out.c, p.substring(0, idx).trim(), p.substring(idx + 1).trim()); }
    }
  } catch (e) {}
  try {
    for (var i = 0; i < localStorage.length; i++) {
      var k = localStorage.key(i);
      try { push(out.l, k, localStorage.getItem(k)); } catch (e2) {}
    }
  } catch (e) {}
  try {
    for (var i = 0; i < sessionStorage.length; i++) {
      var k = sessionStorage.key(i);
      try { push(out.s, k, sessionStorage.getItem(k)); } catch (e2) {}
    }
  } catch (e) {}
  return out;
})()"""
    }

    /** 解析 "a=1; b=2" 形式的 Cookie 头，忽略没有名字的片段。 */
    fun parseCookieHeader(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        raw.split(';').forEach { part ->
            val i = part.indexOf('=')
            if (i > 0) {
                val name = part.substring(0, i).trim()
                if (name.isNotEmpty()) out[name] = part.substring(i + 1).trim()
            }
        }
        return out
    }

    /** 把 evaluateJavascript 的返回值还原成三张表。 */
    fun harvest(jsResult: String?, nativeCookies: Map<String, String>): Harvest {
        val root = jsResult?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        val jsCookies = root?.optJSONObject("c").toStringMap()
        val local = root?.optJSONObject("l").toStringMap() ?: emptyMap()
        val session = root?.optJSONObject("s").toStringMap() ?: emptyMap()
        return Harvest(
            cookie = LinkedHashMap<String, String>().apply {
                putAll(jsCookies)
                putAll(nativeCookies)
            },
            local = local,
            session = session,
        )
    }

    /** localStorage 里的值有时是带引号的 JSON 字符串，这里脱掉一层并去掉首尾空白。 */
    fun normalizeValue(value: String): String {
        val trimmed = value.trim()
        val unwrapped = if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            runCatching { JSONObject("{\"v\":$trimmed}").getString("v") }.getOrDefault(trimmed)
        } else {
            trimmed
        }
        return unwrapped.trim()
    }

    /**
     * 找出所有「登录后才会出现」的字段。自动获取用它判断登录是否完成。
     *
     * 返回顺序与 [SiteTarget.strongKeys] 一致，第一个就是最能代表登录状态的字段。
     */
    fun strongHits(target: SiteTarget, harvest: Harvest): List<StrongHit> {
        val merged = harvest.merged
        return target.strongKeys.mapNotNull { key ->
            val entry = merged.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
                ?: return@mapNotNull null
            val value = normalizeValue(entry.value)
            if (value.isBlank()) null else StrongHit(entry.key, value)
        }
    }

    /** 由 [StrongHit] 组装出结果。 */
    fun tokenFor(target: SiteTarget, key: String, value: String, harvest: Harvest): CapturedToken =
        CapturedToken(
            site = target.name,
            url = target.url,
            key = key,
            value = value,
            pairs = harvest.merged,
        )

    /**
     * 手动获取：按字段优先级精确匹配，再做大小写不敏感匹配，
     * 然后模糊匹配，最后兜底返回整段 Cookie。
     */
    fun resolve(target: SiteTarget, harvest: Harvest, allowFallback: Boolean): CapturedToken? {
        val merged = harvest.merged

        target.keys.forEach { key ->
            val value = merged[key]
            if (!value.isNullOrBlank()) return build(target, key, value, harvest)
        }

        target.keys.forEach { key ->
            val hit = merged.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
            if (hit != null && hit.value.isNotBlank()) return build(target, hit.key, hit.value, harvest)
        }

        if (allowFallback) {
            val fuzzy = merged.entries.firstOrNull { (key, value) ->
                value.isNotBlank() && (
                    key.contains("token", true) ||
                        key.contains("session", true) ||
                        key.contains("auth", true) ||
                        key.contains("jwt", true) ||
                        key.contains("ticket", true)
                    )
            }
            if (fuzzy != null) return build(target, fuzzy.key, fuzzy.value, harvest)

            if (harvest.cookieHeader.isNotBlank()) {
                return CapturedToken(
                    site = target.name,
                    url = target.url,
                    key = "Cookie",
                    value = harvest.cookieHeader,
                    pairs = merged,
                )
            }
        }

        return null
    }

    private fun build(target: SiteTarget, key: String, raw: String, harvest: Harvest) = CapturedToken(
        site = target.name,
        url = target.url,
        key = key,
        value = normalizeValue(raw),
        pairs = harvest.merged,
    )

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) return emptyMap()
        val out = LinkedHashMap<String, String>()
        keys().forEach { key -> out[key] = optString(key) }
        return out
    }
}
