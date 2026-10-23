/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

internal data class ProfileDraft(val bio: String = "", val tag: String = "", val emoji: String = "", val public: Boolean = false)
internal val profileEmojis = listOf("rose", "pray", "cute", "kiss", "kiss-closed", "sob-blue", "sob")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileEditor(draft: ProfileDraft, dirty: Boolean, loaded: Boolean, busy: Boolean,
    notice: String, failed: Boolean, onChange: (ProfileDraft) -> Unit, onRetry: () -> Unit,
    onReset: () -> Unit, onSave: () -> Unit) {
    val theme = LocalTheme.current
    var picker by remember { mutableStateOf(false) }
    val username = ShellState.playerName.ifBlank { "Player" }
    @Composable fun stage(modifier: Modifier) {
        Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF17191D))
            .border(1.dp, Color.White.copy(.055f), RoundedCornerShape(20.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE PREVIEW", color = theme.textColorSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                ProfilePill(if (draft.public) "Public" else "Private")
            }
            PlayerSkinPreview(username, ShellState.playerUuid, Modifier.fillMaxWidth(), tag = draft.tag, emoji = draft.emoji)
        }
    }
    @Composable fun details(modifier: Modifier) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProfileSection("Your identity", "Your Minecraft username stays linked to your account.") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerHead(username, ShellState.playerUuid, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(username, color = theme.textColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon("lock", color = theme.textColorSecondary, modifier = Modifier.size(11.dp))
                            Text("Minecraft account", color = theme.textColorSecondary, fontSize = 11.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("YOUR TAG", color = theme.textColorSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        BasicTextField(draft.tag, { input -> onChange(draft.copy(tag = input.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }.take(5))) },
                            enabled = loaded && !busy, singleLine = true,
                            textStyle = TextStyle(color = theme.textColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = theme.typography.family),
                            cursorBrush = SolidColor(Accent),
                            modifier = Modifier.width(106.dp).height(42.dp).trackTextInputFocus()
                                .semantics { contentDescription = "Profile tag, up to five letters or numbers" }
                                .reConfigGlass(RoundedCornerShape(10.dp), theme.componentBackground, theme.borderColor).padding(11.dp),
                            decorationBox = { inner -> Box { if (draft.tag.isEmpty()) Text("TAG", color = theme.textColorSecondary.copy(.5f), fontSize = 17.sp); inner() } })
                        Text("${draft.tag.length}/5", color = theme.textColorSecondary, fontSize = 10.sp)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor.copy(.14f)))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("About you", color = theme.textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${draft.bio.length}/280", color = theme.textColorSecondary, fontSize = 11.sp)
                }
                BasicTextField(draft.bio, { input -> onChange(draft.copy(bio = input.filter { it >= ' ' && it != '§' && it != '\u007f' }.take(280))) },
                    enabled = loaded && !busy, textStyle = TextStyle(color = theme.textColor, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = theme.typography.family),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 86.dp).trackTextInputFocus()
                        .semantics { contentDescription = "Profile description" }
                        .reConfigGlass(RoundedCornerShape(11.dp), theme.componentBackground, theme.borderColor).padding(12.dp),
                    decorationBox = { inner -> Box { if (draft.bio.isEmpty()) Text("A little about you…", color = theme.textColorSecondary.copy(.6f), fontSize = 13.sp); inner() } })
            }
            ProfileSection("Make it yours", "Preview changes here. Save to apply your tag in game.") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileTag(draft.tag, draft.emoji)
                    Spacer(Modifier.weight(1f))
                    ProfileAction(if (picker) "Close emoji picker" else "Choose emoji", enabled = loaded && !busy) { picker = !picker }
                }
                AnimatedVisibility(picker) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ProfileAction("None", selected = draft.emoji.isEmpty()) { onChange(draft.copy(emoji = "")) }
                        profileEmojis.forEach { id ->
                            val source = rememberInteractionSource()
                            val hovered by source.collectIsHoveredAsState()
                            Box(Modifier.size(36.dp).background(if (draft.emoji == id) Accent.copy(.18f) else if (hovered) theme.componentBackground else Color.Transparent, RoundedCornerShape(9.dp))
                                .border(1.dp, if (draft.emoji == id) Accent.copy(.5f) else Color.Transparent, RoundedCornerShape(9.dp))
                                .semantics { contentDescription = "Use $id emoji" }.onClick(source, !busy) { onChange(draft.copy(emoji = id)) }, contentAlignment = Alignment.Center) {
                                Icon("/assets/reconfig/emojis/$id.png", modifier = Modifier.size(25.dp))
                            }
                        }
                    }
                }
            }
            ProfileSection("Profile visibility", "You decide who can discover your profile.") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Publish my profile", color = theme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("I consent to sharing my name, UUID, bio, tag and emoji in public profile search and live ReConfig nametags.", color = theme.textColorSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    SwitchControl(draft.public) { if (loaded && !busy) onChange(draft.copy(public = it)) }
                }
                Text(if (draft.public) "Save to publish these changes." else "Private profiles are hidden from public search.", color = theme.textColorSecondary, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (!loaded) if (failed) "Profile unavailable" else "Loading profile…" else if (dirty) "Unsaved changes" else "All changes saved", color = if (dirty) Accent else theme.textColorSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                if (dirty) ProfileAction("Reset", enabled = !busy) { onReset() }
                ProfileAction(if (busy) "Saving…" else "Save changes", primary = true, enabled = loaded && dirty && !busy) { onSave() }
            }
            AnimatedVisibility(notice.isNotBlank()) {
                Row(Modifier.fillMaxWidth().background((if (failed) Color(0xFFE28E91) else Accent).copy(.07f), RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(notice, color = if (failed) Color(0xFFE9A5A8) else theme.textColorSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (!loaded && failed) ProfileAction("Retry") { onRetry() }
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 700.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            stage(Modifier.width(290.dp)); details(Modifier.weight(1f))
        } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            stage(Modifier.fillMaxWidth()); details(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ProfileSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalTheme.current
    Column(Modifier.fillMaxWidth().reConfigGlass(RoundedCornerShape(16.dp), theme.modCardBackground, theme.borderColor).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = theme.textColorSecondary, fontSize = 11.sp, lineHeight = 16.sp)
        }
        content()
    }
}

@Composable
internal fun ProfileTag(tag: String, emoji: String) {
    val theme = LocalTheme.current
    Row(Modifier.heightIn(min = 30.dp).background(theme.componentBackground.copy(.85f), RoundedCornerShape(8.dp)).border(1.dp, theme.borderColor.copy(.2f), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (emoji in profileEmojis) Icon("/assets/reconfig/emojis/$emoji.png", modifier = Modifier.size(20.dp))
        Text(tag.ifEmpty { "TAG" }, color = if (tag.isEmpty()) theme.textColorSecondary.copy(.45f) else theme.textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun ProfilePill(label: String) {
    val theme = LocalTheme.current
    Text(label, color = theme.textColorSecondary, fontSize = 10.sp,
        modifier = Modifier.background(Color.White.copy(.035f), RoundedCornerShape(20.dp)).padding(horizontal = 9.dp, vertical = 5.dp))
}

@Composable
internal fun ProfileAction(label: String, primary: Boolean = false, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .975f else 1f, tween(130))
    val fill by animateColorAsState(when { !enabled -> theme.componentBackground.copy(.4f); primary -> Accent.copy(if (hovered) .92f else .78f); selected -> Accent.copy(.17f); hovered -> theme.componentBackground; else -> theme.componentBackground.copy(.65f) }, tween(160))
    Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.reConfigGlass(RoundedCornerShape(10.dp), fill, if (primary) Accent else theme.borderColor)
        .onClick(source, enabled, onClick).padding(horizontal = 13.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (!enabled) theme.textColorSecondary.copy(.45f) else if (primary) theme.accentTextColor else theme.textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ProfileSearchBar(query: String, searching: Boolean, onMode: (Boolean) -> Unit, onQuery: (String) -> Unit) {
    val theme = LocalTheme.current
    val interaction = rememberInteractionSource()
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val expanded = hovered || focused || searching
    val width by animateDpAsState(if (expanded) 320.dp else 42.dp, tween(230))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.width(width).height(42.dp).clip(RoundedCornerShape(12.dp)).hoverable(interaction)
            .reConfigGlass(RoundedCornerShape(12.dp), theme.modCardBackground, theme.borderColor),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).semantics { contentDescription = "Search player profiles" }.onClick(rememberInteractionSource()) { onMode(true) }, contentAlignment = Alignment.Center) {
                Icon("search", color = if (expanded) Accent else theme.textColorSecondary, modifier = Modifier.size(18.dp))
            }
            if (expanded) BasicTextField(query, { onQuery(it); onMode(true) }, singleLine = true,
                textStyle = TextStyle(color = theme.textColor, fontSize = 13.sp, fontFamily = theme.typography.family), cursorBrush = SolidColor(Accent),
                modifier = Modifier.weight(1f).padding(end = 10.dp).trackTextInputFocus().onFocusChanged { focused = it.isFocused; if (it.isFocused) onMode(true) }
                    .semantics { contentDescription = "Search by player name or tag" },
                decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search players…", color = theme.textColorSecondary, fontSize = 13.sp); inner() } })
        }
        Spacer(Modifier.weight(1f))
        if (searching) ProfileAction("Back to your profile") { onMode(false) }
        else Text("Your profile", color = theme.textColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
