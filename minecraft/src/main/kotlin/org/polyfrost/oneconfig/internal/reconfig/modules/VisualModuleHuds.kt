/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.client.Minecraft
//? if >= 26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor as GuiContext
//?} else {
//import net.minecraft.client.gui.GuiGraphics as GuiContext
//?}
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
import org.polyfrost.compose.composables.absoluteAt
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.background
import org.polyfrost.compose.composables.border
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.internal.OneConfigConfig

/** Native item rendering remains inside HudManager's persisted editor transform. */
abstract class VisualModuleHud(private val moduleId: String, title: String, private val startX: Float,
    private val startY: Float) : LegacyHud("reconfig-$moduleId", title, Category.INFO) {
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun isAvailable() = ReConfigHudVisibility.gameplayOverlayVisible() && ModuleHuds.inWorld && ModuleAccess.enabled(moduleId)
    protected fun hiddenByReConfig() = !ReConfigHudVisibility.gameplayOverlayVisible()
    override fun defaultPosition() = startX to startY
    override fun update() = false
    protected fun label(g: GuiContext, text: String, x: Int, y: Int, color: Int = -1) {
        //? if >= 26.1 {
        g.text(Minecraft.getInstance().font, text, x, y, color, false)
        //? } else
        //g.drawString(Minecraft.getInstance().font, text, x, y, color, false)
    }
    protected fun item(g: GuiContext, stack: ItemStack, x: Int, y: Int) {
        if (stack.isEmpty) return
        //? if >= 26.1 {
        g.item(stack, x, y)
        //? } else
        //g.renderItem(stack, x, y)
        if (stack.isDamageableItem && stack.maxDamage > 0 && stack.damageValue > 0) {
            val fraction = ((stack.maxDamage - stack.damageValue).toFloat() / stack.maxDamage).coerceIn(0f, 1f)
            g.fill(x + 2, y + 13, x + 15, y + 15, 0xFF11151C.toInt())
            g.fill(x + 2, y + 13, x + 2 + (13 * fraction).toInt(), y + 14,
                if (fraction <= .2f) 0xFFFF646F.toInt() else 0xFF76E4AD.toInt())
        }
        if (stack.count > 1) {
            val count = stack.count.toString()
            label(g, count, x + 16 - Minecraft.getInstance().font.width(count), y + 8)
        }
    }
    protected fun showBackground() = showBackground && !OneConfigConfig.disableAllHudBackgrounds && ModuleAccess.choice(moduleId, "show_background", "true").toBoolean()
    protected fun panel(g: GuiContext) { if (showBackground()) roundedPanel(g, 0, 0, width.toInt(), height.toInt(), 7) }
    protected fun roundedPanel(g: GuiContext, left:Int, top:Int, right:Int, bottom:Int, radius:Int, fill:Int=0xD018202A.toInt()) {
        fun inset(row:Int,r:Int):Int { val dy=(r-row-.5).coerceAtLeast(0.0);return kotlin.math.ceil(r-kotlin.math.sqrt((r*r-dy*dy).coerceAtLeast(0.0))).toInt() }
        for(y in top until bottom){val edge=minOf(y-top,bottom-1-y);val cut=if(edge<radius)inset(edge,radius) else 0;g.fill(left+cut,y,right-cut,y+1,fill)}
        val outline=0x368DB9DC
        for(y in top until bottom){val edge=minOf(y-top,bottom-1-y);val cut=if(edge<radius)inset(edge,radius) else 0;g.fill(left+cut,y,left+cut+1,y+1,outline);g.fill(right-cut-1,y,right-cut,y+1,outline)}
        g.fill(left+radius,top,right-radius,top+1,outline);g.fill(left+radius,bottom-1,right-radius,bottom,outline)
    }
}

