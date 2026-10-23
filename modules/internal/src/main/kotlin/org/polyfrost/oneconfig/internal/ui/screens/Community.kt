/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/** Platform callbacks are installed by the Minecraft client; network work runs on IO. */
object CommunityActions {
    @JvmField var connect: ((String) -> Unit)? = null
    @JvmField var status: ((String) -> CommunityServerStatus)? = null
}
data class CommunityServerStatus(val latencyMs: Long? = null, val players: Int? = null, val iconPng: ByteArray? = null)
private data class CommunityProfile(val uuid: String, val name: String, val bio: String, val tag: String, val emoji: String)
private data class DirectoryServer(val id: String, val name: String, val address: String, val description: String, val players: Int?)
private val serverProbePermits = Semaphore(4)
private val serverStatusCache = ConcurrentHashMap<String, Pair<Long, CommunityServerStatus>>()
private val communityEmojis = listOf("rose", "pray", "cute", "kiss", "kiss-closed", "sob-blue", "sob")
private fun JsonObject.string(key: String) = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
private fun query(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
private fun safeText(value: String, limit: Int) = value.filter { it >= ' ' && it != '§' && it != '\u007f' }.take(limit)
private suspend fun communityGet(path: String): JsonObject = withContext(Dispatchers.IO) {
    JsonParser.parseString(ReConfigSocialService.communityRequest(path)).asJsonObject
}
private fun profile(item: JsonObject) = CommunityProfile(item.string("uuid"), safeText(item.string("name"), 16), safeText(item.string("bio"), 280), safeText(item.string("tag"), 5), item.string("emoji"))

@Composable
fun YourProfileScreen() {
    ShellState.title = "Profiles"
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(ProfileDraft()) }
    var savedDraft by remember { mutableStateOf(ProfileDraft()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CommunityProfile>>(emptyList()) }
    var searchNotice by remember { mutableStateOf("") }
    var viewing by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(retry, ShellState.playerUuid) {
        loaded = false
        failed = false
        notice = "Loading your profile…"
        try {
            val response = communityGet("/v2/profile/me")
            val item = response.getAsJsonObject("profile") ?: response
            draft = ProfileDraft(
                safeText(item.string("bio"), 280),
                safeText(item.string("tag"), 5),
                item.string("emoji").takeIf { it in communityEmojis }.orEmpty(),
                item.get("public")?.takeUnless { it.isJsonNull }?.asBoolean ?: false,
            )
            savedDraft = draft
            org.polyfrost.oneconfig.internal.reconfig.ReConfigProfileTags.rememberOwn(ShellState.playerUuid, draft.tag, draft.emoji)
            loaded = true
            notice = ""
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            failed = true
            notice = "Your profile could not be loaded. Check your connection and try again."
        }
    }
    LaunchedEffect(search, searching) {
        if (!searching) return@LaunchedEffect
        delay(350)
        searchNotice = "Searching public profiles…"
        try {
            val response = communityGet("/v2/profiles?q=${query(search)}")
            results = response.getAsJsonArray("profiles")?.map { profile(it.asJsonObject) }.orEmpty()
            searchNotice = if (results.isEmpty()) "No public profiles found." else ""
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { results = emptyList(); searchNotice = "Profile search is unavailable. Try again shortly." }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileSearchBar(search, searching, { searching = it }, { search = safeText(it, 32) })
        AnimatedContent(targetState = searching, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(130)) }, modifier = Modifier.weight(1f)) { showSearch ->
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!showSearch) item {
            ProfileEditor(
                draft = draft, dirty = draft != savedDraft, loaded = loaded, busy = busy,
                notice = notice, failed = failed,
                onChange = { draft = it; notice = ""; failed = false },
                onRetry = { retry++ },
                onReset = { draft = savedDraft; notice = ""; failed = false },
                onSave = {
                    if (loaded && !busy && draft != savedDraft) {
                        val pending = draft
                        busy = true
                        failed = false
                        val body = JsonObject().apply {
                            addProperty("bio", pending.bio); addProperty("tag", pending.tag); addProperty("emoji", pending.emoji)
                            addProperty("public", pending.public); addProperty("publicationConsent", pending.public)
                        }.toString()
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { ReConfigSocialService.communityRequest("/v2/profile/me", "POST", body) }
                                savedDraft = pending
                                org.polyfrost.oneconfig.internal.reconfig.ReConfigProfileTags.rememberOwn(ShellState.playerUuid, pending.tag, pending.emoji)
                                notice = if (pending.public) "Saved. Your profile is visible in public search." else "Saved. Your profile is private."
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) { failed = true; notice = "Could not save. Your edits are kept here so you can try again." }
                            finally { busy = false }
                        }
                    }
                },
            )
        }
        if (showSearch) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Find your people", color = LocalTheme.current.textColor, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text("Search public ReConfig profiles by name or tag.", color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                if (searchNotice.isNotEmpty()) Text(searchNotice, color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                else Text("${results.size} public ${if (results.size == 1) "profile" else "profiles"}", color = LocalTheme.current.textColorSecondary, fontSize = 11.sp)
            }
        }
        if (showSearch) items(results, key = { it.uuid }) { entry ->
            CommunityPanel {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayerHead(entry.name, entry.uuid, Modifier.size(58.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileLabel(entry.name, entry.tag, entry.emoji)
                        if (entry.bio.isNotBlank()) Text(entry.bio, color = LocalTheme.current.textColorSecondary)
                    }
                    CommunityButton(if (viewing == entry.uuid) "Close profile" else "View profile") { viewing = if (viewing == entry.uuid) null else entry.uuid }
                }
                AnimatedVisibility(viewing == entry.uuid) { PlayerSkinPreview(entry.name, entry.uuid, Modifier.fillMaxWidth(), tag = entry.tag, emoji = entry.emoji) }
            }
        }
        }
        }
    }
}

