/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Cross-version display-name access for both ItemStack and newer container
 * template entries (for example ItemStackTemplate on 26.2).
 */
internal object ItemStackNameCompat {
    private val candidateMethods = arrayOf("getHoverName", "getName", "getDisplayName", "getItemName")
    private val itemMethods = arrayOf("getItem", "item")

    fun component(stackLike: Any): Component {
        for (name in candidateMethods) {
            val value = runCatching {
                stackLike.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }?.invoke(stackLike)
            }.getOrNull()
            if (value is Component) return value
        }

        val item: Item? = when (stackLike) {
            is ItemStack -> stackLike.item
            else -> itemMethods.asSequence().mapNotNull { name ->
                runCatching {
                    stackLike.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }?.invoke(stackLike)
                }.getOrNull() as? Item
            }.firstOrNull()
        }

        val id = item?.let { runCatching { BuiltInRegistries.ITEM.getKey(it).toString() }.getOrNull() }
        return Component.literal((id ?: "item").substringAfter(':').replace('_', ' '))
    }

    fun text(stackLike: Any): String = component(stackLike).string
}
