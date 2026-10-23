/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.PlayerHead
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.reConfigGlass
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.navigation.graph.MessagesGraph
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.reconfig.PrivacyConsent
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SocialFriend(val uuid: String, val name: String, val state: String, val online: Boolean)
data class ChatLine(val text: String, val mine: Boolean, val sender: String, val id: Long = 0, val time: Long = System.currentTimeMillis(), val replyTo: Long? = null, val replyText: String? = null, val replySender: String? = null)

data class SocialInvitation(val id: Long, val sender: String, val address: String, val time: Long)

private data class ReConfigEmoji(val id: String, val asset: String)
private val reConfigEmojis = listOf(
    ReConfigEmoji("rose", "/assets/reconfig/emojis/rose.png"),
    ReConfigEmoji("pray", "/assets/reconfig/emojis/pray.png"),
    ReConfigEmoji("cute", "/assets/reconfig/emojis/cute.png"),
    ReConfigEmoji("kiss", "/assets/reconfig/emojis/kiss.png"),
    ReConfigEmoji("kiss-closed", "/assets/reconfig/emojis/kiss-closed.png"),
    ReConfigEmoji("sob-blue", "/assets/reconfig/emojis/sob-blue.png"),
    ReConfigEmoji("sob", "/assets/reconfig/emojis/sob.png"),
)
private const val EMOJI_PREFIX = "reconfig-emoji:"
private fun emojiToken(id: String) = "$EMOJI_PREFIX$id"
private fun emojiAsset(text: String) = text.takeIf { it.startsWith(EMOJI_PREFIX) }
    ?.removePrefix(EMOJI_PREFIX)?.let { id -> reConfigEmojis.firstOrNull { it.id == id }?.asset }

private object ReConfigSocialState {
    val friends = mutableStateListOf<SocialFriend>()
    val chats = mutableStateMapOf<String, SnapshotStateList<ChatLine>>()
    var active by mutableStateOf<String?>(null)

    fun add(name: String) {
        ReConfigSocialService.addFriend(name)
    }

    fun send(name: String, body: String, replyTo: Long? = null, onResult: (Boolean) -> Unit = {}) {
        ReConfigSocialService.sendMessage(name, body, replyTo, onResult)
    }

    fun deleteMessage(id: Long) { ReConfigSocialService.deleteMessage(id) }
}

