/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.awt.AlphaComposite
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Flow
import javax.imageio.ImageIO

/** Official profile/texture requests only. This cache contains public head images, never tokens. */
internal data class PlayerSkin(val png: ByteArray, val headPng: ByteArray, val slim: Boolean)
internal object OfficialHeads {
    private data class Entry(val skin: PlayerSkin?, val expires: Long)
    private val cache = LinkedHashMap<String, Entry>(256, .75f, true)
    private val locks = Array(16) { Mutex() }
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
        .followRedirects(HttpClient.Redirect.NEVER).build()
    private val uuidPattern = Regex("[0-9a-f]{32}")
    private val namePattern = Regex("[A-Za-z0-9_]{1,16}")

    suspend fun load(username: String, uuid: String): ByteArray? = loadSkin(username, uuid)?.headPng

    suspend fun loadSkin(username: String, uuid: String): PlayerSkin? = withContext(Dispatchers.IO) {
        val id = uuid.replace("-", "").lowercase().takeIf { uuidPattern.matches(it) }
        val name = username.takeIf { namePattern.matches(it) }
        val key = id ?: name?.lowercase() ?: return@withContext null
        locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            synchronized(cache) { cache[key] }?.takeIf { it.expires > System.currentTimeMillis() }?.let { return@withLock it.skin }
            val png = try {
                val resolved = id ?: JsonParser.parseString(String(fetch(URI("https://api.mojang.com/users/profiles/minecraft/$name"), 32_768), Charsets.UTF_8))
                    .asJsonObject.get("id").asString.lowercase().also { require(uuidPattern.matches(it)) }
                val profile = JsonParser.parseString(String(fetch(URI("https://sessionserver.mojang.com/session/minecraft/profile/$resolved"), 65_536), Charsets.UTF_8)).asJsonObject
                val textureProperty = profile.getAsJsonArray("properties").firstOrNull { it.asJsonObject.get("name")?.asString == "textures" }
                    ?.asJsonObject?.get("value")?.asString ?: error("No texture")
                val textures = JsonParser.parseString(String(Base64.getDecoder().decode(textureProperty), Charsets.UTF_8)).asJsonObject
                val original = URI(textures.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").asString)
                require(original.host == "textures.minecraft.net" && original.rawUserInfo == null && original.port == -1)
                require(original.path.matches(Regex("/texture/[0-9a-fA-F]+")))
                val secureTexture = URI("https", "textures.minecraft.net", original.path, null)
                val raw = fetch(secureTexture, 1_048_576)
                val head = crop(raw)
                val slim = textures.getAsJsonObject("textures").getAsJsonObject("SKIN").getAsJsonObject("metadata")?.get("model")?.asString == "slim"
                PlayerSkin(normalize(raw), head, slim)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                null
            } catch (_: Exception) { null }
            synchronized(cache) {
                cache[key] = Entry(png, System.currentTimeMillis() + if (png == null) 60_000L else 30 * 60_000L)
                while (cache.size > 256) cache.remove(cache.keys.first())
            }
            png
        }
    }

    private fun fetch(uri: URI, limit: Int): ByteArray {
        val response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(6)).GET().build(),
            HttpResponse.BodyHandler { BoundedBody(limit) })
        check(response.statusCode() == 200)
        return response.body()
    }

    /** Enforces the body cap while receiving, before allocating an unbounded response buffer. */
    private class BoundedBody(private val limit: Int) : HttpResponse.BodySubscriber<ByteArray> {
        private val result = CompletableFuture<ByteArray>()
        private val bytes = ByteArrayOutputStream()
        private var subscription: Flow.Subscription? = null
        override fun getBody(): CompletionStage<ByteArray> = result
        override fun onSubscribe(value: Flow.Subscription) { subscription = value; value.request(1) }
        override fun onNext(items: List<ByteBuffer>) {
            for (item in items) {
                if (item.remaining() > limit - bytes.size()) {
                    subscription?.cancel()
                    result.completeExceptionally(IllegalArgumentException("Image response too large"))
                    return
                }
                val chunk = ByteArray(item.remaining())
                item.get(chunk)
                bytes.write(chunk)
            }
            subscription?.request(1)
        }
        override fun onError(error: Throwable) { result.completeExceptionally(error) }
        override fun onComplete() { result.complete(bytes.toByteArray()) }
    }

    /** Called only after crop has validated dimensions. Keep the shared cache small. */
    private fun normalize(bytes: ByteArray): ByteArray {
        val source = ImageIO.read(ByteArrayInputStream(bytes))
        val target = BufferedImage(64, if (source.height == source.width) 64 else 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = target.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            graphics.drawImage(source, 0, 0, target.width, target.height, null)
        } finally { graphics.dispose(); source.flush() }
        return ByteArrayOutputStream().use { output ->
            check(ImageIO.write(target, "png", output))
            target.flush()
            output.toByteArray()
        }
    }

    private fun crop(bytes: ByteArray): ByteArray {
        val skin = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val readers = ImageIO.getImageReaders(input)
            require(readers.hasNext())
            val reader = readers.next()
            try {
                reader.input = input
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                require(width in 64..1024 && width % 64 == 0 && (height == width || height * 2 == width))
                reader.read(0)
            } finally { reader.dispose() }
        }
        val unit = skin.width / 64
        val head = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        val graphics = head.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            graphics.composite = AlphaComposite.Src
            graphics.drawImage(skin, 0, 0, 64, 64, 8 * unit, 8 * unit, 16 * unit, 16 * unit, null)
            // Legacy skins use opaque unused hat pixels; Minecraft treats those as no overlay.
            val hasHat = skin.height == skin.width || (32 * unit until 64 * unit).any { x ->
                (0 until 32 * unit).any { y -> (skin.getRGB(x, y) ushr 24) < 128 }
            }
            if (hasHat) {
                graphics.composite = AlphaComposite.SrcOver
                graphics.drawImage(skin, 0, 0, 64, 64, 40 * unit, 8 * unit, 48 * unit, 16 * unit, null)
            }
        } finally { graphics.dispose(); skin.flush() }
        return ByteArrayOutputStream().use { output ->
            check(ImageIO.write(head, "png", output))
            head.flush()
            output.toByteArray()
        }
    }
}

@Composable
fun PlayerHead(modifier: Modifier = Modifier) {
    HeadImage(ShellState.playerHeadPng, modifier)
}

@Composable
fun PlayerHead(uuid: String, modifier: Modifier = Modifier) {
    PlayerHead("", uuid, modifier)
}

@Composable
fun PlayerHead(username: String, uuid: String, modifier: Modifier = Modifier) {
    var bytes by remember(username, uuid) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(username, uuid) { bytes = OfficialHeads.load(username, uuid) }
    HeadImage(bytes, modifier)
}

@Composable
private fun HeadImage(bytes: ByteArray?, modifier: Modifier) {
    val fallback = painterResource("/assets/oneconfig/images/head.png")
    val painter = remember(bytes) {
        bytes?.let { runCatching { BitmapPainter(loadImageBitmap(ByteArrayInputStream(it))) }.getOrNull() }
    }
    val theme = LocalTheme.current
    Image(painter ?: fallback, null, modifier.clip(theme.sideBarNavigationEntryShape)
        .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape))
}

