/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.debug.DebugScreenEntries
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus
import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.reconfig.modules.AutoTextController
import org.polyfrost.oneconfig.internal.reconfig.modules.FreeLookController
import org.polyfrost.oneconfig.internal.reconfig.modules.ModuleHuds
import org.polyfrost.oneconfig.internal.reconfig.modules.ProjectilePrediction
import org.polyfrost.oneconfig.internal.reconfig.modules.WaypointRuntime

/** Edge-triggered dispatcher shared by every ReConfig gameplay module. */
object ReConfigModuleRuntime {
    private val previouslyDown = HashMap<String, Boolean>()
    private var started = false
    private var lastHitboxRendered: Boolean? = null

    @JvmStatic fun start() {
        if (started) return
        started = true
        ModuleHuds.register()
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
    }

    private fun tick(mc: Minecraft) {
        ModuleHuds.tick(mc)
        WaypointRuntime.tick(mc)
        ProjectilePrediction.tick(mc)
        val window = Platform.compatibility().windowHandle()
        if (window == 0L) return
        val inputBlocked = mc.player == null || mc.screen != null || KeyCapture.moduleId != null || GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE
        ModuleCatalog.modules.forEach { module ->
            if (module.id == "freelook" && ModuleAccess.choice("freelook", "mode", "Hold") == "Hold") return@forEach
            if (risingEdge("${module.id}:toggle", module.key("toggle_key"), window, inputBlocked)) module.toggle()
        }
        val freelook = ModuleCatalog.byId("freelook")!!
        val holding = !inputBlocked && freelook.key() > 0 && GLFW.glfwGetKey(window, freelook.key()) == GLFW.GLFW_PRESS
        FreeLookController.update(mc, if (ModuleAccess.choice("freelook", "mode", "Hold") == "Hold") freelook.enabled && holding else freelook.enabled && !inputBlocked)
        syncHitboxes(mc)
        val module = ModuleCatalog.byId("auto_text") ?: return
        if (risingEdge("auto_text:send", module.key("send_key"), window, inputBlocked) && module.enabled) {
            AutoTextController.onSend(mc, module)
        }
    }

    private fun syncHitboxes(mc: Minecraft) {
        val module = ModuleCatalog.byId("hitbox") ?: return
        val entries = mc.debugEntries
        val rendered = entries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)
        val last = lastHitboxRendered
        if (last != null && rendered != last) module.updateEnabled(rendered)
        else if (rendered != module.enabled) {
            entries.setStatus(
                DebugScreenEntries.ENTITY_HITBOXES,
                if (module.enabled) DebugScreenEntryStatus.ALWAYS_ON else DebugScreenEntryStatus.NEVER,
            )
        }
        lastHitboxRendered = entries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)
    }

    private fun risingEdge(token: String, key: Int, window: Long, blocked: Boolean): Boolean {
        val down = key in GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS
        val wasDown = previouslyDown.put(token, down) == true
        return !blocked && down && !wasDown
    }
}
