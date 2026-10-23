/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CompletableFuture
import java.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/** Local Windows media session metadata; never inspects browser titles or uploads listening activity. */
object BrowserMusicBridge {
    data class Media(val title: String, val artist: String, val playing: Boolean, val position: Long,
        val duration: Long, val sampledAt: Long = System.nanoTime() / 1_000_000L,
        val source: String = "Spotify", val sessionId: String = "",
        val artworkPng: ByteArray? = null, val canPlayPause: Boolean = false,
        val canPrevious: Boolean = false, val canNext: Boolean = false) {
        fun currentSeconds() = MediaTimeline.position(position, duration, playing, sampledAt, System.nanoTime() / 1_000_000L)
    }
    @Volatile var media: Media? = null
        private set
    val title: String get() = media?.title ?: "No supported music session"
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "ReConfig-BrowserMusic").apply { isDaemon = true } }
    private val running = AtomicBoolean(false)
    private var nextPollAt = 0L
    private val artworkCache = LinkedHashMap<String, ByteArray?>(8, .75f, true)
    enum class Action { PLAY_PAUSE, PREVIOUS, NEXT }
    init {
        org.polyfrost.oneconfig.internal.reconfig.MediaControls.snapshot = {
            val current = media
            val enabled = ModuleAccess.enabled("browser_music_hud") && !running.get()
            org.polyfrost.oneconfig.internal.reconfig.MediaControls.State(current?.title ?: "No supported music session",
                current?.playing == true, enabled && current?.canPlayPause == true,
                enabled && current?.canPrevious == true, enabled && current?.canNext == true)
        }
        org.polyfrost.oneconfig.internal.reconfig.MediaControls.command = { value ->
            runCatching { control(Action.valueOf(value)) }.getOrDefault(false)
        }
    }

    /** Revalidate the exact session and its capabilities at execution time; never send global media keys. */
    fun control(action: Action): Boolean {
        val snapshot = media ?: return false
        if (!ModuleAccess.enabled("browser_music_hud") || snapshot.sessionId.isBlank()) return false
        val supported = when (action) {
            Action.PLAY_PAUSE -> snapshot.canPlayPause
            Action.PREVIOUS -> snapshot.canPrevious
            Action.NEXT -> snapshot.canNext
        }
        if (!supported || !running.compareAndSet(false, true)) return false
        worker.execute {
            try {
                val id = Base64.getEncoder().encodeToString(snapshot.sessionId.toByteArray(Charsets.UTF_8))
                runScript("\$target = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('$id'))\n\$action = '${action.name}'\n" + MEDIA_SCRIPT)
                media = if (ModuleAccess.enabled("browser_music_hud")) runCatching { detectMedia() }.getOrNull() else null
            } finally { running.set(false) }
        }
        return true
    }

    fun tick(nowMs: Long) {
        if (!ModuleAccess.enabled("browser_music_hud")) { media = null; return }
        if (nowMs < nextPollAt || !running.compareAndSet(false, true)) return
        nextPollAt = nowMs + 5000L
        worker.execute {
            try {
                val detected = runCatching { detectMedia() }.getOrNull()
                media = if (ModuleAccess.enabled("browser_music_hud")) detected else null
            }
            finally { running.set(false) }
        }
    }

    private fun detectMedia(): Media? {
        if (!System.getProperty("os.name", "").lowercase(Locale.ROOT).startsWith("windows")) {
            return null
        }
        val payload = runScript(MEDIA_SCRIPT) ?: return null
        val data = Gson().fromJson(payload, JsonObject::class.java)
        val title = data["title"].asString.filter { !it.isISOControl() }.take(160)
        if (title.isBlank()) return null
        val artist = data["artist"].asString.filter { !it.isISOControl() }.take(100)
        val encoded = data["artwork"]?.asString.orEmpty()
        val key = data["sessionId"].asString + "\n" + title + "\n" + artist
        val artwork = if (artworkCache.containsKey(key)) artworkCache[key] else {
            val decoded = runCatching { decodeArtwork(encoded) }.getOrNull()
            artworkCache[key] = decoded
            while (artworkCache.size > 8) artworkCache.remove(artworkCache.keys.first())
            decoded
        }
        return Media(title, artist, data["playing"].asBoolean,
            data["position"].asDouble.toLong().coerceIn(0, 86400), data["duration"].asDouble.toLong().coerceIn(0, 86400),
            sessionId = data["sessionId"].asString.take(256), artworkPng = artwork,
            canPlayPause = data["canPlayPause"].asBoolean, canPrevious = data["canPrevious"].asBoolean, canNext = data["canNext"].asBoolean)
    }

    private fun decodeArtwork(encoded: String): ByteArray? {
        if (encoded.isBlank() || encoded.length > 1_400_000) return null
        val bytes = Base64.getDecoder().decode(encoded)
        val source = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            try {
                reader.input = input
                require(reader.getWidth(0) in 1..2048 && reader.getHeight(0) in 1..2048)
                reader.read(0)
            } finally { reader.dispose() }
        }
        val image = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try { graphics.drawImage(source, 0, 0, 256, 256, null) }
        finally { graphics.dispose(); source.flush() }
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output); image.flush(); output.toByteArray()
        }
    }

    private fun runScript(script: String): String? {
        val process = runCatching {
            ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start()
        }.getOrNull() ?: return null
        // Drain concurrently: artwork can exceed the OS pipe capacity before the process exits.
        val output = CompletableFuture.supplyAsync { process.inputStream.use { it.readNBytes(1_450_001) } }
        try {
            if (!process.waitFor(6000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) return null
            val bytes = output.get(500, TimeUnit.MILLISECONDS)
            if (bytes.size > 1_450_000) return null
            return String(bytes, Charsets.UTF_8).trim().removePrefix("\uFEFF").takeIf { it.isNotBlank() }
        } finally { process.destroyForcibly(); output.cancel(true) }
    }

    private val MEDIA_SCRIPT = """
${'$'}ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
${'$'}null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
function Await(${'$'}op, ${'$'}type) {
 ${'$'}method = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { ${'$'}_.Name -eq 'AsTask' -and ${'$'}_.IsGenericMethod -and ${'$'}_.GetParameters().Count -eq 1 -and ${'$'}_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
 ${'$'}task = ${'$'}method.MakeGenericMethod(${'$'}type).Invoke(${'$'}null, @(${'$'}op))
 if (!${'$'}task.Wait(1500)) { throw 'Timeout' }
 ${'$'}task.Result
}
${'$'}manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
foreach (${'$'}session in ${'$'}manager.GetSessions()) {
 if (${'$'}session.SourceAppUserModelId -notmatch '(?i)^Spotify(?:\.exe|AB\.SpotifyMusic_.*)?${'$'}') { continue }
 if (${'$'}target -and ${'$'}session.SourceAppUserModelId -ne ${'$'}target) { continue }
 ${'$'}playback = ${'$'}session.GetPlaybackInfo()
 ${'$'}controls = ${'$'}playback.Controls
 if (${'$'}action) {
  ${'$'}operation = ${'$'}null
  if (${'$'}action -eq 'PLAY_PAUSE' -and ${'$'}controls.IsPlayPauseToggleEnabled) { ${'$'}operation = ${'$'}session.TryTogglePlayPauseAsync() }
  if (${'$'}action -eq 'PREVIOUS' -and ${'$'}controls.IsPreviousEnabled) { ${'$'}operation = ${'$'}session.TrySkipPreviousAsync() }
  if (${'$'}action -eq 'NEXT' -and ${'$'}controls.IsNextEnabled) { ${'$'}operation = ${'$'}session.TrySkipNextAsync() }
  if (${'$'}operation) { ${'$'}null = Await ${'$'}operation ([bool]) }
  break
 }
 ${'$'}media = Await (${'$'}session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime])
 ${'$'}status = ${'$'}session.GetPlaybackInfo().PlaybackStatus.ToString()
 if (!${'$'}media.Title -or ${'$'}status -notin @('Playing','Paused')) { continue }
 ${'$'}timeline = ${'$'}session.GetTimelineProperties()
 ${'$'}title = ${'$'}media.Title.Substring(0,[Math]::Min(160,${'$'}media.Title.Length))
 ${'$'}artist = [string]${'$'}media.Artist
 ${'$'}artist = ${'$'}artist.Substring(0,[Math]::Min(100,${'$'}artist.Length))
 ${'$'}artwork = ''
 try {
  if (${'$'}media.Thumbnail) {
   ${'$'}stream = Await (${'$'}media.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType=WindowsRuntime])
   try {
    if (${'$'}stream.Size -gt 0 -and ${'$'}stream.Size -le 1048576) {
     ${'$'}reader = [Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType=WindowsRuntime]::new(${'$'}stream)
     try {
      ${'$'}size = [uint32]${'$'}stream.Size
      ${'$'}loaded = Await (${'$'}reader.LoadAsync(${'$'}size)) ([uint32])
      if (${'$'}loaded -eq ${'$'}size) {
       ${'$'}bytes = New-Object byte[] ${'$'}size
       ${'$'}reader.ReadBytes(${'$'}bytes)
       ${'$'}artwork = [Convert]::ToBase64String(${'$'}bytes)
      }
     } finally { ${'$'}reader.Dispose() }
    }
   } finally { ${'$'}stream.Dispose() }
  }
 } catch { ${'$'}artwork = '' }
 @{ title=${'$'}title; artist=${'$'}artist; sessionId=${'$'}session.SourceAppUserModelId; artwork=${'$'}artwork; canPlayPause=${'$'}controls.IsPlayPauseToggleEnabled; canPrevious=${'$'}controls.IsPreviousEnabled; canNext=${'$'}controls.IsNextEnabled; playing=(${'$'}status -eq 'Playing'); position=[Math]::Max(0,${'$'}timeline.Position.TotalSeconds); duration=[Math]::Max(0,${'$'}timeline.EndTime.TotalSeconds) } | ConvertTo-Json -Compress
 break
}
""".trimIndent()
}