class InventoryGridHud : VisualModuleHud("inventory_hud", "Inventory", 190f, 40f) {
    private val columns get() = ModuleAccess.number("inventory_hud", "columns", 9f).toInt().coerceIn(3, 9)
    private val rows get() = ModuleAccess.number("inventory_hud", "rows", 4f).toInt().coerceIn(1, 4)
    override val width get() = (columns * 18 + 8).toFloat()
    override val height get() = (rows * 18 + 22).toFloat()
    override fun render(mcCtx: GuiContext) {
        if (hiddenByReConfig() || !HudManager.isEditing && !isAvailable()) return
        val inventory = Minecraft.getInstance().player?.inventory ?: return
        panel(mcCtx)
        val cols = columns
        val count = (rows * cols).coerceAtMost(36)
        label(mcCtx, "INVENTORY", 5, 5, 0xFFABBED3.toInt())
        // Backpack slots followed by hotbar; preserve native 16px aspect ratio.
        for (index in 0 until count) {
            val slot = if (index < 27) index + 9 else index - 27
            val x = 4 + (index % cols) * 18
            val y = 18 + (index / cols) * 18
            if (showBackground()) roundedPanel(mcCtx, x, y, x + 17, y + 17, 4, 0x553E4658)
            item(mcCtx, inventory.getItem(slot), x, y)
        }
    }
}

/** Effects use one card per tick-sampled effect, with a consistent drawn status icon. */
class EffectsHud : VisualModuleHud("effect_status", "Active Effects", 8f, 100f) {
    private data class EffectLine(val name: String, val duration: String, val beneficial: Boolean)
    private var effects: List<EffectLine> = emptyList()
    override fun updateFrequency() = 100L
    override fun update(): Boolean {
        effects = Minecraft.getInstance().player?.activeEffects?.map { effect ->
            val seconds = effect.duration.coerceAtLeast(0) / 20
            EffectLine("${effect.effect.value().displayName.string} ${effect.amplifier + 1}",
                if (effect.isInfiniteDuration) "∞" else "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60),
                effect.effect.value().isBeneficial)
        } ?: emptyList()
        return true
    }
    override val width get() = ((effects.maxOfOrNull { Minecraft.getInstance().font.width(it.name) } ?: 85) + 38).coerceAtLeast(124).toFloat()
    override val height get() = (effects.size.coerceAtLeast(1) * 29).toFloat()
    override fun render(mcCtx: GuiContext) {
        if (hiddenByReConfig() || !HudManager.isEditing && !isAvailable()) return
        if (effects.isEmpty()) { panel(mcCtx); label(mcCtx, "No active effects", 8, 10, 0xFFABBED3.toInt()); return }
        effects.forEachIndexed { index, effect ->
            val y = index * 29
            if (showBackground()) roundedPanel(mcCtx, 0, y, width.toInt(), y + 26, 7)
            val color = if (effect.beneficial) 0xFF76E4AD.toInt() else 0xFFFF818B.toInt()
            // Plus/minus icons distinguish status without depending on OS glyphs.
            mcCtx.fill(7, y + 12, 17, y + 14, color)
            if (effect.beneficial) mcCtx.fill(11, y + 8, 13, y + 18, color)
            label(mcCtx, effect.name, 24, y + 4)
            label(mcCtx, effect.duration, 24, y + 15, color)
        }
    }
}