object ReConfigSocialService {
    private const val URL = "https://reconfig-chat.duv14-reconfig-api.workers.dev"
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build()
    private val prefs = Preferences.userRoot().node("dev/duv14/reconfig")
    @Volatile private var identityName = ""
    @Volatile private var identityUuid = ""
    private fun token():String {
        check(org.polyfrost.oneconfig.internal.reconfig.SocialSyncState.validIdentity(identityName, identityUuid)) { "Sign in with a Minecraft account first" }
        val account=identityUuid
        val key="accountToken.$account"
        prefs.get(key,null)?.let{return it}
        val created=UUID.randomUUID().toString().replace("-","")
        prefs.put(key,created)
        runCatching{prefs.flush()}
        return created
    }
    private val worker = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "ReConfig-Social").apply { isDaemon = true }
    }
    @Volatile private var enrolledIdentity = ""
    private val enrollmentRetry = org.polyfrost.oneconfig.internal.reconfig.EnrollmentRetry()
    @Volatile private var socialTabVisible = false
    @Volatile private var lastBackgroundSync = 0L
    @Volatile private var lastFullRefresh = 0L
    @Volatile private var rateLimitedUntil = 0L
    private val syncState = org.polyfrost.oneconfig.internal.reconfig.SocialSyncState()
    private val pendingInvitationAcks = linkedSetOf<Long>()
    private val receivedInvitations = mutableStateListOf<SocialInvitation>()
    private val historyBefore = ConcurrentHashMap<String, Long>()
    private val historyMore = mutableStateMapOf<String, Boolean>()
    private val historyLoading = mutableStateMapOf<String, Boolean>()
    @Volatile private var networkFailures = 0
    @Volatile var enrollmentProof: (() -> String)? = null
    private val invitationCooldowns = ConcurrentHashMap<String, Long>()
    private val conversationCursors = ConcurrentHashMap<String, Long>()

    init { worker.scheduleWithFixedDelay({ runCatching {
        val now = System.currentTimeMillis()
        if (now < rateLimitedUntil) return@runCatching
        if (socialTabVisible && System.currentTimeMillis() - lastFullRefresh >= 10_000L) refresh()
        if (now - lastBackgroundSync >= if (socialTabVisible) 10_000L else 60_000L) backgroundSync()
    } }, 2, 5, TimeUnit.SECONDS) }

    @JvmStatic fun start(name:String,uuid:String) {
        val nextUuid = uuid.replace("-", "").lowercase()
        worker.execute {
            if (identityUuid != nextUuid || identityName != name.trim()) {
                identityName = name.trim()
                identityUuid = nextUuid.takeIf { org.polyfrost.oneconfig.internal.reconfig.SocialSyncState.validIdentity(identityName, it) }.orEmpty()
                enrolledIdentity = ""
                enrollmentRetry.reset()
                syncState.reset()
                pendingInvitationAcks.clear()
                Snapshot.withMutableSnapshot { receivedInvitations.clear() }
                historyBefore.clear()
                Snapshot.withMutableSnapshot { historyMore.clear(); historyLoading.clear() }
                lastBackgroundSync = 0
                lastFullRefresh = 0
                rateLimitedUntil = 0
                networkFailures = 0
                conversationCursors.clear()
                Snapshot.withMutableSnapshot {
                    ReConfigSocialState.friends.clear()
                    ReConfigSocialState.chats.clear()
                    ReConfigSocialState.active = null
                }
            }
            org.polyfrost.oneconfig.internal.reconfig.ReConfigProfileTags.start(identityUuid)
            ShellState.playerName = identityName
            ShellState.playerUuid = identityUuid
            runCatching { backgroundSync() }
        }
    }

    fun setTabVisible(visible: Boolean) {
        socialTabVisible = visible
        if (visible) worker.execute { if (System.currentTimeMillis() - lastFullRefresh >= 10_000L) runCatching { refresh() } }
    }

    fun openConversation(name: String) {
        ReConfigSocialState.active = name
        worker.execute { runCatching { syncConversation(name) } }
    }

    @JvmStatic fun friendsSnapshot(): List<SocialFriend> = ReConfigSocialState.friends.toList()
    @JvmStatic fun conversationSnapshot(name: String): List<ChatLine> = ReConfigSocialState.chats[name]?.toList().orEmpty()
    @JvmStatic fun invitationsSnapshot(): List<SocialInvitation> = receivedInvitations.toList()
    fun dismissInvitation(id: Long) { Snapshot.withMutableSnapshot { receivedInvitations.removeAll { it.id == id } } }
    @JvmStatic fun hasOlderMessages(name: String): Boolean {
        val friend = ReConfigSocialState.friends.firstOrNull { it.name.equals(name, true) } ?: return false
        return historyMore[friend.uuid] == true
    }
    @JvmStatic fun isLoadingHistory(name: String): Boolean = historyLoading[name] == true
    @JvmStatic fun loadOlderMessages(name: String) {
        if (historyLoading[name] == true) return
        Snapshot.withMutableSnapshot { historyLoading[name] = true }
        worker.execute { try { checked("Could not load older messages") {
            ensureEnrolled()
            val friend = ReConfigSocialState.friends.firstOrNull { it.state == "friends" && it.name.equals(name, true) } ?: return@checked
            val before = historyBefore[friend.uuid] ?: return@checked
            if (historyMore[friend.uuid] == true && before > 0) readConversationPage(friend, "/v2/messages?with=${friend.uuid}&before=$before", true)
        } } finally { Snapshot.withMutableSnapshot { historyLoading[name] = false } } }
    }

    fun addFriend(name: String) = worker.execute { checked("Friend request failed") { ensureEnrolled(); post("/v2/friends/request", "{\"name\":${quoted(name)}}"); refresh() } }
    fun accept(uuid: String) = worker.execute { checked("Could not accept request") { ensureEnrolled(); post("/v2/friends/accept", "{\"uuid\":${quoted(uuid)}}"); refresh() } }
    fun decline(uuid: String) = worker.execute { checked("Could not decline request") { ensureEnrolled(); post("/v2/friends/decline", "{\"uuid\":${quoted(uuid)}}"); refresh() } }
    fun cancel(uuid: String) = worker.execute { checked("Could not cancel request") { ensureEnrolled(); post("/v2/friends/cancel", "{\"uuid\":${quoted(uuid)}}"); refresh() } }
    fun remove(uuid: String) = worker.execute { checked("Could not remove friend") { ensureEnrolled(); post("/v2/friends/remove", "{\"uuid\":${quoted(uuid)}}"); refresh() } }
    fun invite(name: String) {
        val key=name.lowercase();val now=System.currentTimeMillis();val previous=invitationCooldowns[key]?:0L
        if(now-previous<30_000){Notifications.info("Invite cooldown","Try again in ${kotlin.math.ceil((30_000-(now-previous))/1000.0).toInt()}s");return}
        invitationCooldowns[key]=now
        worker.execute { checked("Invite failed") { ensureEnrolled(); val address=ShellState.serverAddress.ifBlank { error("Join a multiplayer server first") }; post("/v2/invitations", "{\"to\":${quoted(name)},\"serverAddress\":${quoted(address)}}"); Notifications.success("Invite sent", "Invited $name") } }
    }

    fun sendMessage(name: String, body: String, replyTo: Long? = null, onResult: (Boolean) -> Unit = {}) = worker.execute {
        val sent = runCatching {
            ensureEnrolled()
            val reply = replyTo?.let { ",\"replyTo\":$it" }.orEmpty()
            post("/v2/messages", "{\"to\":${quoted(name)},\"body\":${quoted(body)}$reply}")
        }.onFailure { Notifications.error("Message not sent", friendlyFailure(it)) }.isSuccess
        // A failed refresh after an accepted POST must not invite resending a duplicate.
        if (sent) runCatching { syncConversation(name) }
        onResult(sent)
    }
    fun deleteMessage(id: Long) = worker.execute { checked("Message not deleted") { ensureEnrolled(); request("/v2/messages/$id", "DELETE", null); applyDeletion(id) } }

    private fun refresh() {
        if (!PrivacyConsent.accepted) return
        lastFullRefresh = System.currentTimeMillis()
        ensureEnrolled()
        val friendsJson = runCatching { get("/v2/friends") }.getOrNull() ?: return
        val friends = JsonParser.parseString(friendsJson).asJsonObject.getAsJsonArray("friends") ?: return
        Snapshot.withMutableSnapshot {
            val replacement=mutableListOf<SocialFriend>()
            friends.forEach { element ->
                val item = element.asJsonObject
                val uuid=item.get("uuid")?.asString?:return@forEach
                val name = item.get("name")?.asString ?: return@forEach
                val state=item.get("state")?.asString?:"outgoing"
                val online=item.get("online")?.asInt==1
                replacement += SocialFriend(uuid,name,state,online)
            }
            ReConfigSocialState.friends.clear()
            ReConfigSocialState.friends.addAll(replacement)
        }
        ReConfigSocialState.active?.let(::syncConversation)
    }

    private fun syncConversation(name: String) {
        ensureEnrolled()
        val friend = ReConfigSocialState.friends.firstOrNull { it.state == "friends" && it.name.equals(name, true) } ?: return
        val after = conversationCursors[friend.uuid] ?: 0L
        readConversationPage(friend, "/v2/messages?with=${friend.uuid}&after=$after", after == 0L)
    }

    private fun readConversationPage(friend: SocialFriend, path: String, older: Boolean) {
        val payload = JsonParser.parseString(get(path)).asJsonObject
        val messages = payload.getAsJsonArray("messages") ?: return
        val friendId = friend.uuid.replace("-", "").lowercase()
        val parsed = messages.mapNotNull { element ->
            val item = element.asJsonObject
            val senderId = item.get("sender").asString.replace("-", "").lowercase()
            val recipientId = item.get("recipient").asString.replace("-", "").lowercase()
            if (!org.polyfrost.oneconfig.internal.reconfig.SocialSyncState.inConversation(identityUuid, friendId, senderId, recipientId)) return@mapNotNull null
            val mine = org.polyfrost.oneconfig.internal.reconfig.SocialSyncState.sentByMe(identityUuid, friendId, senderId, recipientId)
            val replySenderId = item.get("reply_sender")?.takeUnless { it.isJsonNull }?.asString?.replace("-", "")?.lowercase()
            ChatLine(item.get("body").asString, mine, if (mine) identityName else friend.name,
                item.get("id").asLong, item.get("created_at").asLong,
                item.get("reply_to")?.takeUnless { it.isJsonNull }?.asLong,
                item.get("reply_body")?.takeUnless { it.isJsonNull }?.asString,
                replySenderId?.let { if (it == friendId) friend.name else identityName })
        }
        parsed.maxOfOrNull { it.id }?.let { conversationCursors[friend.uuid] = maxOf(conversationCursors[friend.uuid] ?: 0L, it) }
        if (older) {
            payload.get("beforeCursor")?.asLong?.takeIf { it > 0 }?.let { historyBefore[friend.uuid] = it }
            Snapshot.withMutableSnapshot { historyMore[friend.uuid] = payload.get("hasMore")?.asBoolean == true }
        }
        Snapshot.withMutableSnapshot {
            val target = ReConfigSocialState.chats.getOrPut(friend.name) { mutableStateListOf() }
            val merged = (target.toList() + parsed).distinctBy { it.id }.sortedBy { it.id }
            if (merged != target.toList()) { target.clear(); target.addAll(merged) }
        }
    }

    private fun applyDeletion(id: Long) {
        Snapshot.withMutableSnapshot {
            ReConfigSocialState.chats.values.forEach { list ->
                list.removeAll { it.id == id }
                for (index in list.indices) if (list[index].replyTo == id) list[index] = list[index].copy(replyText = "Message deleted", replySender = null)
            }
        }
    }

    private fun backgroundSync() {
        if (!PrivacyConsent.accepted || !org.polyfrost.oneconfig.internal.reconfig.SocialSyncState.validIdentity(identityName, identityUuid)) return
        lastBackgroundSync = System.currentTimeMillis()
        ensureEnrolled()
        val serverHash = ShellState.serverAddress.takeIf(String::isNotBlank)?.let(::sha256).orEmpty()
        post("/v2/presence", "{\"serverHash\":${quoted(serverHash)}}")
        val payload = JsonParser.parseString(get(syncState.path())).asJsonObject
        fun cursor(key: String) = payload.get(key)?.asLong ?: 0L
        if (!syncState.isReady) {
            syncState.bootstrap(cursor("cursor"), cursor("requestCursor"), cursor("deletionCursor"), cursor("invitationCursor"))
            persistNotificationCursors()
            networkFailures = 0
            return
        }
        payload.getAsJsonArray("messages")?.forEach { element ->
            val item = element.asJsonObject
            if (item.get("id").asLong > syncState.messages && !ShellState.uiOpen)
                Notifications.info(item.get("sender_name")?.asString ?: "ReConfig", item.get("body")?.asString ?: "")
        }
        payload.getAsJsonArray("requests")?.forEach { element ->
            val item = element.asJsonObject
            if (item.get("id").asLong > syncState.requests && !ShellState.uiOpen)
                Notifications.info("Friend request", "${item.get("name").asString} wants to be friends")
        }
        payload.getAsJsonArray("invitations")?.forEach { element ->
            val item = element.asJsonObject
            val invitation = SocialInvitation(item.get("id").asLong, item.get("sender").asString, item.get("serverAddress").asString, item.get("created_at").asLong)
            if (invitation.id > syncState.invitations && receivedInvitations.none { it.id == invitation.id }) {
                Snapshot.withMutableSnapshot {
                    receivedInvitations.add(invitation)
                    if (receivedInvitations.size > 100) receivedInvitations.removeAt(0)
                }
                if (!ShellState.uiOpen) Notifications.info("Server invite from ${invitation.sender}", "Open Friends to view this invitation")
            }
            pendingInvitationAcks.add(invitation.id)
        }
        payload.getAsJsonArray("deleted")?.forEach { applyDeletion(it.asJsonObject.get("message_id").asLong) }
        syncState.advance(cursor("cursor"), cursor("requestCursor"), cursor("deletionCursor"), cursor("invitationCursor"))
        persistNotificationCursors()
        // An ACK failure leaves the IDs queued; the notification cursor prevents duplicate popups.
        if (pendingInvitationAcks.isNotEmpty()) {
            val ids = pendingInvitationAcks.take(50)
            post("/v2/invitations/ack", "{\"ids\":[${ids.joinToString(",")}]}")
            pendingInvitationAcks.removeAll(ids.toSet())
        }
        networkFailures = 0
    }

    private fun persistNotificationCursors() {
        prefs.putLong("messageCursor.$identityUuid", syncState.messages)
        prefs.putLong("requestCursor.$identityUuid", syncState.requests)
        prefs.putLong("deletionCursor.$identityUuid", syncState.deletions)
        prefs.putLong("invitationCursor.$identityUuid", syncState.invitations)
        runCatching { prefs.flush() }
    }

    @Synchronized
    fun communityRequest(path: String, method: String = "GET", body: String? = null): String {
        check(PrivacyConsent.accepted) { "Accept the privacy policy first" }
        ensureEnrolled()
        return request(path, method, body)
    }

    @Synchronized
    private fun ensureEnrolled() {
        check(PrivacyConsent.accepted) { "Accept the privacy policy first" }
        check(org.polyfrost.oneconfig.internal.reconfig.SocialSyncState.validIdentity(identityName, identityUuid)) { "Sign in with a Minecraft account first" }
        val name = identityName
        val uuid = identityUuid
        val identity="$uuid:$name"
        if (enrolledIdentity == identity) return
        enrollmentRetry.beforeAttempt()
        try {
            val payload = "{\"uuid\":${quoted(uuid)},\"name\":${quoted(name)},\"token\":${quoted(token())}"
            try { post("/v2/enroll", "$payload}") }
            catch (failure: Exception) {
                if (!failure.message.orEmpty().contains("ownership", true)) throw failure
                val proof = enrollmentProof?.invoke() ?: error("Minecraft account verification is unavailable")
                check(proof.matches(Regex("[a-f0-9]{40}"))) { "Minecraft account verification failed" }
                post("/v2/enroll", "$payload,\"serverId\":${quoted(proof)}}")
            }
            enrolledIdentity = identity
            enrollmentRetry.succeeded()
        } catch (failure: Exception) {
            val actionable = enrollmentRetry.failed(failure)
            System.getLogger("ReConfig-Social").log(System.Logger.Level.WARNING,
                "${actionable.message} (${failure.javaClass.simpleName})")
            throw actionable
        }
    }

    private fun get(path: String): String = request(path, "GET", null)
    private fun post(path: String, body: String): String = request(path, "POST", body)
    private fun request(path: String, method: String, body: String?): String {
        check(System.currentTimeMillis() >= rateLimitedUntil) { "ReConfig service is temporarily busy. Please wait before trying again." }
        require(path.startsWith("/v2/") && !path.contains("..")) { "Invalid social request" }
        val builder = HttpRequest.newBuilder(URI.create(URL + path)).timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer ${token()}").header("Content-Type", "application/json")
        builder.method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
        val response = try { http.send(builder.build(), HttpResponse.BodyHandlers.ofString()) }
        catch (failure: java.io.IOException) { backOff(); throw IllegalStateException("Could not reach ReConfig. Check your connection and try again.", failure) }
        catch (failure: InterruptedException) { Thread.currentThread().interrupt(); throw IllegalStateException("The request was interrupted. Please try again.", failure) }
        if(response.statusCode() !in 200..299){
            val json=runCatching{JsonParser.parseString(response.body()).asJsonObject}.getOrNull()
            if(response.statusCode()==429){
                val seconds=json?.get("retryAfterSeconds")?.asLong?.coerceIn(30,3600)?:300L
                rateLimitedUntil=System.currentTimeMillis()+seconds*1000L
                error("ReConfig service is temporarily busy. Try again in ${seconds}s.")
            }
            if (response.statusCode() >= 500) {
                backOff()
                error("ReConfig is temporarily unavailable. Please try again shortly.")
            }
            if (response.statusCode() == 401) enrolledIdentity = ""
            val detail=json?.get("detail")?.asString?:json?.get("error")?.asString
            error(detail?.take(240)?.takeUnless { Regex("D1_ERROR|SQLITE|SELECT |INSERT |token_hash|stack trace", RegexOption.IGNORE_CASE).containsMatchIn(it) }?:"ReConfig service returned HTTP ${response.statusCode()}")
        }
        return response.body()
    }

    private fun quoted(value: String): String = com.google.gson.Gson().toJson(value)
    private fun sha256(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}
    private fun backOff() {
        networkFailures = (networkFailures + 1).coerceAtMost(6)
        rateLimitedUntil = System.currentTimeMillis() + (5_000L shl networkFailures).coerceAtMost(300_000L)
    }
    private fun friendlyFailure(failure: Throwable): String = when (failure) {
        is IllegalStateException, is IllegalArgumentException -> failure.message ?: "The request could not be completed. Please try again."
        else -> "The request could not be completed. Please try again."
    }
    private inline fun checked(title:String,block:()->Unit){runCatching(block).onFailure{Notifications.error(title,friendlyFailure(it))}}
}

