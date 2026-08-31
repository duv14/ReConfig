/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess

/** Native item rendering remains inside HudManager's persisted editor transform. */
abstract class VisualModuleHud(private val moduleId: String, title: String, private val startX: Float,
    private val startY: Float) : LegacyHud("reconfig-$moduleId", title, Category.INFO) {
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun isAvailable() = ModuleHuds.inWorld && ModuleAccess.enabled(moduleId)
    override fun defaultPosition() = startX to startY
    override fun update() = false
    protected fun label(g: GuiGraphicsExtractor, text: String, x: Int, y: Int, color: Int = -1) {
        //? if >= 26.1 {
        g.text(Minecraft.getInstance().font, text, x, y, color, true)
        //? } else
        //g.drawString(Minecraft.getInstance().font, text, x, y, color, true)
    }
    protected fun item(g: GuiGraphicsExtractor, stack: ItemStack, x: Int, y: Int) {
        if (stack.isEmpty) return
        //? if >= 26.1 {
        g.item(stack, x, y)
        //? } else
        //g.renderItem(stack, x, y)
        // Native stack icon plus count; armor uses an explicit durability bar below.
        if (stack.count > 1) {
            val count = stack.count.toString()
            label(g, count, x + 16 - Minecraft.getInstance().font.width(count), y + 8)
        }
    }
    protected fun panel(g: GuiGraphicsExtractor) = g.fill(0, 0, width.toInt(), height.toInt(), 0xA0181B24.toInt())
}

class InventoryGridHud : VisualModuleHud("inventory_hud", "Inventory", 190f, 40f) {
    override val width = 166f
    override val height = 79f
    override fun render(mcCtx: GuiGraphicsExtractor) {
        if (!HudManager.isEditing && !isAvailable()) return
        val inventory = Minecraft.getInstance().player?.inventory ?: return
        panel(mcCtx)
        // Three backpack rows, then the hotbar separated by three pixels.
        for (index in 0 until 36) {
            val slot = if (index < 27) index + 9 else index - 27
            val x = 2 + (index % 9) * 18
            val y = 2 + (index / 9) * 18 + if (index >= 27) 3 else 0
            mcCtx.fill(x, y, x + 17, y + 17, 0x553E4658)
            item(mcCtx, inventory.getItem(slot), x, y)
        }
    }
}

/** Explicit line layout: TextHud's Poppins renderer treats text as one glyph run. */
class EffectsHud : VisualModuleHud("effect_status", "Active Effects", 8f, 100f) {
    private fun lines() = ModuleHuds.effectText.lines().ifEmpty { listOf("No active effects") }
    override val width: Float
        get() = ((lines().maxOfOrNull { Minecraft.getInstance().font.width(it) } ?: 0) + 8).coerceAtLeast(108).toFloat()
    override val height: Float
        get() = (lines().size.coerceAtLeast(1) * 12 + 8).toFloat()
    override fun render(mcCtx: GuiGraphicsExtractor) {
        if (!HudManager.isEditing && !isAvailable()) return
        panel(mcCtx)
        lines().forEachIndexed { index, line -> label(mcCtx, line, 4, 4 + index * 12) }
    }
}

class ArmorStatusHud : VisualModuleHud("armor_status", "Armor Status", 190f, 130f) {
    override val width = 110f
    override val height = 82f
    override fun render(mcCtx: GuiGraphicsExtractor) {
        if (!HudManager.isEditing && !isAvailable()) return
        val player = Minecraft.getInstance().player ?: return
        panel(mcCtx)
        listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).forEachIndexed { index, slot ->
            val stack = player.getItemBySlot(slot)
            val y = 2 + index * 20
            item(mcCtx, stack, 2, y)
            if (stack.isEmpty) label(mcCtx, "—", 22, y + 3, 0xFF999999.toInt())
            else if (stack.isDamageableItem) {
                val remaining = (stack.maxDamage - stack.damageValue).coerceAtLeast(0)
                val fraction = remaining.toFloat() / stack.maxDamage.coerceAtLeast(1)
                val color = if (fraction < .2f) 0xFFFF5F65.toInt() else 0xFF76E4AD.toInt()
                label(mcCtx, "$remaining/${stack.maxDamage}", 22, y + 1, color)
                mcCtx.fill(22, y + 13, 104, y + 15, 0xFF343947.toInt())
                mcCtx.fill(22, y + 13, 22 + (82 * fraction).toInt(), y + 15, color)
            } else label(mcCtx, "Unbreakable", 22, y + 3)
        }
    }
}

class KeystrokesHud : VisualModuleHud("keystrokes", "Keystrokes", 370f, 40f) {
    override val width = 76f
    override val height = 100f
    override fun render(mcCtx: GuiGraphicsExtractor) {
        if (!HudManager.isEditing && !isAvailable()) return
        val mc = Minecraft.getInstance()
        val options = mc.options
        fun key(name: String, x: Int, y: Int, w: Int, down: Boolean) {
            mcCtx.fill(x, y, x + w, y + 23, if (down && mc.screen == null) 0xDD617DF1.toInt() else 0xA0181B24.toInt())
            label(mcCtx, name, x + (w - mc.font.width(name)) / 2, y + 7)
        }
        key("W", 26, 0, 24, options.keyUp.isDown)
        key("A", 0, 25, 24, options.keyLeft.isDown)
        key("S", 26, 25, 24, options.keyDown.isDown)
        key("D", 52, 25, 24, options.keyRight.isDown)
        key("LMB", 0, 50, 37, options.keyAttack.isDown)
        key("RMB", 39, 50, 37, options.keyUse.isDown)
        key("SPACE", 0, 75, 76, options.keyJump.isDown)
    }
}
