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

/** World data is sampled on the client tick; HUD composition only reads strings. */
object ModuleHuds {
    @Volatile var itemText = "Items: 0"
    @Volatile var targetText = ""
    @Volatile var inWorld = false
    fun register() {
        HudManager.register(ItemCounterHud(), "oneconfig.builtin")
        HudManager.register(WailaHud(), "oneconfig.builtin")
    }
    fun tick(mc: Minecraft) {
        val player = mc.player
        val level = mc.level
        inWorld = player != null && level != null
        if (player == null || level == null) { itemText = "Items: 0"; targetText = ""; return }
        val held = player.mainHandItem
        var total = 0
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (!stack.isEmpty && (held.isEmpty || ItemStack.isSameItem(stack, held))) total += stack.count
        }
        itemText = "${if (held.isEmpty) "Items" else held.hoverName.string}: $total"
        targetText = when (val target = mc.hitResult) {
            is BlockHitResult -> if (target.type == net.minecraft.world.phys.HitResult.Type.BLOCK)
                level.getBlockState(target.blockPos).block.name.string else ""
            is EntityHitResult -> target.entity.displayName.string
            else -> ""
        }
    }
}

class ItemCounterHud : TextHud("reconfig-item-counter", "Item Counter", Category.INFO, "") {
    override fun getText() = ModuleHuds.itemText
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun updateFrequency() = 50L
    override fun isAvailable() = ModuleHuds.inWorld && ModuleAccess.enabled("item_counter")
    override fun defaultPosition() = (HudManager.guiScreenWidth.coerceAtLeast(320f) / 2f - 40f) to (HudManager.guiScreenHeight.coerceAtLeast(240f) - 65f)
}

class WailaHud : TextHud("reconfig-waila", "WAILA", Category.INFO, "") {
    override fun getText() = ModuleHuds.targetText.ifEmpty { if (HudManager.isEditorOpen) "Grass Block" else "" }
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun updateFrequency() = 50L
    override fun isAvailable() = ModuleHuds.inWorld && ModuleAccess.enabled("waila") && ModuleHuds.targetText.isNotEmpty()
    override fun defaultPosition() = (HudManager.guiScreenWidth.coerceAtLeast(320f) / 2f - 40f) to 20f
}
