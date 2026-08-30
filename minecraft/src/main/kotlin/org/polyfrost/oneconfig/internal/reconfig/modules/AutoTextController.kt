/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.internal.reconfig.ClientModule

object AutoTextController {
    fun onSend(mc: Minecraft, module: ClientModule) {
        val setting = module.settings.firstOrNull { setting -> setting.id == "message" } ?: return
        val message = module.value(setting).trim().take(256)
        if (message.isBlank()) return
        val connection = mc.player?.connection ?: return
        connection.sendChat(message)
    }
}