@Composable
fun FriendsScreen() {
    ShellState.title = "Friends"
    DisposableEffect(Unit) { ReConfigSocialService.setTabVisible(true); onDispose { ReConfigSocialService.setTabVisible(false) } }
    var dialog by remember { mutableStateOf(false) }
    var popupMounted by remember { mutableStateOf(false) }
    val animationScope = rememberCoroutineScope()
    fun openDialog() { popupMounted=true;animationScope.launch { withFrameNanos { };dialog=true } }
    fun closeDialog() { dialog=false;animationScope.launch { delay(190);popupMounted=false } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SocialButton("plus", "Add friend") { openDialog() }
        }
        Spacer(Modifier.height(18.dp))
        val invitations = ReConfigSocialService.invitationsSnapshot()
        if (invitations.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(invitations, key = { it.id }) { invitation ->
                    Row(Modifier.fillMaxWidth().reConfigGlass(LocalTheme.current.buttonShape, LocalTheme.current.componentBackground, LocalTheme.current.borderColor).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${invitation.sender} invited you to a server", modifier = Modifier.weight(1f), color = LocalTheme.current.textColor)
                        SocialButton("right-arrow", "Join") {
                            CommunityActions.connect?.invoke(invitation.address)
                            ReConfigSocialService.dismissInvitation(invitation.id)
                        }
                        SocialButton("close", "Dismiss", subtle = true) { ReConfigSocialService.dismissInvitation(invitation.id) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (ReConfigSocialState.friends.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No friends yet - add a Minecraft player.", color = LocalTheme.current.textColorSecondary)
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ReConfigSocialState.friends, key = { it.name.lowercase() }) { friend ->
                FriendRow(friend)
            }
        }
    }
    if (popupMounted) {
        Popup(alignment=Alignment.Center,properties=PopupProperties(focusable=true),onDismissRequest={closeDialog()}) {
            AnimatedVisibility(
                visible = dialog,
                enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) + scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = .94f) + slideInVertically(tween(260,easing=FastOutSlowInEasing)){it/14},
                exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) + scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = .97f) + slideOutVertically(tween(180,easing=FastOutSlowInEasing)){it/18},
            ) { AddFriendDialog { closeDialog() } }
        }
    }
}

