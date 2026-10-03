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
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.core.registries.BuiltInRegistries
import java.util.Locale

object ReConfigHudVisibility {
    fun gameplayOverlayVisible() = !ShellState.uiOpen || HudManager.isEditorOpen
}

/** Tick-sampled world labels; visual HUDs read client-thread state during extraction. */
object ModuleHuds {
    @Volatile var itemText = "Items: 0"
    @Volatile var targetText = ""
    @Volatile var inWorld = false
    val counters = HudCounters()
    var coordinateText = ""
    var effectText = "No active effects"
    var serverText = "Offline"
    var reachText = "Reach: —"
    var targetHudText = "No target"
    var speedText = "Speed: 0.00 blocks/s"
    var lightText = "Light: —"
    var clockText = "Day 1 • 06:00"
    var durabilityText = "Durability OK"
    var durabilityWarning = false
    var lowHealthText = "Health OK"
    var lowHealthWarning = false
    var projectileText = "Arrows 0 • Pearls 0 • Wind 0"
    var sessionText = "Session 00:00 • Hits 0 • Deaths 0"
    private var sessionStartedAt = now()
    private var sessionDeaths = 0
    private var wasAlive = true
    private var world: Any? = null
    private var registered = false
    fun now() = System.nanoTime() / 1_000_000L
    fun register() {
        if (registered) return
        registered = true
        HudManager.register(ItemCounterHud(), "oneconfig.builtin")
        HudManager.register(WailaHud(), "oneconfig.builtin")
        listOf(CpsHud(), FpsHud(), CoordinatesHud(), EffectsHud(), ComboHud(), MemoryHud(), ServerStatusHud(),
            ReachIndicatorHud(), TargetHud(), SpeedometerHud(), LightLevelHud(), ClockHud(), DurabilityAlertHud(),
            LowHealthHud(), ProjectileCounterHud(), SessionStatsHud(), TotemPopHud(), BrowserMusicHud(),
            KeystrokesHud(), ArmorStatusHud(), InventoryGridHud()).forEach { HudManager.register(it, "oneconfig.builtin") }
        EventManager.register(MouseInputEvent::class.java) { event ->
            val mc = Minecraft.getInstance()
            if (event.state == 1 && mc.player != null && Platform.screen().current<Any?>() == null) counters.click(event.button, now())
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
        if (world !== level) {
            counters.clear(); world = level; sessionStartedAt = now(); sessionDeaths = 0; wasAlive = player?.isAlive ?: true
        }
        inWorld = player != null && level != null
        if (player == null || level == null) { itemText = "Items: 0"; targetText = ""; return }
        if (wasAlive && !player.isAlive) sessionDeaths++
        wasAlive = player.isAlive
        if (!player.isAlive) counters.combo(now())
        val biome = level.getBiome(player.blockPosition()).unwrapKey().map { it.identifier().path.replace('_', ' ') }.orElse("Unknown")
        coordinateText = String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f | %s | %s", player.x, player.y, player.z, player.direction.name, biome)
        effectText = player.activeEffects.joinToString("\n") { effect ->
            val seconds = (effect.duration / 20).coerceAtLeast(0)
            val timer = if (effect.isInfiniteDuration) "∞" else "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
            "${effect.effect.value().displayName.string} ${effect.amplifier + 1}  $timer"
        }.ifEmpty { "No active effects" }
        val ping = mc.connection?.getPlayerInfo(player.uuid)?.latency
        serverText = "Ping: ${ping?.let { "$it ms" } ?: "unavailable"} | TPS: unavailable | Loss: unavailable"
        val horizontalBps = player.deltaMovement.horizontalDistance() * 20.0
        speedText = if (ModuleAccess.choice("speedometer", "units", "blocks/s") == "km/h")
            "Speed: %.2f km/h".format(Locale.ROOT, horizontalBps * 3.6) else "Speed: %.2f blocks/s".format(Locale.ROOT, horizontalBps)
        lightText = "Light: ${level.getMaxLocalRawBrightness(player.blockPosition())}"
        val worldTime = level.gameTime
        val worldTicks = Math.floorMod(worldTime, 24000L)
        val totalMinutes = ((worldTicks + 6000L) % 24000L) * 1440L / 24000L
        val hour24 = (totalMinutes / 60L).toInt(); val minute = (totalMinutes % 60L).toInt(); val day = worldTime / 24000L + 1L
        clockText = if (ModuleAccess.choice("clock_hud", "format", "24h") == "12h") {
            val h = ((hour24 + 11) % 12) + 1; val suffix = if (hour24 < 12) "AM" else "PM"
            "Day $day • %d:%02d %s".format(Locale.ROOT, h, minute, suffix)
        } else "Day $day • %02d:%02d".format(Locale.ROOT, hour24, minute)
        val held = player.mainHandItem
        var total = 0
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (!stack.isEmpty && (held.isEmpty || ItemStack.isSameItem(stack, held))) total += stack.count
        }
        itemText = "${if (held.isEmpty) "Items" else ItemStackNameCompat.text(held)}: $total"

        val durabilityThreshold = ModuleAccess.number("durability_alerts", "threshold", 15f).coerceIn(1f, 90f)
        val durabilityCandidates = buildList {
            for (slot in 0 until inventory.containerSize) add(inventory.getItem(slot))
            add(player.getItemBySlot(EquipmentSlot.HEAD)); add(player.getItemBySlot(EquipmentSlot.CHEST))
            add(player.getItemBySlot(EquipmentSlot.LEGS)); add(player.getItemBySlot(EquipmentSlot.FEET)); add(player.mainHandItem); add(player.offhandItem)
        }.filter { !it.isEmpty && it.isDamageableItem && it.maxDamage > 0 }
        val weakest = durabilityCandidates.minByOrNull { (it.maxDamage - it.damageValue).toFloat() / it.maxDamage.toFloat() }
        val weakestPct = weakest?.let { ((it.maxDamage - it.damageValue).toFloat() / it.maxDamage.toFloat() * 100f).coerceIn(0f, 100f) } ?: 100f
        durabilityWarning = weakest != null && weakestPct <= durabilityThreshold
        durabilityText = if (weakest == null) "Durability: no damageable gear" else "${if (durabilityWarning) "⚠ " else ""}${ItemStackNameCompat.text(weakest)}: ${weakestPct.toInt()}%"

        val healthPct = if (player.maxHealth > 0f) player.health / player.maxHealth * 100f else 100f
        lowHealthWarning = healthPct <= ModuleAccess.number("low_health_warning", "threshold", 30f).coerceIn(1f, 90f)
        lowHealthText = "${if (lowHealthWarning) "⚠ LOW HEALTH • " else "Health: "}${player.health.coerceAtLeast(0f).let { "%.1f".format(Locale.ROOT, it) }}/${"%.1f".format(Locale.ROOT, player.maxHealth)}"

        var arrows = 0; var pearls = 0; var wind = 0; var snowballs = 0
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot); if (stack.isEmpty) continue
            when (BuiltInRegistries.ITEM.getKey(stack.item).path) {
                "arrow", "spectral_arrow", "tipped_arrow" -> arrows += stack.count
                "ender_pearl" -> pearls += stack.count
                "wind_charge" -> wind += stack.count
                "snowball", "egg" -> snowballs += stack.count
            }
        }
        projectileText = "Arrows $arrows • Pearls $pearls • Wind $wind • Throwables $snowballs"
        val elapsedSec = ((now() - sessionStartedAt).coerceAtLeast(0L) / 1000L)
        sessionText = "Session %02d:%02d • Hits %d • Deaths %d".format(Locale.ROOT, elapsedSec / 60, elapsedSec % 60, counters.totalHits(), sessionDeaths)

        val currentTarget = mc.hitResult
        reachText = if (currentTarget is EntityHitResult) {
            val decimals = ModuleAccess.number("reach_indicator", "decimals", 2f).toInt().coerceIn(0, 3)
            "Reach: %.${decimals}f m".format(Locale.ROOT, player.eyePosition.distanceTo(currentTarget.location))
        } else "Reach: —"
        targetHudText = if (currentTarget is EntityHitResult) {
            val entity = currentTarget.entity
            val distance = player.eyePosition.distanceTo(currentTarget.location)
            if (entity is LivingEntity) "${entity.displayName.string} • %.1f/%.1f HP • %.2fm".format(Locale.ROOT, entity.health, entity.maxHealth, distance)
            else "${entity.displayName.string} • %.2fm".format(Locale.ROOT, distance)
        } else "No target"
        targetText = when (val target = currentTarget) {
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
        return ReConfigHudVisibility.gameplayOverlayVisible() && ModuleHuds.inWorld && ModuleAccess.enabled(moduleId)
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
class ReachIndicatorHud : ModuleTextHud("reach_indicator", "Reach Indicator", 205f) {
    override fun getText() = ModuleHuds.reachText
    override fun isAvailable() = super.isAvailable() && (HudManager.isEditing || Minecraft.getInstance().hitResult is EntityHitResult)
}

class TargetHud : ModuleTextHud("target_hud", "Target HUD", 225f) {
    override fun getText() = ModuleHuds.targetHudText
    override fun isAvailable() = super.isAvailable() && (HudManager.isEditing || Minecraft.getInstance().hitResult is EntityHitResult)
}
class SpeedometerHud : ModuleTextHud("speedometer", "Speedometer", 245f) { override fun getText() = ModuleHuds.speedText }
class LightLevelHud : ModuleTextHud("light_level", "Light Level", 265f) { override fun getText() = ModuleHuds.lightText }
class ClockHud : ModuleTextHud("clock_hud", "Clock", 285f) { override fun getText() = ModuleHuds.clockText }
class DurabilityAlertHud : ModuleTextHud("durability_alerts", "Durability Alerts", 305f) {
    override fun getText() = ModuleHuds.durabilityText
    override fun isAvailable() = super.isAvailable() && (HudManager.isEditing || ModuleHuds.durabilityWarning)
}
class LowHealthHud : ModuleTextHud("low_health_warning", "Low Health Warning", 325f) {
    override fun getText() = if (HudManager.isEditing && !ModuleHuds.lowHealthWarning) "⚠ LOW HEALTH • 5.0/20.0" else ModuleHuds.lowHealthText
    override fun isAvailable() = super.isAvailable() && (HudManager.isEditing || ModuleHuds.lowHealthWarning)
}
class ProjectileCounterHud : ModuleTextHud("projectile_counter", "Projectile Counter", 345f) { override fun getText() = ModuleHuds.projectileText }
class SessionStatsHud : ModuleTextHud("session_stats", "Session Statistics", 365f) { override fun getText() = ModuleHuds.sessionText }
class TotemPopHud : ModuleTextHud("totem_pop_customizer", "Totem Pops", 385f) { override fun getText() = TotemPopTracker.displayText }
class BrowserMusicHud : ModuleTextHud("browser_music_hud", "Browser Music", 405f) { override fun getText() = "♫ ${BrowserMusicBridge.title}" }

class ItemCounterHud : TextHud("reconfig-item-counter", "Item Counter", Category.INFO, "") {
    init { bgRadius = 8f; bgColor = 0xD018202A.toInt() }
    override fun getText() = ModuleHuds.itemText
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun updateFrequency() = 50L
    override fun isAvailable(): Boolean {
        showBackground = ModuleAccess.choice("item_counter", "show_background", "true").toBoolean()
        return ReConfigHudVisibility.gameplayOverlayVisible() && ModuleHuds.inWorld && ModuleAccess.enabled("item_counter")
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
        return ReConfigHudVisibility.gameplayOverlayVisible() && ModuleHuds.inWorld && ModuleAccess.enabled("waila") && (HudManager.isEditing || ModuleHuds.targetText.isNotEmpty())
    }
    override fun defaultPosition() = (HudManager.guiScreenWidth.coerceAtLeast(320f) / 2f - 40f) to 20f
}
