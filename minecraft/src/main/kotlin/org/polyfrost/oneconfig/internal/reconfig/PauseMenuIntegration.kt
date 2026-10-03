/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.network.chat.Component
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen

/** Adds a small branded ReConfig entry to the vanilla Escape/pause menu. */
object PauseMenuIntegration {
    private var registered = false
    fun register() {
        if (registered) return
        registered = true
        ScreenEvents.AFTER_INIT.register { _, screen, scaledWidth, _ ->
            if (screen !is PauseScreen) return@register
            val button = Button.builder(Component.literal("◆ ReConfig")) { OneConfigUIScreen.openLastSession() }
                .bounds((scaledWidth - 106).coerceAtLeast(4), 8, 98, 20)
                .build()
            //? if >= 26.2 {
            Screens.getWidgets(screen).add(button)
            //? } else {
            /*Screens.getButtons(screen).add(button)
            *///? }
        }
    }
}