class ArmorStatusHud : VisualModuleHud("armor_status", "Armor Status", 190f, 130f) {
    override val width = 110f
    override val height = 82f
    override fun render(mcCtx: GuiContext) {
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
    override fun isAvailable()=ReConfigHudVisibility.gameplayOverlayVisible()&&ModuleHuds.inWorld&&ModuleAccess.enabled("keystrokes")
    override fun update():Boolean {
        val mc=Minecraft.getInstance();val o=mc.options
        val next=Keys(o.keyUp.isDown,o.keyLeft.isDown,o.keyDown.isDown,o.keyRight.isDown,
            o.keyAttack.isDown,o.keyUse.isDown,o.keyJump.isDown)
        if(next==keys.value)return false
        keys.value=next;return true
    }
    @Composable override fun Content() {
        val state=keys.value
        val showBackground = showBackground && !OneConfigConfig.disableAllHudBackgrounds && ModuleAccess.choice("keystrokes", "show_background", "true").toBoolean()
        @Composable
        fun key(label:String,w:Float,down:Boolean){
            val fill=if(!showBackground) PolyColor(0x00000000) else if(down)PolyColor(0xCC466F9A.toInt()) else PolyColor(0xD018202A.toInt())
            val outline=if(showBackground) PolyColor(0x368DB9DC) else PolyColor(0x00000000)
            PolyBox(PolyModifier.size(w,23f).background(fill,6f).border(outline,.7f,6f)){
                PolyMcText(label,shadow=false,modifier=PolyModifier.align(PolyAlign.Center))
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

/** Local media artwork and timeline; transport controls live in the module settings. */
class BrowserMusicHud : Hud("reconfig-browser_music_hud", "Browser Music", Category.INFO) {
    private val snapshot = mutableStateOf<Pair<BrowserMusicBridge.Media?, Long>>(null to 0L)
    init { bgRadius=10f; bgColor=0x0018202A; padLeft=0f; padRight=0f; padTop=0f; padBottom=0f }
    override fun showByDefault() = true
    override fun multipleInstancesAllowed() = false
    override fun defaultPosition() = 8f to 405f
    override fun minimumSize() = 232f to 78f
    override fun updateFrequency() = 250L
    override fun isAvailable() = ReConfigHudVisibility.gameplayOverlayVisible() && ModuleHuds.inWorld && ModuleAccess.enabled("browser_music_hud")
    override fun update(): Boolean {
        val media = BrowserMusicBridge.media
        val next = media to (media?.currentSeconds() ?: 0L)
        if (next == snapshot.value) return false
        snapshot.value = next
        return true
    }
    private fun time(seconds: Long) = "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
    @Composable override fun Content() {
        val (media, position) = snapshot.value
        val artwork = androidx.compose.runtime.remember(media?.artworkPng) {
            media?.artworkPng?.let { runCatching { org.jetbrains.skia.Image.makeFromEncoded(it) }.getOrNull() }
        }
        androidx.compose.runtime.DisposableEffect(artwork) { onDispose { artwork?.close() } }
        val background = showBackground && !OneConfigConfig.disableAllHudBackgrounds && ModuleAccess.choice("browser_music_hud", "show_background", "true").toBoolean()
        val font = Minecraft.getInstance().font
        val accent = PolyColor(if (media != null) 0xFF00A6FF.toInt() else 0xFF788494.toInt())
        PolyBox(PolyModifier.size(232f,78f)) {
            org.polyfrost.compose.composables.PolyCanvas(PolyModifier.size(232f,78f)) { x,y,_,_ ->
                paint.isAntiAlias = true
                if (background) {
                    rect(x,y,232f,78f,PolyColor(0xD018202A.toInt()),10f)
                    rectStroke(x+.5f,y+.5f,231f,77f,PolyColor(0x368DB9DC),.7f,10f)
                }
                if (artwork != null) {
                    save()
                    try {
                        clipRRect(x+8,y+9,35f,35f,7f)
                        paint.color = -1
                        image(artwork,x+8,y+9,35f,35f,paint)
                    } finally { restore() }
                } else {
                    rect(x+27,y+18,3f,15f,accent,1f)
                    rect(x+21,y+31,7f,4f,accent,2f)
                    rect(x+28,y+18,7f,3f,accent,1f)
                }
                rect(x+10,y+55,212f,3f,PolyColor(0x554D6077),1.5f)
                if ((media?.duration ?: 0) > 0) rect(x+10,y+55,(212*MediaTimeline.progress(position,media!!.duration)).toFloat(),3f,accent,1.5f)
            }
            PolyMcText(shadow=false, text=font.plainSubstrByWidth(media?.title ?: "Nothing playing",170), modifier=PolyModifier.absoluteAt(50f,11f))
            PolyMcText(shadow=false, text=font.plainSubstrByWidth(media?.artist?.ifBlank { "Spotify" } ?: "Spotify · Windows media",170), color=PolyColor(0xFFADB9C9.toInt()),modifier=PolyModifier.absoluteAt(50f,24f))
            PolyMcText(shadow=false, text=if(media==null) "No supported session" else if(media.playing) "PLAYING · SPOTIFY" else "PAUSED · SPOTIFY",color=PolyColor(0xFF8193A8.toInt()),modifier=PolyModifier.absoluteAt(50f,37f))
            PolyMcText(shadow=false, text=if(media==null) "—:—" else time(position),color=PolyColor(0xFFADB9C9.toInt()),modifier=PolyModifier.absoluteAt(10f,64f))
            val end = if((media?.duration ?: 0)>0) time(media!!.duration) else "—:—"
            PolyMcText(end,shadow=false,color=PolyColor(0xFFADB9C9.toInt()),modifier=PolyModifier.absoluteAt(222f-font.width(end),64f))
        }
    }
}