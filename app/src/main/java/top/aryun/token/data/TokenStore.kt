package top.aryun.token.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TokenStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("get_cookie_store", Context.MODE_PRIVATE)

    fun loadRecords(): List<TokenRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                TokenRecord(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    site = item.optString("site"),
                    url = item.optString("url"),
                    key = item.optString("key"),
                    value = item.optString("value"),
                    createdAt = item.optLong("createdAt"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveRecords(records: List<TokenRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("id", record.id)
                    put("site", record.site)
                    put("url", record.url)
                    put("key", record.key)
                    put("value", record.value)
                    put("createdAt", record.createdAt)
                }
            )
        }
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    fun loadCustomSites(): List<CustomSite> {
        val raw = prefs.getString(KEY_SITES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                CustomSite(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = item.optString("name"),
                    url = item.optString("url"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveCustomSites(sites: List<CustomSite>) {
        val array = JSONArray()
        sites.forEach { site ->
            array.put(
                JSONObject().apply {
                    put("id", site.id)
                    put("name", site.name)
                    put("url", site.url)
                }
            )
        }
        prefs.edit().putString(KEY_SITES, array.toString()).apply()
    }

    /** 保存到本地：写入应用私有外部目录，返回文件路径。 */
    fun exportToFile(record: TokenRecord): String? = runCatching {
        val base = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val dir = File(base, "tokens").apply { mkdirs() }
        val safeName = record.site.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(24)
        val safeKey = record.key.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(24)
        val file = File(dir, "${safeName}_$safeKey.txt")
        file.writeText(
            buildString {
                appendLine("站点：${record.site}")
                appendLine("字段：${record.key}")
                appendLine("地址：${record.url}")
                appendLine("时间：${formatTime(record.createdAt)}")
                appendLine()
                appendLine(record.value)
            }
        )
        file.absolutePath
    }.getOrNull()

    /** 把全部 Token 汇总导出成一个文件。 */
    fun exportAll(records: List<TokenRecord>): String? = runCatching {
        val base = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val dir = File(base, "tokens").apply { mkdirs() }
        val file = File(dir, "all_tokens.txt")
        file.writeText(
            records.joinToString("\n\n") { record ->
                buildString {
                    appendLine("===== ${record.site} · ${record.key} =====")
                    appendLine("地址：${record.url}")
                    appendLine("时间：${formatTime(record.createdAt)}")
                    appendLine(record.key + "=" + record.value)
                }
            }
        )
        file.absolutePath
    }.getOrNull()

    companion object {
        private const val KEY_RECORDS = "records"
        private const val KEY_SITES = "custom_sites"

        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

        fun formatTime(timestamp: Long): String =
            TIME_FORMAT.format(Date(timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()))

        /** 同一站点 + 同一字段（大小写不敏感）只保留最新一条。 */
        fun upsert(records: List<TokenRecord>, record: TokenRecord): List<TokenRecord> =
            listOf(record) + records.filterNot {
                it.site == record.site && it.key.equals(record.key, ignoreCase = true)
            }
    }
}
