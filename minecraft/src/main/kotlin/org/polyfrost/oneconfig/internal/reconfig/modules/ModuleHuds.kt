/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.TextHud
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.core.registries.BuiltInRegistries
import java.util.Locale

/** Tick-sampled world labels; visual HUDs read client-thread state during extraction. */
object ModuleHuds {
    @Volatile var itemText = "Items: 0"
    @Volatile var targetText = ""
    @Volatile var inWorld = false
    val counters = HudCounters()
    var coordinateText = ""
    var effectText = "No active effects"
    var serverText = "Offline"
    private var world: Any? = null
    private var registered = false
    fun now() = System.nanoTime() / 1_000_000L
    fun register() {
        if (registered) return
        registered = true
        HudManager.register(ItemCounterHud(), "oneconfig.builtin")
        HudManager.register(WailaHud(), "oneconfig.builtin")
        listOf(CpsHud(), FpsHud(), CoordinatesHud(), EffectsHud(), ComboHud(), MemoryHud(), ServerStatusHud(),
            KeystrokesHud(), ArmorStatusHud(), InventoryGridHud()).forEach { HudManager.register(it, "oneconfig.builtin") }
        EventManager.register(MouseInputEvent::class.java) { event ->
            val mc = Minecraft.getInstance()
            if (event.state == 1 && mc.player != null && mc.screen == null) counters.click(event.button, now())
        }
    }
    /** Invoked at packet-handler TAIL, after vanilla has marshalled to the client thread. */
    @JvmStatic fun damage(packet: ClientboundDamageEventPacket) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val victim = packet.entityId()
        // Incoming damage always ends a streak, even if its attacker is not a player.
        if (victim == player.id) {
            counters.damage(victim, packet.sourceCauseId(), player.id, now())
            return
        }
        // Do not count mobs, armor stands, missing entities, or unconfirmed swings.
        if (!ModuleAccess.enabled("combo_counter") || mc.level?.getEntity(victim) !is Player) return
        counters.damage(victim, packet.sourceCauseId(), player.id, now())
    }
    fun tick(mc: Minecraft) {
        val player = mc.player
        val level = mc.level
        if (world !== level) { counters.clear(); world = level }
        inWorld = player != null && level != null
        if (player == null || level == null) { itemText = "Items: 0"; targetText = ""; return }
        if (!player.isAlive) counters.clear()
        val biome = level.getBiome(player.blockPosition()).unwrapKey().map { it.identifier().path.replace('_', ' ') }.orElse("Unknown")
        coordinateText = String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f | %s | %s", player.x, player.y, player.z, player.direction.name, biome)
        effectText = player.activeEffects.joinToString("\n") { effect ->
            val seconds = (effect.duration / 20).coerceAtLeast(0)
            val timer = if (effect.isInfiniteDuration) "∞" else "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
            "${effect.effect.value().displayName.string} ${effect.amplifier + 1}  $timer"
        }.ifEmpty { "No active effects" }
        val ping = mc.connection?.getPlayerInfo(player.uuid)?.latency
        serverText = "Ping: ${ping?.let { "$it ms" } ?: "unavailable"} | TPS: unavailable | Loss: unavailable"
        val held = player.mainHandItem
        var total = 0
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (!stack.isEmpty && (held.isEmpty || ItemStack.isSameItem(stack, held))) total += stack.count
        }
        itemText = "${if (held.isEmpty) "Items" else held.hoverName.string}: $total"
        targetText = when (val target = mc.hitResult) {
            is BlockHitResult -> if (target.type == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                val block = level.getBlockState(target.blockPos).block
                "${block.name.string}\n${BuiltInRegistries.BLOCK.getKey(block)}"
            } else ""
            is EntityHitResult -> "${target.entity.displayName.string}\n${BuiltInRegistries.ENTITY_TYPE.getKey(target.entity.type)}"
            else -> ""
        }
    }
}

abstract class ModuleTextHud(private val moduleId: String, title: String, private val initialY: Float) :
    TextHud("reconfig-$moduleId", title, Category.INFO, "") {
    init { bgRadius = 8f; bgColor = 0xD018202A.toInt() }
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun updateFrequency() = 50L
    override fun isAvailable(): Boolean {
        showBackground = ModuleAccess.choice(moduleId, "show_background", "true").toBoolean()
        return ModuleHuds.inWorld && ModuleAccess.enabled(moduleId)
    }
    override fun defaultPosition() = 8f to initialY
}
class CpsHud : ModuleTextHud("cps", "CPS", 40f) {
    override fun getText() = "L ${ModuleHuds.counters.cps(0, ModuleHuds.now())} | R ${ModuleHuds.counters.cps(1, ModuleHuds.now())} CPS"
}
class FpsHud : ModuleTextHud("fps", "FPS", 60f) {
    override fun getText() = "${Minecraft.getInstance().fps} FPS"
}
class CoordinatesHud : ModuleTextHud("coordinates", "Coordinates", 80f) {
    override fun getText() = ModuleHuds.coordinateText
}
class ComboHud : ModuleTextHud("combo_counter", "Combo Counter", 145f) {
    override fun getText() = "Combo: ${ModuleHuds.counters.combo(ModuleHuds.now())}"
}
class MemoryHud : ModuleTextHud("memory_monitor", "Memory Monitor", 165f) {
    override fun getText(): String {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val warning = used.toDouble() / runtime.maxMemory().coerceAtLeast(1) >= 0.85
        return "${if (warning) "High heap usage! " else ""}RAM: ${used / 1048576} / ${runtime.totalMemory() / 1048576} MiB allocated (${runtime.maxMemory() / 1048576} MiB max)"
    }
}
class ServerStatusHud : ModuleTextHud("server_status", "Server Status", 185f) {
    override fun getText() = ModuleHuds.serverText
}

class ItemCounterHud : TextHud("reconfig-item-counter", "Item Counter", Category.INFO, "") {
    init { bgRadius = 8f; bgColor = 0xD018202A.toInt() }
    override fun getText() = ModuleHuds.itemText
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun updateFrequency() = 50L
    override fun isAvailable(): Boolean {
        showBackground = ModuleAccess.choice("item_counter", "show_background", "true").toBoolean()
        return ModuleHuds.inWorld && ModuleAccess.enabled("item_counter")
    }
    override fun defaultPosition() = (HudManager.guiScreenWidth.coerceAtLeast(320f) / 2f - 40f) to (HudManager.guiScreenHeight.coerceAtLeast(240f) - 65f)
}

class WailaHud : TextHud("reconfig-waila", "WAILA", Category.INFO, "") {
    init { bgRadius = 8f; bgColor = 0xD018202A.toInt() }
    override fun getText() = ModuleHuds.targetText.ifEmpty { if (HudManager.isEditorOpen) "Grass Block" else "" }
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun updateFrequency() = 50L
    override fun isAvailable(): Boolean {
        showBackground = ModuleAccess.choice("waila", "show_background", "true").toBoolean()
        return ModuleHuds.inWorld && ModuleAccess.enabled("waila") && ModuleHuds.targetText.isNotEmpty()
    }
    override fun defaultPosition() = (HudManager.guiScreenWidth.coerceAtLeast(320f) / 2f - 40f) to 20f
}