@Composable
private fun FriendRow(friend: SocialFriend) {
    val theme = LocalTheme.current
    Row(
        Modifier.fillMaxWidth().height(68.dp).reConfigGlass(theme.modCardShape,theme.modCardBackground,theme.borderColor).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box { PlayerHead(friend.name,friend.uuid,Modifier.size(42.dp)); Box(Modifier.align(Alignment.BottomEnd).size(10.dp).background(if(friend.online)Color(0xFF43C76A) else Color(0xFF737A86),theme.buttonShape)) }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(friend.name, color = theme.textColor, fontWeight = FontWeight.Medium)
            Text(when(friend.state){"friends"->if(friend.online)"Online" else "Offline";"incoming"->"Incoming friend request";else->"Friend request pending"}, color = theme.textColorSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        when(friend.state){
            "incoming"->{SocialButton("checkmark","Accept"){ReConfigSocialService.accept(friend.uuid)};SocialButton("close","Decline",true){ReConfigSocialService.decline(friend.uuid)}}
            "outgoing"->SocialButton("close","Cancel Request",subtle=true,destructive = true){ReConfigSocialService.cancel(friend.uuid)}
            "friends"->{SocialButton("text","Message"){ReConfigSocialService.openConversation(friend.name);LocalNavController.wrapper.navigate(MessagesGraph)};SocialButton("right-arrow","Invite to Server",true){ReConfigSocialService.invite(friend.name)};SocialButton("close","Remove",subtle=true,destructive=true){ReConfigSocialService.remove(friend.uuid)}}
        }
    }
}

@Composable
private fun AddFriendDialog(close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color(0xA0000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(400.dp).background(LocalTheme.current.popupBackground, LocalTheme.current.popupShape)
                .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.popupShape).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Add a friend", color = LocalTheme.current.textColor, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            SocialInput(name, "Exact Minecraft username") { name = it.take(16) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialButton("plus", "Send request") { if (name.isNotBlank()) ReConfigSocialState.add(name.trim()); close() }
                SocialButton("close", "Cancel", subtle = true) { close() }
            }
        }
    }
}

