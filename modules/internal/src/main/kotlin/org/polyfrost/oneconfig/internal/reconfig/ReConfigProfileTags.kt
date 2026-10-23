/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.internal.reconfig

import com.google.gson.JsonParser
import org.polyfrost.oneconfig.internal.ui.screens.ReConfigSocialService
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Public tags are fetched at most once per five minutes; private/missing results for ten minutes. */
object ReConfigProfileTags {
    private val executor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue<Runnable>(32),
        { task -> Thread(task, "ReConfig-ProfileTags").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
    @Volatile private var self = ""
    @Volatile private var ownRetryAt = 0L
    @Volatile private var ownPending = false
    private var ownRevision = 0L
    private val cache = ProfileTagCache(System::currentTimeMillis, executor) { uuid ->
        try { if (!PrivacyConsent.accepted) null else {
            Thread.sleep(1500) // bound background reads even in a crowd
            val response = JsonParser.parseString(ReConfigSocialService.communityRequest("/v2/profiles/$uuid")).asJsonObject
            parse(response.getAsJsonObject("profile"))
        } } catch (e: InterruptedException) { Thread.currentThread().interrupt(); null }
          catch (_: Exception) { null }
    }
    private fun parse(item: com.google.gson.JsonObject?): ProfileTagCache.Tag? {
        val text = item?.get("tag")?.asString.orEmpty()
        return if (text.matches(Regex("[A-Za-z0-9]{1,5}"))) ProfileTagCache.Tag(text, item?.get("emoji")?.asString.orEmpty()) else null
    }
    @JvmStatic @Synchronized fun start(uuid: String) {
        val id = uuid.replace("-", "").lowercase().takeIf { it.matches(Regex("[a-f0-9]{32}")) }.orEmpty()
        if (cache.identity(id)) { self = id; ownRevision++; ownPending = false; ownRetryAt = 0L }
    }
    @JvmStatic @Synchronized fun rememberOwn(uuid: String, tag: String, emoji: String) {
        val id = uuid.replace("-", "").lowercase()
        if (id != self) return
        ownRevision++
        cache.own(id, if (tag.matches(Regex("[A-Za-z0-9]{1,5}"))) ProfileTagCache.Tag(tag, emoji) else null)
        ownRetryAt = System.currentTimeMillis() + 300_000L
    }
    @Synchronized private fun refreshOwn() {
        if (ownPending || self.isEmpty() || System.currentTimeMillis() < ownRetryAt) return
        val id = self; val revision = ownRevision
        ownPending = true; ownRetryAt = System.currentTimeMillis() + 60_000L
        runCatching { executor.execute {
            try {
                if (PrivacyConsent.accepted) {
                    val response = JsonParser.parseString(ReConfigSocialService.communityRequest("/v2/profile/me")).asJsonObject
                    val value = parse(response.getAsJsonObject("profile"))
                    synchronized(this) { if (self == id && revision == ownRevision) { cache.own(id, value); ownRetryAt = System.currentTimeMillis() + 300_000L } }
                }
            } catch (_: Exception) { /* preserve current tag; retry after the bounded delay */ }
            finally { synchronized(this) { if (self == id) ownPending = false } }
        } }.onFailure { ownPending = false }
    }
    @JvmStatic fun lookup(uuid: UUID): ProfileTagCache.Tag? {
        if (!PrivacyConsent.accepted || self.isEmpty()) return null
        val id = uuid.toString().replace("-", "")
        if (id == self) refreshOwn()
        return cache.lookup(id)
    }
}
