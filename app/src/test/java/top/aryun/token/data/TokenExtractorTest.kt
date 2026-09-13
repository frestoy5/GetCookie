package top.aryun.token.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenExtractorTest {

    private val netease = SITE_PRESETS.first { it.id == "netease" }
    private val douyin = SITE_PRESETS.first { it.id == "douyin" }
    private val pan123 = SITE_PRESETS.first { it.id == "pan123" }

    private fun harvest(
        cookie: Map<String, String> = emptyMap(),
        local: Map<String, String> = emptyMap(),
        session: Map<String, String> = emptyMap(),
    ) = Harvest(cookie = cookie, local = local, session = session)

    @Test
    fun parseCookieHeader_splitsPairsAndTrims() {
        val parsed = TokenExtractor.parseCookieHeader(" a=1 ; MUSIC_U = u2 ;broken; =3")
        assertEquals("1", parsed["a"])
        assertEquals("u2", parsed["MUSIC_U"])
        assertEquals(2, parsed.size)
    }

    @Test
    fun parseCookieHeader_handlesBlankInput() {
        assertTrue(TokenExtractor.parseCookieHeader(null).isEmpty())
        assertTrue(TokenExtractor.parseCookieHeader("").isEmpty())
    }

    @Test
    fun normalizeValue_unwrapsJsonStringsAndWhitespace() {
        assertEquals("abc", TokenExtractor.normalizeValue("abc"))
        assertEquals("abc", TokenExtractor.normalizeValue("  abc  "))
        assertEquals("abc", TokenExtractor.normalizeValue("  \"abc\"  "))
        assertEquals("a\"b", TokenExtractor.normalizeValue("\"a\\\"b\""))
    }

    @Test
    fun harvest_mergesJsResultWithNativeCookies() {
        val merged = TokenExtractor.harvest(
            jsResult = """{"c":{"a":"1"},"l":{"token":"from-local"},"s":{"sid":"from-session"}}""",
            nativeCookies = mapOf("MUSIC_U" to "http-only-value"),
        )
        assertEquals("1", merged.cookie["a"])
        assertEquals("http-only-value", merged.cookie["MUSIC_U"])
        assertEquals("from-local", merged.local["token"])
        assertEquals("from-session", merged.session["sid"])
    }

    @Test
    fun harvest_toleratesMissingOrNullJsResult() {
        assertTrue(TokenExtractor.harvest("null", emptyMap()).merged.isEmpty())
        assertTrue(TokenExtractor.harvest("not json", emptyMap()).merged.isEmpty())
        assertEquals("1", TokenExtractor.harvest(null, mapOf("k" to "1")).merged["k"])
    }

    @Test
    fun collectJs_embedsKeysAndDeepFlag() {
        val shallow = TokenExtractor.collectJs(listOf("token", "MUSIC_U"), deep = false)
        assertTrue(shallow.contains("var DEEP = false;"))
        assertTrue(shallow.contains("\"MUSIC_U\""))

        val deep = TokenExtractor.collectJs(listOf("token"), deep = true)
        assertTrue(deep.contains("var DEEP = true;"))
    }

    @Test
    fun strongHits_returnsOnlyLoginOnlyKeysInPriorityOrder() {
        val hits = TokenExtractor.strongHits(
            netease,
            harvest(
                cookie = mapOf("__csrf" to "csrf", "MUSIC_A" to "second"),
                local = mapOf("MUSIC_U" to "first"),
            ),
        )
        assertEquals(listOf("MUSIC_U", "MUSIC_A"), hits.map { it.key })
    }

    @Test
    fun strongHits_skipsBlankValues() {
        val hits = TokenExtractor.strongHits(netease, harvest(cookie = mapOf("MUSIC_U" to "   ")))
        assertTrue(hits.isEmpty())
    }

    @Test
    fun resolve_prefersHighestPriorityKey() {
        val token = TokenExtractor.resolve(
            netease,
            harvest(cookie = mapOf("__csrf" to "csrf"), local = mapOf("MUSIC_U" to "music-u")),
            allowFallback = false,
        )
        assertEquals("MUSIC_U", token?.key)
        assertEquals("music-u", token?.value)
    }

    @Test
    fun resolve_matchesKeyCaseInsensitively() {
        val token = TokenExtractor.resolve(
            netease,
            harvest(cookie = mapOf("music_u" to "lowercase")),
            allowFallback = false,
        )
        assertEquals("music_u", token?.key)
        assertEquals("lowercase", token?.value)
    }

    @Test
    fun resolve_returnsNullWhenNothingMatchesAndFallbackDisabled() {
        assertNull(
            TokenExtractor.resolve(netease, harvest(cookie = mapOf("irrelevant" to "1")), allowFallback = false)
        )
    }

    @Test
    fun resolve_fallsBackToFuzzyTokenName() {
        val token = TokenExtractor.resolve(
            netease,
            harvest(cookie = mapOf("my_weird_token" to "v")),
            allowFallback = true,
        )
        assertEquals("my_weird_token", token?.key)
    }

    @Test
    fun resolve_fallsBackToWholeCookieHeader() {
        val token = TokenExtractor.resolve(
            netease,
            harvest(cookie = mapOf("sid" to "1", "uid" to "2")),
            allowFallback = true,
        )
        assertEquals("Cookie", token?.key)
        assertEquals("sid=1; uid=2", token?.value)
    }

    @Test
    fun autoCapture_detectsLoginTransition() {
        val loggedOut = harvest(cookie = mapOf("_ntes_nuid" to "guest"))
        val loggedIn = harvest(cookie = mapOf("_ntes_nuid" to "guest", "MUSIC_U" to "u1"))

        val baseline = TokenExtractor.strongHits(netease, loggedOut)
            .associate { it.key.lowercase() to it.value }
        assertTrue("访客状态不应有强字段", baseline.isEmpty())

        val changed = TokenExtractor.strongHits(netease, loggedIn)
            .firstOrNull { baseline[it.key.lowercase()] != it.value }
        assertEquals("MUSIC_U", changed?.key)
    }

    @Test
    fun autoCapture_doesNotFireWhileNothingChanges() {
        val guest = harvest(cookie = mapOf("sessionid" to "guest-session"))
        val baseline = TokenExtractor.strongHits(douyin, guest)
            .associate { it.key.lowercase() to it.value }

        val changed = TokenExtractor.strongHits(douyin, guest)
            .firstOrNull { baseline[it.key.lowercase()] != it.value }
        assertNull("登录前不应触发自动返回", changed)
    }

    @Test
    fun autoCapture_firesWhenAlreadyLoggedIn() {
        val alreadyIn = harvest(cookie = mapOf("LOGIN_INFO" to "logged-in"))
        val youtube = SITE_PRESETS.first { it.id == "youtube" }
        val hits = TokenExtractor.strongHits(youtube, alreadyIn)
        assertEquals("LOGIN_INFO", hits.firstOrNull()?.key)
    }

    @Test
    fun autoCapture_baselineIsStableForQuotedLocalStorageValues() {
        val raw = harvest(local = mapOf("token" to "  \"abc\"  "))
        val baseline = TokenExtractor.strongHits(pan123, raw)
            .associate { it.key.lowercase() to it.value }
        assertEquals("abc", baseline["token"])

        val changed = TokenExtractor.strongHits(pan123, raw)
            .firstOrNull { baseline[it.key.lowercase()] != it.value }
        assertNull("带引号的值不应导致每轮都误触发", changed)
    }
}