@Composable
fun MessagesScreen() {
    ShellState.title = "Messages"
    DisposableEffect(Unit) { ReConfigSocialService.setTabVisible(true); onDispose { ReConfigSocialService.setTabVisible(false) } }
    val friends = ReConfigSocialState.friends
    val active = ReConfigSocialState.active
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyColumn(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(friends.filter { it.state=="friends" }, key = { it.name.lowercase() }) { friend ->
                Row(Modifier.fillMaxWidth().reConfigGlass(LocalTheme.current.buttonShape,if(active==friend.name)Accent.copy(.12f) else LocalTheme.current.componentBackground,LocalTheme.current.borderColor,if(active==friend.name)Accent.copy(.08f) else Color.Transparent).padding(10.dp).onClick(rememberInteractionSource()){ReConfigSocialService.openConversation(friend.name)},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){PlayerHead(friend.name,friend.uuid,Modifier.size(36.dp));Column{Text(friend.name,color=LocalTheme.current.textColor,fontWeight=FontWeight.Medium);Text(if(friend.online)"Online" else "Offline",color=LocalTheme.current.textColorSecondary,fontSize=11.sp)}}
            }
        }
        if (active == null) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("Choose a friend to start messaging.", color = LocalTheme.current.textColorSecondary)
            }
        } else Chat(active)
    }
}

