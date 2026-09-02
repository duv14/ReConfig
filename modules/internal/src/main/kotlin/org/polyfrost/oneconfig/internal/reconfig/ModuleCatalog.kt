/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.prefs.Preferences

enum class SettingKind { TOGGLE, NUMBER, KEY, TEXT, COLOR, CHOICE, INFO }
data class ModuleSetting(val id:String,val title:String,val kind:SettingKind,val min:Float=0f,val max:Float=1f,val choices:List<String> = emptyList(),val default:String)
class ClientModule(val id:String,val name:String,val description:String,val icon:String,val settings:List<ModuleSetting>) {
    private val prefs=Preferences.userRoot().node("dev/duv14/reconfig/modules/$id")
    var enabled by mutableStateOf(prefs.getBoolean("enabled",false)); private set
    var revision by mutableStateOf(0); private set
    private fun flush(){runCatching{prefs.flush()}}
    fun toggle(){enabled=!enabled;prefs.putBoolean("enabled",enabled);flush();revision++}
    fun updateEnabled(value:Boolean){if(enabled==value)return;enabled=value;prefs.putBoolean("enabled",enabled);flush();revision++}
    fun value(setting:ModuleSetting)=prefs.get(setting.id,setting.default)
    fun key(id:String="toggle_key")=settings.firstOrNull{it.kind==SettingKind.KEY&&it.id==id}?.let{value(it).toIntOrNull()}?:0
    fun setKey(id:String,key:Int)=settings.firstOrNull{it.kind==SettingKind.KEY&&it.id==id}?.let{set(it,key.toString())}
    fun set(setting:ModuleSetting,value:String){
        val normalized=when(setting.kind){
            SettingKind.NUMBER -> value.toFloatOrNull()?.coerceIn(setting.min,setting.max)?.let{
                if(it % 1f == 0f) it.toInt().toString() else it.toString()
            } ?: this.value(setting)
            SettingKind.KEY -> value.toIntOrNull()?.coerceIn(0,512)?.toString() ?: this.value(setting)
            SettingKind.TOGGLE -> value.toBooleanStrictOrNull()?.toString() ?: this.value(setting)
            SettingKind.CHOICE -> value.takeIf(setting.choices::contains) ?: this.value(setting)
            else -> value.take(256)
        }
        prefs.put(setting.id,normalized);flush();revision++
    }
    fun reset(){settings.forEach{prefs.remove(it.id)};flush();revision++}
}
private fun key(id:String="toggle_key",title:String="Enable / disable keybind",default:Int)=ModuleSetting(id,title,SettingKind.KEY,default=default.toString())
private fun toggle(id:String,title:String,default:Boolean=false)=ModuleSetting(id,title,SettingKind.TOGGLE,default=default.toString())
private fun number(id:String,title:String,min:Float,max:Float,default:Float)=ModuleSetting(id,title,SettingKind.NUMBER,min,max,default=default.toString())
private fun text(id:String,title:String,default:String)=ModuleSetting(id,title,SettingKind.TEXT,default=default)
private fun color(id:String,title:String,default:String)=ModuleSetting(id,title,SettingKind.COLOR,default=default)
private fun choice(id:String,title:String,choices:List<String>,default:String)=ModuleSetting(id,title,SettingKind.CHOICE,choices=choices,default=default)
private fun info(id:String,message:String)=ModuleSetting(id,message,SettingKind.INFO,default=message)

