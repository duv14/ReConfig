/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutCubic
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import java.util.prefs.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SocialFriend(val uuid: String, val name: String, val state: String, val online: Boolean)
private data class ChatLine(val text: String, val mine: Boolean, val sender: String, val id: Long = 0, val time: Long = System.currentTimeMillis())

private object ReConfigSocialState {
    val friends = mutableStateListOf<SocialFriend>()
    val chats = mutableStateMapOf<String, SnapshotStateList<ChatLine>>()
    var active by mutableStateOf<String?>(null)

    fun add(name: String) {
        ReConfigSocialService.addFriend(name)
    }

    fun send(name: String, body: String) {
        ReConfigSocialService.sendMessage(name, body)
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
        val account=identityUuid.ifBlank { identityName.lowercase() }.ifBlank { "unknown" }
        val key="accountToken.$account"
        prefs.get(key,null)?.let{return it}
        val created=prefs.get("deviceToken",null)?:UUID.randomUUID().toString().replace("-","")
        prefs.put(key,created)
        runCatching{prefs.flush()}
        return created
    }
    private val worker = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "ReConfig-Social").apply { isDaemon = true }
    }
    @Volatile private var enrolledIdentity = ""
    private val initializedConversations = mutableSetOf<String>()

    init { worker.scheduleWithFixedDelay({ runCatching { refresh() } }, 2, 3, TimeUnit.SECONDS) }

    @JvmStatic fun start(name:String,uuid:String) {
        identityName=name.trim().take(16)
        identityUuid=uuid.replace("-","").lowercase().takeIf { it.length==32 }.orEmpty()
        ShellState.playerName=identityName.ifBlank { ShellState.playerName }
        ShellState.playerUuid=identityUuid.ifBlank { ShellState.playerUuid }
        enrolledIdentity=""
        worker.execute { runCatching { refresh() } }
    }

    fun addFriend(name: String) = worker.execute { checked("Friend request failed") { ensureEnrolled(); post("/v2/friends/request", "{\"name\":${quoted(name)}}"); refresh() } }
    fun accept(uuid: String) = worker.execute { checked("Could not accept request") { ensureEnrolled(); post("/v2/friends/accept", "{\"uuid\":${quoted(uuid)}}"); refresh() } }
    fun decline(uuid: String) = worker.execute { checked("Could not decline request") { ensureEnrolled(); post("/v2/friends/decline", "{\"uuid\":${quoted(uuid)}}"); refresh() } }
    fun cancel(uuid: String) = worker.execute { checked("Could not cancel request") { ensureEnrolled(); post("/v2/friends/cancel", "{\"uuid\":${quoted(uuid)}}"); refresh() } }
    fun invite(name: String) = worker.execute { checked("Invite failed") { ensureEnrolled(); val address=ShellState.serverAddress.ifBlank { error("Join a multiplayer server first") }; post("/v2/invitations", "{\"to\":${quoted(name)},\"serverAddress\":${quoted(address)}}"); Notifications.success("Invite sent", "Invited $name") } }

    fun sendMessage(name: String, body: String) = worker.execute {
        checked("Message not sent") { ensureEnrolled(); post("/v2/messages", "{\"to\":${quoted(name)},\"body\":${quoted(body)}}"); refresh() }
    }
    fun deleteMessage(id: Long) = worker.execute { checked("Message not deleted") { ensureEnrolled(); request("/v2/messages/$id", "DELETE", null); refresh() } }

    private fun refresh() {
        ensureEnrolled()
        val serverHash=ShellState.serverAddress.takeIf(String::isNotBlank)?.let(::sha256).orEmpty()
        runCatching { post("/v2/presence", "{\"serverHash\":${quoted(serverHash)}}") }
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
        ReConfigSocialState.friends.filter { it.state=="friends" }.forEach { friend ->
            val payload=runCatching { get("/v2/messages?with=${friend.uuid}&after=0") }.getOrNull() ?: return@forEach
            val messages=JsonParser.parseString(payload).asJsonObject.getAsJsonArray("messages")
            // The endpoint returns only this authenticated two-person conversation.
            // Classify against its stable peer, not a possibly offline/local player UUID.
            val friendId=friend.uuid.replace("-","").lowercase()
            val parsed=messages.map { element ->
                val item=element.asJsonObject
                val senderId=item.get("sender").asString.replace("-","").lowercase()
                val recipientId=item.get("recipient").asString.replace("-","").lowercase()
                val mine=friendId.isNotBlank() && recipientId == friendId && senderId != friendId
                ChatLine(item.get("body").asString,mine,if(mine)identityName.ifBlank { ShellState.playerName } else friend.name,item.get("id").asLong,item.get("created_at").asLong)
            }
            val oldIds=ReConfigSocialState.chats[friend.name]?.map{it.id}?.toSet().orEmpty()
            if(friend.name in initializedConversations && !ShellState.uiOpen){parsed.filter{!it.mine&&it.id !in oldIds}.forEach{Notifications.info(friend.name,it.text)}}
            initializedConversations += friend.name
            Snapshot.withMutableSnapshot { val target=ReConfigSocialState.chats.getOrPut(friend.name){mutableStateListOf()};if(target.toList()!=parsed){target.clear();target.addAll(parsed)} }
        }
        runCatching { get("/v2/events") }.getOrNull()?.let { payload -> JsonParser.parseString(payload).asJsonObject.getAsJsonArray("invitations")?.forEach { element -> val item=element.asJsonObject;Notifications.info("Server invite from ${item.get("sender").asString}",item.get("serverAddress").asString) } }
    }

    private fun ensureEnrolled() {
        val name = identityName.ifBlank { ShellState.playerName }.takeIf { it.isNotBlank() && it != "Player" } ?: return
        val uuid=identityUuid.ifBlank { ShellState.playerUuid.takeIf { it.length==32 }.orEmpty() }.ifBlank { UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(StandardCharsets.UTF_8)).toString().replace("-","") }
        val identity="$uuid:$name"
        if (enrolledIdentity == identity) return
        post("/v2/enroll", "{\"uuid\":${quoted(uuid)},\"name\":${quoted(name)},\"token\":${quoted(token())}}")
        enrolledIdentity = identity
    }

    private fun get(path: String): String = request(path, "GET", null)
    private fun post(path: String, body: String): String = request(path, "POST", body)
    private fun request(path: String, method: String, body: String?): String {
        val builder = HttpRequest.newBuilder(URI.create(URL + path)).timeout(Duration.ofSeconds(8))
            .header("Authorization", "Bearer ${token()}").header("Content-Type", "application/json")
        builder.method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
        val response=http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if(response.statusCode() !in 200..299){val json=runCatching{JsonParser.parseString(response.body()).asJsonObject}.getOrNull();val detail=json?.get("detail")?.asString?:json?.get("error")?.asString;error(detail?:"ReConfig service returned HTTP ${response.statusCode()}")}
        return response.body()
    }

    private fun quoted(value: String): String = buildString {
        append('"')
        value.forEach { c -> when (c) { '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n"); '\r' -> append("\\r"); else -> append(c) } }
        append('"')
    }
    private fun sha256(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}
    private inline fun checked(title:String,block:()->Unit){runCatching(block).onFailure{Notifications.error(title,it.message?:"Network error")}}
}

@Composable
fun FriendsScreen() {
    ShellState.title = "Friends"
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
        if (ReConfigSocialState.friends.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No friends yet — add another ReConfig user.", color = LocalTheme.current.textColorSecondary)
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
            "friends"->{SocialButton("text","Message"){ReConfigSocialState.active=friend.name;LocalNavController.wrapper.navigate(MessagesGraph)};SocialButton("right-arrow","Invite to Server",true){ReConfigSocialService.invite(friend.name)}}
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
    val friends = ReConfigSocialState.friends
    val active = ReConfigSocialState.active
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyColumn(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(friends.filter { it.state=="friends" }, key = { it.name.lowercase() }) { friend ->
                Row(Modifier.fillMaxWidth().reConfigGlass(LocalTheme.current.buttonShape,if(active==friend.name)Accent.copy(.12f) else LocalTheme.current.componentBackground,LocalTheme.current.borderColor,if(active==friend.name)Accent.copy(.08f) else Color.Transparent).padding(10.dp).onClick(rememberInteractionSource()){ReConfigSocialState.active=friend.name},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){PlayerHead(friend.name,friend.uuid,Modifier.size(36.dp));Column{Text(friend.name,color=LocalTheme.current.textColor,fontWeight=FontWeight.Medium);Text(if(friend.online)"Online" else "Offline",color=LocalTheme.current.textColorSecondary,fontSize=11.sp)}}
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
    val lines = ReConfigSocialState.chats.getOrPut(name) { mutableStateListOf() }
    val listState = rememberLazyListState()
    // Keep entrance animations from replaying on polling or scrolling back.
    val enteredMessages = remember(name) { mutableSetOf<Long>() }
    LaunchedEffect(name, lines.size) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex) }
    Column(Modifier.weight(1f).fillMaxHeight()) {
        Text(name, color = LocalTheme.current.textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
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
                        Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            if(line.mine) MessageDeleteButton { ReConfigSocialState.deleteMessage(line.id) }
                            Box(Modifier.widthIn(max=480.dp).reConfigGlass(LocalTheme.current.popupShape,if(line.mine)Accent.copy(.34f) else LocalTheme.current.modCardBackground,if(line.mine)Accent.copy(.42f) else LocalTheme.current.borderColor,if(line.mine)Accent.copy(.10f) else Color.Transparent).padding(horizontal=14.dp,vertical=9.dp)){Text(line.text,color=if(line.mine)LocalTheme.current.accentTextColor else LocalTheme.current.textColor)}
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.weight(1f)) { SocialInput(draft, "Message $name") { draft = it.take(500) } }
            SocialButton("right-arrow", "Send") {
                if (draft.isNotBlank()) { ReConfigSocialState.send(name, draft.trim()); draft = "" }
            }
        }
    }
}

@Composable
private fun MessageDeleteButton(click: () -> Unit) {
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
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
    val color = if (destructive) Color(0xFFB83D4B).copy(alpha=if(hovered).92f else .78f) else if (subtle) theme.componentBackground else Accent.copy(alpha = if (hovered) .86f else 1f)
    Row(
        Modifier.height(34.dp).reConfigGlass(theme.buttonShape,color,if(destructive)Color(0xFFB83D4B) else theme.borderColor)
            .onClick(source, click).pointerHoverIcon(PointerIcon.Hand).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, color = if (subtle&&!destructive) theme.textColor else theme.accentTextColor, modifier = Modifier.size(15.dp))
        if(label.isNotEmpty()) Text(label, color = if (subtle&&!destructive) theme.textColor else theme.accentTextColor)
    }
}

@Composable
private fun SocialInput(value: String, placeholder: String, change: (String) -> Unit) {
    val theme = LocalTheme.current
    BasicTextField(
        value = value,
        onValueChange = change,
        singleLine = true,
        textStyle = TextStyle(color = theme.textColor, fontSize = 14.sp, fontFamily = theme.typography.family),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier.fillMaxWidth().reConfigGlass(theme.sideBarNavigationEntryShape,theme.componentBackground,theme.borderColor)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = theme.textColorSecondary); inner() } },
    )
}
