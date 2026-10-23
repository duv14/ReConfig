/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.internal.ui.screens.ReConfigSocialService
import org.polyfrost.oneconfig.internal.ui.screens.CommunityActions
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import java.security.SecureRandom

object ReConfigSocialBootstrap {
    @JvmStatic
    fun start() {
        runCatching {
            val minecraft = Minecraft.getInstance()
            val user = minecraft.user
            val name = user.name
            val uuid = user.profileId.toString().replace("-", "")
            ReConfigSocialService.enrollmentProof = {
                val serverId = ByteArray(20).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
                minecraft.services().sessionService().joinServer(user.profileId, user.accessToken, serverId)
                serverId
            }
            CommunityActions.connect = { address ->
                minecraft.execute {
                    ConnectScreen.startConnecting(minecraft.screen ?: TitleScreen(), minecraft, ServerAddress.parseString(address), ServerData("Public server", address, ServerData.Type.OTHER), false, null)
                }
            }
            CommunityActions.status = PublicServerProbe::query
            ReConfigSocialService.start(name, uuid)
        }
    }
}