@Composable
private fun RowScope.Chat(name: String) {
    var draft by remember(name) { mutableStateOf("") }
    var replyingTo by remember(name) { mutableStateOf<ChatLine?>(null) }
    var emojiOpen by remember(name) { mutableStateOf(false) }
    val lines = ReConfigSocialState.chats.getOrPut(name) { mutableStateListOf() }
    val listState = remember(name) { LazyListState() }
    val scope = rememberCoroutineScope()
    var sending by remember(name) { mutableStateOf(false) }
    var scrollAfterSend by remember(name) { mutableStateOf(false) }
    var previousNewest by remember(name) { mutableStateOf<Long?>(null) }
    var previousCount by remember(name) { mutableStateOf(0) }
    fun send(body: String, isEmoji: Boolean = false) {
        if (sending || body.isBlank()) return
        val originalDraft = draft
        val originalReply = replyingTo
        sending = true
        ReConfigSocialState.send(name, body.trim(), replyingTo?.id) { success ->
            scope.launch {
                sending = false
                if (success) {
                    if (!isEmoji && draft == originalDraft) draft = ""
                    if (replyingTo == originalReply) replyingTo = null
                    emojiOpen = false
                    scrollAfterSend = true
                }
            }
        }
    }
    // Keep entrance animations from replaying on polling or scrolling back.
    val enteredMessages = remember(name) { mutableSetOf<Long>() }
    LaunchedEffect(name, lines.lastOrNull()?.id, lines.size, scrollAfterSend) {
        val newest = lines.lastOrNull()?.id
        val nearBottom = (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= previousCount - 3
        val initial = previousNewest == null
        if (lines.isNotEmpty() && (initial || scrollAfterSend || (newest != previousNewest && nearBottom))) {
            listState.animateScrollToItem(lines.lastIndex)
            scrollAfterSend = false
        }
        previousNewest = newest
        previousCount = lines.size
    }
    Column(Modifier.weight(1f).fillMaxHeight()) {
        Text(name, color = LocalTheme.current.textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (ReConfigSocialService.hasOlderMessages(name)) {
            SocialButton("text", if (ReConfigSocialService.isLoadingHistory(name)) "Loading history…" else "Load older messages", subtle = true) {
                ReConfigSocialService.loadOlderMessages(name)
            }
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(state=listState,modifier=Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(lines,key={it.id}) { line ->
                val entrance = remember(name, line.id) {
                    Animatable(if (line.id in enteredMessages) 1f else 0f)
                }
                LaunchedEffect(name, line.id) {
                    enteredMessages.add(line.id)
                    entrance.animateTo(1f, tween(280, easing = EaseOutCubic))
                }
                // A graphics layer preserves row height during entry so scrolling stays stable.
                Row(Modifier.fillMaxWidth().graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 12.dp.toPx()
                },horizontalArrangement=if (line.mine) Arrangement.End else Arrangement.Start) {
                    Column(horizontalAlignment=if(line.mine)Alignment.End else Alignment.Start,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(line.sender,color=LocalTheme.current.textColorSecondary,fontSize=11.sp)
                        line.replyText?.let { quoted -> Box(Modifier.widthIn(max=420.dp).reConfigGlass(LocalTheme.current.buttonShape,LocalTheme.current.componentBackground.copy(.72f),LocalTheme.current.borderColor).padding(horizontal=10.dp,vertical=6.dp)){Column{Text("Reply to ${line.replySender.orEmpty()}",color=Accent,fontSize=10.sp);Text(quoted,color=LocalTheme.current.textColorSecondary,fontSize=11.sp)}} }
                        Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            if(line.mine) MessageDeleteButton { ReConfigSocialState.deleteMessage(line.id) }
                            Box(Modifier.widthIn(max=480.dp).reConfigGlass(LocalTheme.current.popupShape,if(line.mine)Accent.copy(.34f) else LocalTheme.current.modCardBackground,if(line.mine)Accent.copy(.42f) else LocalTheme.current.borderColor,if(line.mine)Accent.copy(.10f) else Color.Transparent).onClick(rememberInteractionSource()){replyingTo=line}.padding(horizontal=14.dp,vertical=9.dp)){
                                val asset = emojiAsset(line.text)
                                if (asset != null) Icon(asset, modifier = Modifier.size(72.dp))
                                else Text(line.text,color=if(line.mine)LocalTheme.current.accentTextColor else LocalTheme.current.textColor)
                            }
                        }
                    }
                }
            }
        }
        replyingTo?.let { line -> Row(Modifier.fillMaxWidth().reConfigGlass(LocalTheme.current.buttonShape,LocalTheme.current.componentBackground,LocalTheme.current.borderColor).padding(8.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Replying to ${line.sender}",color=Accent,fontSize=11.sp);Text(line.text,color=LocalTheme.current.textColorSecondary,fontSize=12.sp)};SocialButton("close",""){replyingTo=null}};Spacer(Modifier.height(7.dp)) }
        if(emojiOpen) EmojiPicker { token -> send(token, isEmoji = true) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SocialButton("smile", "") { emojiOpen=!emojiOpen }
            Box(Modifier.weight(1f)) { SocialInput(draft, "Message $name", onSubmit={send(draft)}) { draft = it.take(500) } }
            SocialButton("right-arrow", if (sending) "Sending…" else "Send") {
                send(draft)
            }
        }
    }
}

@Composable
private fun MessageDeleteButton(click: () -> Unit) {
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .955f else if (hovered) 1.015f else 1f, tween(150, easing = FastOutSlowInEasing))
    Box(
        Modifier.size(24.dp)
            .semantics { contentDescription = "Delete message" }
            .onClick(source, click).pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon("trash", color = LocalTheme.current.textColorSecondary.copy(alpha = if (hovered) .9f else .55f),
            modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun SocialButton(icon: String, label: String, subtle: Boolean = false, destructive: Boolean = false, click: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) .955f else if (hovered) 1.015f else 1f,
        tween(150, easing = FastOutSlowInEasing),
    )
    val color = if (destructive) Color(0xFFB83D4B).copy(alpha=if(hovered).92f else .78f) else if (subtle) theme.componentBackground else Accent.copy(alpha = if (hovered) .86f else 1f)
    Row(
        Modifier.height(34.dp).graphicsLayer { scaleX=scale;scaleY=scale }.reConfigGlass(theme.buttonShape,color,if(destructive)Color(0xFFB83D4B) else theme.borderColor)
            .onClick(source, click).pointerHoverIcon(PointerIcon.Hand).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, color = if (subtle&&!destructive) theme.textColor else theme.accentTextColor, modifier = Modifier.size(15.dp))
        if(label.isNotEmpty()) Text(label, color = if (subtle&&!destructive) theme.textColor else theme.accentTextColor)
    }
}

@Composable
private fun EmojiPicker(select:(String)->Unit){
    Row(Modifier.fillMaxWidth().reConfigGlass(LocalTheme.current.popupShape,LocalTheme.current.popupBackground,LocalTheme.current.borderColor).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){reConfigEmojis.forEach{emoji->Box(Modifier.size(38.dp).onClick(rememberInteractionSource()){select(emojiToken(emoji.id))}.pointerHoverIcon(PointerIcon.Hand),contentAlignment=Alignment.Center){Icon(emoji.asset,modifier=Modifier.size(32.dp))}}}
}

@Composable
private fun SocialInput(value: String, placeholder: String, onSubmit:()->Unit = {}, change: (String) -> Unit) {
    val theme = LocalTheme.current
    BasicTextField(
        value = value,
        onValueChange = change,
        singleLine = true,
        textStyle = TextStyle(color = theme.textColor, fontSize = 14.sp, fontFamily = theme.typography.family),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier.fillMaxWidth().trackTextInputFocus().onPreviewKeyEvent { event -> if(event.type==KeyEventType.KeyDown&&(event.key==Key.Enter||event.key==Key.NumPadEnter)){onSubmit();true}else false }.reConfigGlass(theme.sideBarNavigationEntryShape,theme.componentBackground,theme.borderColor)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = theme.textColorSecondary); inner() } },
    )
}
