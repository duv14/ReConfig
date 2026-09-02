/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.client.Minecraft
import net.minecraft.world.level.storage.LevelResource
import org.polyfrost.oneconfig.internal.reconfig.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object WaypointRuntime {
    private var previousPlayer: Any? = null
    private var dead = false
    fun tick(mc: Minecraft) {
        val player=mc.player;val level=mc.level
        if(player==null||level==null){WaypointRepository.context=null;previousPlayer=null;dead=false;return}
        val world=mc.singleplayerServer?.getWorldPath(LevelResource.ROOT)?.toAbsolutePath()?.normalize()?.toString()?.let{"local:$it"}
            ?:mc.currentServer?.ip?.lowercase(Locale.ROOT)?.let{"server:$it"}
        if(world==null){WaypointRepository.context=null;return}
        val context=WaypointContext(world,level.dimension().identifier().toString(),player.x,player.y,player.z)
        WaypointRepository.context=context
        if(previousPlayer!==player){previousPlayer=player;dead=false}
        val nowDead=player.isDeadOrDying
        if(nowDead&&!dead&&ModuleAccess.enabled("waypoints")&&ModuleAccess.choice("waypoints","death_waypoints","false").toBoolean()) {
            runCatching { WaypointRepository.store?.add(world,context.dimension,"Death "+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),context.x,context.y,context.z,0xFFFF7078.toInt()) }
                .onFailure { WaypointRepository.error=it.message }
        }
        dead=nowDead
    }
}
