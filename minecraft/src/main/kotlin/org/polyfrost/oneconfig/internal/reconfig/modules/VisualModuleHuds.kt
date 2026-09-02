/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyColumn
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.PolyRow
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.background
import org.polyfrost.compose.composables.border
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.render.PolyColor

/** Native item rendering remains inside HudManager's persisted editor transform. */
abstract class VisualModuleHud(private val moduleId: String, title: String, private val startX: Float,
    private val startY: Float) : LegacyHud("reconfig-$moduleId", title, Category.INFO) {
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun isAvailable() = ModuleHuds.inWorld && ModuleAccess.enabled(moduleId)
    protected fun hiddenByReConfig() = ShellState.uiOpen && !HudManager.isEditing
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
    protected fun showBackground() = ModuleAccess.choice(moduleId, "show_background", "true").toBoolean()
    protected fun panel(g: GuiGraphicsExtractor) { if (showBackground()) roundedPanel(g, 0, 0, width.toInt(), height.toInt(), 7) }
    protected fun roundedPanel(g: GuiGraphicsExtractor, left:Int, top:Int, right:Int, bottom:Int, radius:Int, fill:Int=0xD018202A.toInt()) {
        fun inset(row:Int,r:Int):Int { val dy=(r-row-.5).coerceAtLeast(0.0);return kotlin.math.ceil(r-kotlin.math.sqrt((r*r-dy*dy).coerceAtLeast(0.0))).toInt() }
        for(y in top until bottom){val edge=minOf(y-top,bottom-1-y);val cut=if(edge<radius)inset(edge,radius) else 0;g.fill(left+cut,y,right-cut,y+1,fill)}
        val outline=0x368DB9DC
        for(y in top until bottom){val edge=minOf(y-top,bottom-1-y);val cut=if(edge<radius)inset(edge,radius) else 0;g.fill(left+cut,y,left+cut+1,y+1,outline);g.fill(right-cut-1,y,right-cut,y+1,outline)}
        g.fill(left+radius,top,right-radius,top+1,outline);g.fill(left+radius,bottom-1,right-radius,bottom,outline)
    }
}

class InventoryGridHud : VisualModuleHud("inventory_hud", "Inventory", 190f, 40f) {
    override val width = 166f
    override val height = 79f
    override fun render(mcCtx: GuiGraphicsExtractor) {
        if (hiddenByReConfig() || !HudManager.isEditing && !isAvailable()) return
        val inventory = Minecraft.getInstance().player?.inventory ?: return
        panel(mcCtx)
        // Three backpack rows, then the hotbar separated by three pixels.
        for (index in 0 until 36) {
            val slot = if (index < 27) index + 9 else index - 27
            val x = 2 + (index % 9) * 18
            val y = 2 + (index / 9) * 18 + if (index >= 27) 3 else 0
            roundedPanel(mcCtx,x,y,x+17,y+17,4,0x553E4658)
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
        if (hiddenByReConfig() || !HudManager.isEditing && !isAvailable()) return
        panel(mcCtx)
        lines().forEachIndexed { index, line -> label(mcCtx, line, 4, 4 + index * 12) }
    }
}

class ArmorStatusHud : VisualModuleHud("armor_status", "Armor Status", 190f, 130f) {
    override val width = 110f
    override val height = 82f
    override fun render(mcCtx: GuiGraphicsExtractor) {
        if (hiddenByReConfig() || !HudManager.isEditing && !isAvailable()) return
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

class KeystrokesHud : Hud("reconfig-keystrokes", "Keystrokes", Category.INFO) {
    private data class Keys(val w:Boolean=false,val a:Boolean=false,val s:Boolean=false,val d:Boolean=false,
        val lmb:Boolean=false,val rmb:Boolean=false,val space:Boolean=false)
    private val keys = mutableStateOf(Keys())
    init { bgRadius=8f;bgColor=0x0018202A;padLeft=0f;padRight=0f;padTop=0f;padBottom=0f }
    override fun showByDefault()=true
    override fun multipleInstancesAllowed()=false
    override fun defaultPosition()=370f to 40f
    override fun minimumSize()=76f to 100f
    override fun updateFrequency()=16L
    override fun isAvailable()=ModuleHuds.inWorld&&ModuleAccess.enabled("keystrokes")
    override fun update():Boolean {
        val mc=Minecraft.getInstance();val o=mc.options
        val next=Keys(o.keyUp.isDown,o.keyLeft.isDown,o.keyDown.isDown,o.keyRight.isDown,
            o.keyAttack.isDown,o.keyUse.isDown,o.keyJump.isDown)
        if(next==keys.value)return false
        keys.value=next;return true
    }
    @Composable override fun Content() {
        val state=keys.value
        val showBackground = ModuleAccess.choice("keystrokes", "show_background", "true").toBoolean()
        @Composable
        fun key(label:String,w:Float,down:Boolean){
            val fill=if(!showBackground) PolyColor(0x00000000) else if(down)PolyColor(0xCC466F9A.toInt()) else PolyColor(0xD018202A.toInt())
            val outline=if(showBackground) PolyColor(0x368DB9DC) else PolyColor(0x00000000)
            PolyBox(PolyModifier.size(w,23f).background(fill,6f).border(outline,.7f,6f)){
                PolyMcText(label,modifier=PolyModifier.align(PolyAlign.Center))
            }
        }
        PolyColumn(gap=2f){
            PolyRow{PolyBox(PolyModifier.size(26f,23f));key("W",24f,state.w)}
            PolyRow(gap=2f){key("A",24f,state.a);key("S",24f,state.s);key("D",24f,state.d)}
            PolyRow(gap=2f){key("LMB",37f,state.lmb);key("RMB",37f,state.rmb)}
            key("SPACE",76f,state.space)
        }
    }
}
