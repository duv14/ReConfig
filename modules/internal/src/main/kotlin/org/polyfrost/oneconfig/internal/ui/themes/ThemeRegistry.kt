/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.api.settings.BuiltinVisualizers
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundTheme

object ThemeRegistry {
    internal val registry = mutableStateListOf<UITheme>()
    internal var activeTheme by mutableStateOf<UITheme?>(null)

    private const val DEFAULT_THEME_NAME = "PolyGlass Dark"

    init {
        register(PolyGlassDark)
        register(PolyGlassLight)
        register(MinecraftDark)
        register(MinecraftLight)

        activeTheme = PolyGlassDark
        syncNotificationTheme(PolyGlassDark)

        BuiltinVisualizers.register()
    }

    fun init() {}

    fun register(theme: UITheme) {
        registry += theme
    }

    fun activate(theme: UITheme) {
        val previous = activeTheme
        activeTheme = theme
        ThemeConfig.activeTheme = theme.name
        syncNotificationTheme(theme)
        saveThemeToConfig()
        if (previous != null) {
            if (UiSoundTheme.of(previous) != UiSoundTheme.of(theme)) {
                UiSounds.onSoundThemeChanged()
            } else {
                UiSounds.play(UiSoundEvent.CLICK)
            }
        }
    }

    fun loadFromConfig() {
        try {
            activeTheme = PolyGlassDark
            ThemeConfig.activeTheme = PolyGlassDark.name
            syncNotificationTheme(PolyGlassDark)
        } catch (_: Exception) {}
    }

    private fun saveThemeToConfig() {
        try {
            ConfigManager.active().save("themes.json")
        } catch (_: Exception) {}
    }
}