object ModuleCatalog { val modules=listOf(
    ClientModule("auto_text","Auto Text Hot Key","Send your configured chat message with one key","auto-text",listOf(key("toggle_key","Enable / disable keybind",0),key("send_key","Send message keybind",0),text("message","Message","Hello!"))),
    ClientModule("better_sounds","Better Sounds","Custom shield, eating, hit, wind and mace sounds","better-sounds",listOf(key(default=0),toggle("shield_break","Shield break sound",true),toggle("eating","Eating sounds",true),toggle("hits","Hit sounds",true),toggle("wind_charges","Wind charge sounds",true),toggle("mace_hits","Mace hit sounds",true))),
    ClientModule("fog","Fog Customizer","Control client-side fog distance and density","fog-customizer",listOf(key(default=0),number("fog_distance","Fog distance",2f,512f,96f),number("fog_opacity","Fog opacity",0f,100f,65f))),
    ClientModule("fov","FOV Changer","Use a gameplay FOV beyond the vanilla limit","fov-changer",listOf(key(default=0),number("fov","Field of view",30f,180f,120f))),
    ClientModule("hitbox","Hitbox Categories","Player categories, hurt colors and attack-range feedback","hitbox",listOf(key(default=0))),
    ClientModule("team_highlight","Team Highlight","Color visible teammates, allies and enemies","team-highlight",listOf(key(default=0))),
    ClientModule("hit_color","Hit Color","Flash a configurable color across entities you hit","hit-color",listOf(key(default=0),number("flash_duration","Flash duration",0.05f,3f,1f),color("hit_color","Hit color","#66AFFF"))),
    ClientModule("hurt_cam","Hurt Cam","Disable the camera tilt that plays when you take damage","hurt-cam",listOf(key(default=0))),
    ClientModule("item_counter","Item Counter","Show the held item's total inventory count","item-counter",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","You can move the HUD through the Edit HUD button on the sidebar"))),
    ClientModule("motion_blur","Motion Blur","World-frame accumulation blur (OpenGL renderer)","motion-blur-new",listOf(key(default=0),number("strength","Blur strength",0f,100f,35f),number("responsiveness","Responsiveness",0f,100f,70f))),
    ClientModule("nick_hider","Nick Hider","Hide or locally replace your third-person nameplate","nick-hider",listOf(key(default=0),text("nickname","Change nickname","Player"),toggle("hide_nickname","Hide nickname",true))),
    ClientModule("particles","Particle Changer","Resize and fade rendered particles","particle-changer",listOf(key(default=0),number("particle_opacity","Particle opacity",0f,100f,100f),number("particle_size","Particle size",10f,300f,100f))),
    ClientModule("waila","WAILA","Show what the crosshair is targeting","waila",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","You can move the HUD through the Edit HUD button on the sidebar"))),
    ClientModule("weather","Weather Changer","Client-side time and precipitation controls","weather-changer",listOf(key(default=0),choice("time","Time of day",listOf("Vanilla","Sunrise","Day","Sunset","Night","Midnight"),"Vanilla"),choice("weather","Weather",listOf("Vanilla","Clear","Rain","Snow"),"Vanilla"))),
    ClientModule("waypoints","Waypoints","Saved world markers, distances, and death locations","waypoints",listOf(key(default=66),toggle("death_waypoints","Create death waypoints",false),toggle("coordinates","Show marker coordinates",false),number("max_distance","Maximum marker distance",64f,30000f,4096f))),
    ClientModule("freelook","Freelook","Detach the third-person camera without rotating your player","freelook",listOf(key("toggle_key","Freelook activation key",342),choice("mode","Activation mode",listOf("Hold","Toggle"),"Hold"))),
    ClientModule("zoom","Zoom","Smooth optical zoom with hold or toggle activation","zoom",listOf(key(default=0),key("activation_key","Zoom key",67),choice("mode","Activation mode",listOf("Hold","Toggle"),"Hold"),number("magnification","Magnification",2f,20f,4f),number("animation_ms","Animation response (ms)",30f,500f,150f))),
    ClientModule("fullbright","Fullbright","Client-only brightness boost; leaves options.txt untouched","fullbright",listOf(key(default=0))),
    ClientModule("fps_boost","FPS Boost","Lowers configurable chunk and entity render load while enabled","fps-boost",listOf(key(default=0),number("chunk_distance","Chunk render distance",2f,32f,8f),number("entity_distance","Entity render distance",25f,100f,75f))),
    ClientModule("toggle_sprint","ToggleSprint","Press your normal sprint key once to toggle sprinting","toggle-sprint",listOf(key(default=0))),
    ClientModule("toggle_sneak","ToggleSneak","Press your normal sneak key once to toggle sneaking","toggle-sneak",listOf(key(default=0))),
    ClientModule("streamer_mode","Streamer Mode","Hide chat and player lists and mask server addresses","streamer-mode",listOf(key(default=0),toggle("hide_chat","Hide chat",true),toggle("hide_players","Hide player list",true),toggle("hide_addresses","Hide server addresses",true),info("privacy_notice","Not an anonymity guarantee: server names, scenery, other mods and desktop captures may still identify a server."))),
    ClientModule("cps","CPS Counter","Left and right clicks in the last second","cps",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("fps","FPS Counter","Current frames per second","fps",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("keystrokes","Keystrokes","Movement, jump and mouse button overlay","keystrokes",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("armor_status","Armor Status","Equipped armor and remaining durability","armor-status",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("effect_status","Effect Status","Active effects and remaining timers","effect-status",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("coordinates","Coordinates / Biome","Position, facing direction and biome","coordinates",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("combo_counter","Combo Counter","Consecutive player hits without taking damage","combo-counter",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("inventory_hud","Compact Inventory","A miniature inventory grid on your HUD","inventory-hud",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD"))),
    ClientModule("memory_monitor","Memory Monitor","Used, allocated and maximum heap with a high-usage warning","memory-monitor",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize this HUD in Edit HUD. High usage is a warning, not a prediction of every crash."))),
    ClientModule("server_status","Server Status","Reported ping and available server timing information","server-status",listOf(key(default=0),toggle("show_background","Show background",true),info("hud_notice","Move and resize in Edit HUD. Exact packet loss and TPS are not supplied by ordinary servers."))),
    ClientModule("wind_charge_optimizer","Wind Charge Optimizer","Immediate client projectile visuals; server effects remain authoritative","wind-charge-optimizer",listOf(key(default=0))),
    ClientModule("pearl_optimizer","Pearl Optimizer","Immediate client pearl visuals; server teleport remains authoritative","pearl-optimizer",listOf(key(default=0))),
    ClientModule("crosshair","Custom Crosshair","Build and preview your own crosshair","custom-crosshair",listOf(key(default=0),choice("shape","Shape",listOf("Cross","Dot","Circle","Chevron"),"Cross"),number("length","Line length",1f,30f,7f),number("gap","Center gap",0f,20f,3f),number("thickness","Thickness",1f,10f,2f),toggle("outline","Outline",true),color("crosshair_color","Crosshair color","#FFFFFF"),number("crosshair_opacity","Opacity",0f,100f,100f),toggle("center_dot","Center dot",false),toggle("dynamic_spread","Dynamic spread",false)))
); fun byId(id:String)=modules.firstOrNull{it.id==id} }