@Composable
fun PublicServersScreen() {
    ShellState.title = "Public Servers"
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("recommended") }
    var refresh by remember { mutableIntStateOf(0) }
    var servers by remember { mutableStateOf<List<DirectoryServer>>(emptyList()) }
    var notice by remember { mutableStateOf("Loading servers…") }
    val liveStatuses = remember { mutableStateMapOf<String, CommunityServerStatus>() }
    LaunchedEffect(search, sort, refresh) {
        delay(450)
        notice = "Loading servers…"
        try {
            val response = communityGet("/v2/servers?q=${query(search)}&sort=$sort")
            servers = response.getAsJsonArray("servers")?.map {
                val item = it.asJsonObject
                DirectoryServer(item.string("id"), safeText(item.string("name"), 80), item.string("address"), safeText(item.string("description"), 500), item.get("players")?.takeUnless { p -> p.isJsonNull }?.asInt)
            }.orEmpty()
            notice = if (servers.isEmpty()) "No servers match your search." else ""
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { servers = emptyList(); notice = "The directory is unavailable. Try again shortly." }
    }
    LaunchedEffect(servers.map { it.address }.toSet(), refresh) {
        val addresses = servers.map { it.address }.distinct()
        liveStatuses.clear()
        coroutineScope {
            addresses.forEach { address ->
                launch {
                    serverProbePermits.withPermit {
                        val result = withContext(Dispatchers.IO) {
                            val cached = serverStatusCache[address]
                            if (cached != null && refresh == 0 && System.currentTimeMillis() - cached.first < 60_000L) cached.second
                            else {
                                val probed = runCatching { CommunityActions.status?.invoke(address) }.getOrNull() ?: CommunityServerStatus()
                                if (serverStatusCache.size >= 256) serverStatusCache.clear()
                                serverStatusCache[address] = System.currentTimeMillis() to probed
                                probed
                            }
                        }
                        liveStatuses[address] = result
                    }
                }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CommunityPanel {
                Text("Public Servers", color = LocalTheme.current.textColor, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text("Discover a server to play on", color = LocalTheme.current.textColorSecondary, fontSize = 13.sp)
                CommunityInput(search, "Search name, server address or game mode") { search = safeText(it, 64) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CommunityButton("Sort: " + when (sort) { "name" -> "Name"; "players" -> "Players"; else -> "Recommended" }) {
                        sort = when (sort) { "recommended" -> "name"; "name" -> "players"; else -> "recommended" }
                    }
                    CommunityButton("Refresh") { refresh++ }
                }
                if (notice.isNotBlank()) Text(notice, color = LocalTheme.current.textColorSecondary)
            }
        }
        if (servers.isNotEmpty()) item {
            Text(if (search.isBlank()) "Recommended servers" else "${servers.size} search results", color = LocalTheme.current.textColorSecondary, fontSize = 13.sp)
        }
        val ordered = if (sort == "players") servers.sortedWith(compareByDescending<DirectoryServer> { liveStatuses[it.address]?.players ?: it.players ?: -1 }.thenBy { it.name }) else servers
        items(ordered, key = { it.id }) { server ->
            DirectoryCard(server, liveStatuses[server.address])
        }
    }
}

@Composable
private fun DirectoryCard(server: DirectoryServer, status: CommunityServerStatus?) {
    val checking = status == null
    var connectNotice by remember(server.address) { mutableStateOf("") }
    val painter = remember(status?.iconPng) { status?.iconPng?.let { bytes -> runCatching { BitmapPainter(loadImageBitmap(ByteArrayInputStream(bytes))) }.getOrNull() } }
    CommunityPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (painter != null) Image(painter, "${server.name} icon", Modifier.size(48.dp))
            else Icon("profiles", modifier = Modifier.size(40.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(server.name, color = LocalTheme.current.textColor, fontWeight = FontWeight.SemiBold)
                Text(server.address, color = Accent, fontSize = 12.sp)
                Text(server.description, color = LocalTheme.current.textColorSecondary)
                val players = status?.players ?: server.players
                Text("${players?.let { "$it players" } ?: "Player count unavailable"} · ${if (checking) "Checking…" else status?.latencyMs?.let { "$it ms" } ?: "Ping unavailable / offline"}", fontSize = 12.sp, color = LocalTheme.current.textColorSecondary)
            }
            CommunityButton("Play", CommunityActions.connect != null) {
                runCatching { CommunityActions.connect?.invoke(server.address) }.onFailure { connectNotice = "Could not connect. Try again from Minecraft Multiplayer." }
            }
        }
        if (connectNotice.isNotEmpty()) Text(connectNotice, color = LocalTheme.current.textColorSecondary)
    }
}

@Composable
private fun ProfileLabel(name: String, tag: String, emoji: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, color = LocalTheme.current.textColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        if (tag.isNotEmpty()) ProfileTag(tag, emoji)
    }
}

