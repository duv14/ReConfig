/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Composable
fun PlayerHead(modifier: Modifier = Modifier) {
    val shape = LocalTheme.current.sideBarNavigationEntryShape
    val fallback = painterResource("/assets/oneconfig/images/head.png")
    val bytes = ShellState.playerHeadPng
    val painter = remember(bytes) {
        bytes?.let { BitmapPainter(loadImageBitmap(ByteArrayInputStream(it))) }
    }

    Image(
        painter = painter ?: fallback,
        contentDescription = null,
        modifier = modifier
            .clip(shape)
            .border(1.dp, LocalTheme.current.borderColor, shape),
    )
}

@Composable
fun PlayerHead(uuid: String, modifier: Modifier = Modifier) {
    var bytes by remember(uuid) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(uuid) {
        bytes = runCatching {
            HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("https://mc-heads.net/avatar/${uuid.replace("-", "")}/64.png")).build(),
                HttpResponse.BodyHandlers.ofByteArray(),
            ).body()
        }.getOrNull()
    }
    val fallback = painterResource("/assets/oneconfig/images/head.png")
    val painter = remember(bytes) { bytes?.let { runCatching { BitmapPainter(loadImageBitmap(ByteArrayInputStream(it))) }.getOrNull() } }
    val shape = LocalTheme.current.sideBarNavigationEntryShape
    Image(painter ?: fallback, null, modifier.clip(shape).border(1.dp, LocalTheme.current.borderColor, shape))
}

@Composable
fun PlayerHead(username: String, uuid: String, modifier: Modifier = Modifier) {
    val identity = "$username:$uuid"
    var bytes by remember(identity) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(identity) {
        bytes = listOf(username, uuid.replace("-", "")).firstNotNullOfOrNull { identifier ->
            runCatching {
                val response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("https://mc-heads.net/avatar/$identifier/64.png")).build(),
                    HttpResponse.BodyHandlers.ofByteArray(),
                )
                response.body().takeIf { response.statusCode() in 200..299 && it.isNotEmpty() }
            }.getOrNull()
        }
    }
    val fallback = painterResource("/assets/oneconfig/images/head.png")
    val painter = remember(bytes) { bytes?.let { runCatching { BitmapPainter(loadImageBitmap(ByteArrayInputStream(it))) }.getOrNull() } }
    val shape = LocalTheme.current.sideBarNavigationEntryShape
    Image(painter ?: fallback, null, modifier.clip(shape).border(1.dp, LocalTheme.current.borderColor, shape))
}
