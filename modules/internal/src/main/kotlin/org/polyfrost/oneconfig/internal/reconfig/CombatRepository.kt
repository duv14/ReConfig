/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Integrates duv14's supplied Hitbox Categories and Team Highlight models.
 */
package org.polyfrost.oneconfig.internal.reconfig

import com.google.gson.GsonBuilder
import org.polyfrost.oneconfig.internal.reconfig.combat.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Shared by the integrated module editor and Minecraft hooks, without another entrypoint. */
object CombatRepository {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val directory = Path.of(System.getProperty("user.dir"), "config", "reconfig")
    var error: String? = null; private set
    private fun <T> load(name: String, type: Class<T>, fallback: () -> T): T = try {
        val path = directory.resolve(name)
        if (Files.exists(path)) Files.newBufferedReader(path).use { gson.fromJson(it, type) } ?: fallback() else fallback()
    } catch (e: Exception) { error = "Cannot load $name: ${e.message}"; fallback() }
    @JvmStatic val hitboxes: HitboxCategoriesConfig = load("hitbox-categories.json", HitboxCategoriesConfig::class.java, HitboxCategoriesConfig::defaults).also { it.sanitize() }
    @JvmStatic val highlights: HighlightConfig = load("team-highlight.json", HighlightConfig::class.java, ::HighlightConfig).also { it.sanitize() }
    @JvmStatic val flashes = HitFlashTracker(1000)
    fun save(): Boolean = try {
        Files.createDirectories(directory)
        for ((name, model) in listOf("hitbox-categories.json" to hitboxes, "team-highlight.json" to highlights)) {
            val destination = directory.resolve(name)
            val temp = Files.createTempFile(directory, name, ".tmp")
            try {
                Files.newBufferedWriter(temp).use { gson.toJson(model, it) }
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING)
            } finally { Files.deleteIfExists(temp) }
        }
        error = null; true
    } catch (e: Exception) { error = "Cannot save combat settings: ${e.message}"; false }
}
