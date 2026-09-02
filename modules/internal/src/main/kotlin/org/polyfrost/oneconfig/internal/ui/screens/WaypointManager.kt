/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.internal.reconfig.*
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.util.UUID

@Composable fun WaypointManager() {
    val t=LocalTheme.current
    val store=remember { WaypointRepository.store }
    var context by remember { mutableStateOf(WaypointRepository.context) }
    var entries by remember { mutableStateOf(store?.all().orEmpty()) }
    var error by remember { mutableStateOf(WaypointRepository.error) }
    var editing by remember { mutableStateOf<WaypointStore.Entry?>(null) }
    var name by remember { mutableStateOf("Home") }
    var x by remember { mutableStateOf("") };var y by remember { mutableStateOf("") };var z by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#8BCBFF") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { while(true) { context=WaypointRepository.context;entries=store?.all().orEmpty();delay(500) } }
    fun act(block:()->Unit) { runCatching(block).onSuccess { entries=store?.all().orEmpty();error=null }.onFailure { error=it.message?:"Unable to save waypoint" } }
    fun usePosition() { context?.let { x="%.1f".format(java.util.Locale.ROOT,it.x);y="%.1f".format(java.util.Locale.ROOT,it.y);z="%.1f".format(java.util.Locale.ROOT,it.z) } }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Waypoint manager",color=t.textColor,fontSize=20.sp)
        Text("Locations are saved per server/world and dimension. Markers are visible through terrain.",color=t.textColorSecondary,fontSize=12.sp)
        if(context==null) Text("Join a world to create waypoints. Saved locations can still be edited below.",color=t.textColorSecondary)
        Column(Modifier.fillMaxWidth().reConfigGlass(t.modCardShape,t.modCardBackground,t.borderColor).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(editing==null)"New waypoint" else "Edit waypoint",color=t.textColor)
            WaypointInput("Name",name,{name=it},Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                WaypointInput("X",x,{x=it},Modifier.weight(1f));WaypointInput("Y",y,{y=it},Modifier.weight(1f));WaypointInput("Z",z,{z=it},Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                ColorControl(color){color=it}
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    WaypointButton("Use current position"){usePosition()}
                    WaypointButton("Save") { act {
                        val s=store?:kotlin.error("Waypoint file could not be loaded")
                        val previous=editing
                        val c=context
                        require(previous!=null||c!=null){"Join a world before creating a waypoint"}
                        val entry=WaypointStore.Entry(previous?.id()?:UUID.randomUUID().toString(),previous?.world()?:c!!.world,previous?.dimension()?:c!!.dimension,
                            name.trim(),x.toDoubleOrNull()?:kotlin.error("Enter a valid X coordinate"),y.toDoubleOrNull()?:kotlin.error("Enter a valid Y coordinate"),z.toDoubleOrNull()?:kotlin.error("Enter a valid Z coordinate"),
                            (color.removePrefix("#").toLong(16) or 0xFF000000).toInt(),previous?.visible()?:true)
                        s.put(entry);editing=null
                    } }
                    if(editing!=null)WaypointButton("Cancel"){editing=null}
                }
            }
            error?.let { Text(it,color=Color(0xFFFF8088),fontSize=12.sp) }
        }
        if(entries.isEmpty())Text("No saved waypoints yet. Use current position, then Save.",color=t.textColorSecondary)
        entries.forEach { e -> key(e.id()) {
            Row(Modifier.fillMaxWidth().reConfigGlass(t.modCardShape,t.componentBackground,t.borderColor).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                SwitchControl(e.visible()){visible->act { store!!.put(WaypointStore.Entry(e.id(),e.world(),e.dimension(),e.name(),e.x(),e.y(),e.z(),e.color(),visible)) }}
                Column(Modifier.weight(1f)) {
                    Text(e.name(),color=Color(e.color()))
                    Text("${e.x().toInt()}, ${e.y().toInt()}, ${e.z().toInt()} · ${e.dimension().removePrefix("minecraft:")}",color=t.textColorSecondary,fontSize=12.sp)
                    if(context?.world!=e.world())Text("Another world/server",color=t.textColorSecondary,fontSize=11.sp)
                }
                WaypointButton("Edit"){editing=e;name=e.name();x=e.x().toString();y=e.y().toString();z=e.z().toString();color="#%06X".format(e.color() and 0xFFFFFF)}
                WaypointButton(if(pendingDelete==e.id())"Confirm delete" else "Delete",true) {
                    if(pendingDelete==e.id())act { store!!.remove(e.id());pendingDelete=null;if(editing?.id()==e.id())editing=null }
                    else pendingDelete=e.id()
                }
            }
        } }
    }
}

@Composable private fun WaypointInput(label:String,value:String,change:(String)->Unit,modifier:Modifier) {
    val t=LocalTheme.current
    Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(label,color=t.textColorSecondary,fontSize=12.sp)
        BasicTextField(value,change,singleLine=true,textStyle=TextStyle(color=t.textColor,fontSize=13.sp,fontFamily=t.typography.family),cursorBrush=SolidColor(Accent),
            modifier=Modifier.fillMaxWidth().trackTextInputFocus().reConfigGlass(t.buttonShape,t.componentBackground,t.borderColor).padding(10.dp))
    }
}
@Composable private fun WaypointButton(label:String,danger:Boolean=false,action:()->Unit) {
    val t=LocalTheme.current;val source=rememberInteractionSource()
    Box(Modifier.reConfigGlass(t.buttonShape,t.componentBackground,t.borderColor).onClick(source,action).padding(horizontal=12.dp,vertical=9.dp)) {
        Text(label,color=if(danger)Color(0xFFFF8088) else t.textColor,fontSize=12.sp)
    }
}
