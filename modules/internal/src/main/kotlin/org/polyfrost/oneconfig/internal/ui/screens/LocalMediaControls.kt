/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.internal.reconfig.MediaControls
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable internal fun LocalMediaControls() {
    var state by remember { mutableStateOf(MediaControls.State()) }
    var notice by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) { state = MediaControls.snapshot?.invoke() ?: MediaControls.State(); delay(500) }
    }
    val theme = LocalTheme.current
    Column(Modifier.fillMaxWidth().reConfigGlass(theme.modCardShape, theme.componentBackground, theme.borderColor).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(state.title, color = theme.textColor)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Triple("Previous", "PREVIOUS", state.previous), Triple(if (state.playing) "Pause" else "Play", "PLAY_PAUSE", state.playPause), Triple("Next", "NEXT", state.next)).forEach { (label, action, enabled) ->
                val source = rememberInteractionSource()
                val click = if (enabled) Modifier.onClick(source) {
                    notice = if (MediaControls.command?.invoke(action) == true) "Control requested from Spotify." else "Media session changed. Try again shortly."
                } else Modifier
                Box(Modifier.reConfigGlass(theme.buttonShape, theme.componentBackground, theme.borderColor).then(click).padding(12.dp)) {
                    Text(label, color = if (enabled) theme.textColor else theme.textColorSecondary.copy(alpha = .4f))
                }
            }
        }
        Text("Spotify on Windows · controls appear only when the media session supports them. YouTube is unavailable because Windows does not reliably identify a browser session’s website.", color = theme.textColorSecondary, fontSize = 12.sp)
        if (notice.isNotEmpty()) Text(notice, color = theme.textColorSecondary, fontSize = 12.sp)
    }
}
