/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.internal.ui.screens.ReConfigSocialService

object ReConfigSocialBootstrap {
    @JvmStatic
    fun start() {
        runCatching {
            val minecraft = Minecraft.getInstance()
            val user = minecraft.user
            val name = user.name
            val uuid = runCatching {
                val method = user.javaClass.methods.firstOrNull {
                    it.name in setOf("getProfileId", "profileId") && it.parameterCount == 0
                }
                method?.invoke(user)?.toString()?.replace("-", "") ?: ""
            }.getOrDefault("")
            ReConfigSocialService.start(name, uuid)
        }
    }
}
