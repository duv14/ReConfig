/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Best-effort local browser media label. On Windows this reads browser main-window titles only;
 * it never contacts a network service and never uploads the title.
 */
object BrowserMusicBridge {
    @Volatile var title: String = "No browser media detected"
        private set
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "ReConfig-BrowserMusic").apply { isDaemon = true } }
    private val running = AtomicBoolean(false)
    private var nextPollAt = 0L

    fun tick(nowMs: Long) {
        if (!ModuleAccess.enabled("browser_music_hud")) return
        if (nowMs < nextPollAt || !running.compareAndSet(false, true)) return
        nextPollAt = nowMs + 2500L
        worker.execute {
            try { title = detectTitle() ?: "No browser media detected" }
            finally { running.set(false) }
        }
    }

    private fun detectTitle(): String? {
        if (!System.getProperty("os.name", "").lowercase(Locale.ROOT).contains("win")) {
            return "Browser Music is supported on Windows"
        }
        val wanted = ModuleAccess.choice("browser_music_hud", "browser", "Auto")
        val processNames = when (wanted) {
            "Chrome" -> listOf("chrome")
            "Edge" -> listOf("msedge")
            "Firefox" -> listOf("firefox")
            else -> listOf("chrome", "msedge", "firefox")
        }
        val names = processNames.joinToString(",") { name -> "'$name'" }
        val script = "Get-Process -Name $names -ErrorAction SilentlyContinue | Where-Object { ${'$'}_.MainWindowTitle } | Select-Object -ExpandProperty MainWindowTitle"
        val process = runCatching {
            ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
                .redirectErrorStream(true).start()
        }.getOrNull() ?: return null
        if (!process.waitFor(1200, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly(); return null
        }
        return process.inputStream.bufferedReader().useLines { lines ->
            lines.map(String::trim).firstOrNull { it.isNotBlank() }
        }?.replace(Regex("\\s[-–—]\\s(Google Chrome|Microsoft Edge|Mozilla Firefox)$", RegexOption.IGNORE_CASE), "")
            ?.take(120)
    }
}