@Composable
private fun CommunityPanel(content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalTheme.current
    Column(Modifier.fillMaxWidth().reConfigGlass(theme.modCardShape, theme.modCardBackground, theme.borderColor).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun CommunityInput(value: String, placeholder: String, singleLine: Boolean = true, changed: (String) -> Unit) {
    val theme = LocalTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(placeholder, fontSize = 12.sp, color = theme.textColorSecondary)
        BasicTextField(value, changed, singleLine = singleLine, textStyle = TextStyle(color = theme.textColor, fontSize = 14.sp), cursorBrush = SolidColor(Accent), modifier = Modifier.fillMaxWidth().trackTextInputFocus().reConfigGlass(theme.buttonShape, theme.componentBackground, theme.borderColor).padding(12.dp))
    }
}

@Composable
private fun CommunityButton(label: String, enabled: Boolean = true, action: () -> Unit) {
    val theme = LocalTheme.current
    Box(Modifier.reConfigGlass(theme.buttonShape, theme.componentBackground, theme.borderColor).onClick(rememberInteractionSource()) { if (enabled) action() }.padding(horizontal = 13.dp, vertical = 9.dp)) {
        Text(label, color = if (enabled) theme.textColor else theme.textColorSecondary, fontSize = 13.sp)
    }
}


