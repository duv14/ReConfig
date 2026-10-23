/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
//? if >= 26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor as Graphics
//? } else
//import net.minecraft.client.gui.GuiGraphics as Graphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.ShulkerBoxBlock
import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess

/** Snapshot only: rendering icons never recursively opens a nested container tooltip. */
class ShulkerPreview private constructor(private val stacks: NonNullList<ItemStack>, private val scale: Float,
    private val background: Boolean) : TooltipComponent, ClientTooltipComponent {
    override fun getWidth(font: Font) = kotlin.math.ceil(166 * scale).toInt()
    override fun getHeight(font: Font) = kotlin.math.ceil(60 * scale).toInt()
    //? if >= 26.1 {
    override fun extractImage(font: Font, x: Int, y: Int, width: Int, height: Int, graphics: Graphics) {
    //? } else
    //override fun renderImage(font: Font, x: Int, y: Int, width: Int, height: Int, graphics: Graphics) {
        graphics.pose().pushMatrix()
        try {
            graphics.pose().translate(x.toFloat(), y.toFloat()).scale(scale, scale)
            if (background) {
                graphics.fill(0, 0, 166, 58, 0xEF171E2A.toInt())
                graphics.fill(1, 0, 165, 1, 0x555F7893)
                graphics.fill(1, 57, 165, 58, 0x99070B12.toInt())
            }
            for (slot in 0 until 27) {
                val sx = 3 + slot % 9 * 18
                val sy = 2 + slot / 9 * 18
                if (background) graphics.fill(sx, sy, sx + 17, sy + 17, 0x553E4658)
                val stack = stacks[slot]
                if (stack.isEmpty) continue
                //? if >= 26.1 {
                graphics.item(stack, sx, sy)
                graphics.itemDecorations(font, stack, sx, sy)
                //? } else {
                /*graphics.renderItem(stack, sx, sy)
                graphics.renderItemDecorations(font, stack, sx, sy)
                *///? }
            }
        } finally { graphics.pose().popMatrix() }
    }
    companion object {
        @JvmStatic fun create(stack: ItemStack): ShulkerPreview? {
            if (!ModuleAccess.enabled("shulker_tooltip")) return null
            val item = stack.item as? BlockItem ?: return null
            if (item.block !is ShulkerBoxBlock) return null
            val loader = FabricLoader.getInstance()
            if (!ModuleAccess.choice("shulker_tooltip", "override_external", "false").toBoolean() &&
                (loader.isModLoaded("shulkerboxtooltip") || loader.isModLoaded("peek"))) return null
            val display = stack.get(DataComponents.TOOLTIP_DISPLAY)
            if (display != null && (display.hideTooltip() || !display.shows(DataComponents.CONTAINER))) return null
            if (ModuleAccess.choice("shulker_tooltip", "require_shift", "false").toBoolean()) {
                val window = Platform.compatibility().windowHandle()
                if (window == 0L || (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) != GLFW.GLFW_PRESS &&
                    GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) != GLFW.GLFW_PRESS)) return null
            }
            val items = NonNullList.withSize(27, ItemStack.EMPTY)
            return runCatching {
                stack.get(DataComponents.CONTAINER)?.copyInto(items)
                val requested = ModuleAccess.number("shulker_tooltip", "preview_scale", 1f)
                val available = (Minecraft.getInstance().window.guiScaledWidth - 24).coerceAtLeast(40) / 166f
                val scale = (if (requested.isFinite()) requested else 1f).coerceIn(.5f, 2f).coerceAtMost(available)
                ShulkerPreview(items, scale, ModuleAccess.choice("shulker_tooltip", "show_background", "true").toBoolean())
            }.getOrNull()
        }
    }
}
