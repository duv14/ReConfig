/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component as McComponent
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.ShulkerBoxBlock
import net.kyori.adventure.text.Component
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent
import org.polyfrost.oneconfig.api.event.v1.events.PacketEvent
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess
import org.polyfrost.oneconfig.api.platform.v1.Platform
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID

object QualityOfLifeModules {
    private var registered = false
    private var pendingGgAt = 0L
    private var lastGgAt = 0L

    fun register() {
        if (registered) return
        registered = true
        EventManager.register(ChatEvent.Receive::class.java) { event -> onChat(event) }
        EventManager.register(PacketEvent.Receive::class.java) { event ->
            val packet = event.getPacket<Any>()
            if (packet is ClientboundEntityEventPacket && packet.eventId.toInt() == 35) {
                Minecraft.getInstance().execute { TotemPopTracker.onPopPacket(packet) }
            }
        }
        ItemTooltipCallback.EVENT.register tooltip@{ stack, _, _, lines ->
            if (!ModuleAccess.enabled("shulker_tooltip")) return@tooltip
            val item = stack.item
            if (item !is BlockItem || item.block !is ShulkerBoxBlock) return@tooltip
            val container = stack.get(DataComponents.CONTAINER) ?: return@tooltip
            val items = container.nonEmptyItems().toList()
            if (items.isEmpty()) {
                lines.add(McComponent.literal("Empty shulker").withStyle(ChatFormatting.DARK_GRAY))
                return@tooltip
            }
            val maxLines = ModuleAccess.number("shulker_tooltip", "max_lines", 6f).toInt().coerceIn(1, 12)
            lines.add(McComponent.literal("Contents (${items.size} stacks):").withStyle(ChatFormatting.GRAY))
            items.take(maxLines).forEach { child ->
                lines.add(McComponent.literal("  ${child.count}× ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(ItemStackNameCompat.component(child).copy().withStyle(ChatFormatting.GRAY)))
            }
            if (items.size > maxLines) {
                lines.add(McComponent.literal("  +${items.size - maxLines} more").withStyle(ChatFormatting.DARK_GRAY))
            }
        }
    }

    fun tick(mc: Minecraft, nowMs: Long) {
        BrowserMusicBridge.tick(nowMs)
        if (pendingGgAt != 0L && nowMs >= pendingGgAt) {
            pendingGgAt = 0L
            val moduleEnabled = ModuleAccess.enabled("auto_gg")
            val message = ModuleAccess.text("auto_gg", "message", "gg").trim().take(256)
            if (moduleEnabled && message.isNotBlank() && mc.player != null && Platform.screen().current<Any?>() == null) {
                mc.player!!.connection.sendChat(message)
                lastGgAt = nowMs
            }
        }
    }

    private fun onChat(event: ChatEvent.Receive) {
        val raw = event.fullyUnformattedMessage
        val now = System.currentTimeMillis()
        if (ModuleAccess.enabled("auto_gg")) {
            val cooldown = (ModuleAccess.number("auto_gg", "cooldown_s", 10f) * 1000L).toLong()
            val triggers = ModuleAccess.text("auto_gg", "triggers", "victory,winner,game over,you won,match over")
                .split(',').map { it.trim().lowercase(Locale.ROOT) }.filter { it.length >= 3 }
            val lower = raw.lowercase(Locale.ROOT)
            if (now - lastGgAt >= cooldown && pendingGgAt == 0L && triggers.any(lower::contains)) {
                pendingGgAt = now + ModuleAccess.number("auto_gg", "delay_ms", 650f).toLong().coerceIn(0L, 5000L)
            }
        }
        if (ModuleAccess.enabled("chat_timestamps")) {
            val pattern = ModuleAccess.choice("chat_timestamps", "format", "HH:mm")
            val formatter = runCatching { DateTimeFormatter.ofPattern(pattern, Locale.ROOT) }.getOrElse { DateTimeFormatter.ofPattern("HH:mm") }
            val stamp = LocalTime.now().format(formatter)
            event.message = Component.text("[$stamp] ").append(event.message)
        }
    }
}

object TotemPopTracker {
    private val counts = LinkedHashMap<UUID, Int>()
    @Volatile var displayText: String = "No recent totem pops"
        private set
    private var world: Any? = null

    fun resetForWorld(level: Any?) {
        if (world === level) return
        world = level
        counts.clear()
        displayText = "No recent totem pops"
    }

    fun onPopPacket(packet: ClientboundEntityEventPacket) {
        if (!ModuleAccess.enabled("totem_pop_customizer")) return
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val entity = packet.getEntity(level) ?: return
        val player = entity as? net.minecraft.world.entity.player.Player ?: return
        val isSelf = player.uuid == mc.player?.uuid
        if (isSelf && !ModuleAccess.choice("totem_pop_customizer", "show_self", "true").toBoolean()) return
        if (!isSelf && !ModuleAccess.choice("totem_pop_customizer", "show_others", "true").toBoolean()) return
        counts[player.uuid] = (counts[player.uuid] ?: 0) + 1
        val recent = counts.entries.toList().takeLast(4).reversed().mapNotNull { (uuid, count) ->
            level.getPlayerByUUID(uuid)?.let { "${it.displayName.string}: $count pop${if (count == 1) "" else "s"}" }
        }
        displayText = recent.joinToString("\n").ifEmpty { "No recent totem pops" }
    }
}
