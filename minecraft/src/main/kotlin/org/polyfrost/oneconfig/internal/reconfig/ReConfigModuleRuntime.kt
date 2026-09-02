/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.components.debug.DebugScreenEntries
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus
import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.reconfig.modules.AutoTextController
import org.polyfrost.oneconfig.internal.reconfig.modules.FreeLookController
import org.polyfrost.oneconfig.internal.reconfig.modules.FpsBoostController
import org.polyfrost.oneconfig.internal.reconfig.modules.ModuleHuds
import org.polyfrost.oneconfig.internal.reconfig.modules.ProjectilePrediction
import org.polyfrost.oneconfig.internal.reconfig.modules.WaypointRuntime
import org.polyfrost.oneconfig.internal.reconfig.modules.ActivationState
import org.polyfrost.oneconfig.internal.reconfig.modules.ZoomController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.reconfig.PrivacyConsent

/** Edge-triggered dispatcher shared by every ReConfig gameplay module. */
object ReConfigModuleRuntime {
    private val previouslyDown = HashMap<String, Boolean>()
    private var started = false
    private var lastHitboxRendered: Boolean? = null
    private val freelookActivation = ActivationState()
    private val zoomActivation = ActivationState()
    private var sprintWasEnabled = false
    private var sneakWasEnabled = false
    private var sprintLatched = false
    private var sneakLatched = false
    private var sprintPhysicalWasDown = false
    private var sneakPhysicalWasDown = false
    private var previousLevel: Any? = null

    @JvmStatic fun start() {
        if (started) return
        started = true
        ModuleHuds.register()
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
    }

    private fun tick(mc: Minecraft) {
        if (!PrivacyConsent.accepted) {
            FreeLookController.update(mc, false)
            ZoomController.activate(false)
            return
        }
        if (previousLevel !== mc.level) {
            CombatRepository.flashes.clear()
            previousLevel = mc.level
            lastHitboxRendered = null
        }
        ModuleHuds.tick(mc)
        ShellState.serverTelemetry = if (ModuleAccess.enabled("server_status") && mc.player != null)
            ModuleHuds.serverText.replace(" | ", "\n") else ""
        WaypointRuntime.tick(mc)
        ProjectilePrediction.tick(mc)
        FpsBoostController.update(mc)
        val window = Platform.compatibility().windowHandle()
        if (window == 0L) return
        val inputBlocked = mc.player == null || mc.screen != null || KeyCapture.moduleId != null || GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE
        ModuleCatalog.modules.forEach { module ->
            if (module.id == "freelook") return@forEach
            if (risingEdge("${module.id}:toggle", module.key("toggle_key"), window, inputBlocked)) module.toggle()
        }
        val freelook = ModuleCatalog.byId("freelook")!!
        val holding = isDown(freelook.key(), window)
        FreeLookController.update(mc, freelookActivation.update(freelook.enabled, holding,
            ModuleAccess.choice("freelook", "mode", "Hold") == "Hold", inputBlocked))
        val zoom = ModuleCatalog.byId("zoom")!!
        ZoomController.activate(zoomActivation.update(zoom.enabled, isDown(zoom.key("activation_key"), window),
            ModuleAccess.choice("zoom", "mode", "Hold") == "Hold", inputBlocked))
        val sprint = ModuleAccess.enabled("toggle_sprint")
        val sneak = ModuleAccess.enabled("toggle_sneak")
        val sprintPhysical = physicalKeyDown(mc.options.keySprint.saveString(), window)
        val sneakPhysical = physicalKeyDown(mc.options.keyShift.saveString(), window)
        if (sprint && !inputBlocked) {
            if (sprintPhysical && !sprintPhysicalWasDown) sprintLatched = !sprintLatched
            mc.options.keySprint.setDown(sprintLatched)
        } else {
            if (sprintWasEnabled) sprintLatched = false
            if (!inputBlocked) mc.options.keySprint.setDown(sprintPhysical)
        }
        if (sneak && !inputBlocked) {
            if (sneakPhysical && !sneakPhysicalWasDown) sneakLatched = !sneakLatched
            mc.options.keyShift.setDown(sneakLatched)
        } else {
            if (sneakWasEnabled) sneakLatched = false
            if (!inputBlocked) mc.options.keyShift.setDown(sneakPhysical)
        }
        sprintPhysicalWasDown = sprintPhysical
        sneakPhysicalWasDown = sneakPhysical
        sprintWasEnabled = sprint
        sneakWasEnabled = sneak
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
        val down = isDown(key, window)
        val wasDown = previouslyDown.put(token, down) == true
        return !blocked && down && !wasDown
    }
    private fun isDown(key: Int, window: Long) = key in GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS
    private fun physicalKeyDown(saved: String, window: Long): Boolean {
        val key = runCatching { InputConstants.getKey(saved).value }.getOrDefault(-1)
        return isDown(key, window)
    }
}
