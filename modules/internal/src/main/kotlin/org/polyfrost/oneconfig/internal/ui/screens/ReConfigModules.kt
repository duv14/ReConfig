/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.reconfig.*
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.settings.SliderControl
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerPopup
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModulesGraph
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus

private object ModuleSearchSelection { var pending by mutableStateOf<ClientModule?>(null) }

@Composable fun ReConfigModulesScreen(){
    ShellState.title="Modules"
    var selected by remember{mutableStateOf<ClientModule?>(ModuleSearchSelection.pending)}
    LaunchedEffect(Unit){ModuleSearchSelection.pending=null}
    selected?.let{ModuleEditor(it){selected=null}}?:ModulesGrid{selected=it}
}

@Composable internal fun ReConfigModuleSearchResults(query:String) {
    val normalized=query.trim()
    val matches=remember(normalized){if(normalized.isEmpty()) emptyList() else ModuleCatalog.modules.filter{module->module.name.contains(normalized,true)||module.description.contains(normalized,true)||module.id.replace('_',' ').contains(normalized,true)}}
    if(matches.isEmpty())return
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("RECONFIG MODULES",color=LocalTheme.current.textColorSecondary,fontSize=11.sp,fontWeight=FontWeight.Medium)
        LazyVerticalGrid(columns=GridCells.Fixed(4),verticalArrangement=Arrangement.spacedBy(19.dp),horizontalArrangement=Arrangement.spacedBy(19.dp),modifier=Modifier.fillMaxWidth().heightIn(max=320.dp)){
            items(matches,key={it.id}){module->ModuleCard(module){ModuleSearchSelection.pending=module;LocalNavController.wrapper.navigate(ModulesGraph)}}
        }
    }
}

@Composable private fun ModulesGrid(open:(ClientModule)->Unit){
    var category by remember { mutableStateOf("All") }
    val qualityOfLife=setOf("auto_text","better_sounds","nick_hider","waypoints","toggle_sprint","toggle_sneak","streamer_mode","fps_boost")
    val gameplay=setOf("hitbox","hurt_cam","freelook","zoom","wind_charge_optimizer","pearl_optimizer")
    val visual=setOf("fog","fov","hit_color","motion_blur","particles","weather","crosshair","fullbright","team_highlight")
    val huds=setOf("item_counter","waila","cps","fps","keystrokes","armor_status","effect_status","coordinates","combo_counter","inventory_hud","memory_monitor","server_status")
    val filteredModules=when(category){"Quality of Life"->ModuleCatalog.modules.filter{it.id in qualityOfLife};"Gameplay"->ModuleCatalog.modules.filter{it.id in gameplay};"Visual"->ModuleCatalog.modules.filter{it.id in visual};"HUDs"->ModuleCatalog.modules.filter{it.id in huds};else->ModuleCatalog.modules}
    Column(verticalArrangement=Arrangement.spacedBy(18.dp)){
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("All","Quality of Life","Gameplay","Visual","HUDs").forEach{label->Chip(label,category==label){category=label}}}
        LazyVerticalGrid(columns=GridCells.Fixed(4),verticalArrangement=Arrangement.spacedBy(19.dp),horizontalArrangement=Arrangement.spacedBy(19.dp),modifier=Modifier.padding(end=14.dp)){
            items(filteredModules,key={it.id}){ModuleCard(it){open(it)}}
        }
    }
}

@Composable private fun Chip(label:String,selected:Boolean,click:()->Unit){
    val t=LocalTheme.current;val s=rememberInteractionSource();val hovered by s.collectIsHoveredAsState();val bg by animateColorAsState(if(selected)Accent else if(hovered)t.modCardBackground else t.componentBackground)
    Row(Modifier.height(34.dp).reConfigGlass(t.buttonShape,bg,if(selected)Accent else t.borderColor,if(selected)Accent.copy(.10f) else Color.Transparent).onClick(s,click).pointerHoverIcon(PointerIcon.Hand).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text(label,color=if(selected)t.accentTextColor else t.textColor,fontSize=13.sp)}
}

@Composable private fun ModuleCard(module:ClientModule,open:()->Unit){
    val t=LocalTheme.current;val s=rememberInteractionSource();val settingsSource=rememberInteractionSource();val hovered by s.collectIsHoveredAsState();val scale by animateFloatAsState(if(hovered)1.006f else 1f,animationSpec=tween(180, easing = FastOutSlowInEasing))
    val transition=tween<Color>(260,easing=FastOutSlowInEasing)
    val iconColor by animateColorAsState(if(module.enabled)Color(0xFFADD7FA) else t.textColorSecondary,transition)
    val dotColor by animateColorAsState(if(module.enabled)Color(0xFF8BCBFF) else Color(0xFF555E69),transition)
    val footerColor by animateColorAsState(if(module.enabled)Color(0x665F8FB8) else t.componentBackground,transition)
    val titleColor by animateColorAsState(if(module.enabled)Color(0xFFF0F8FF) else t.textColor,transition)
    Box(Modifier.fillMaxWidth().height(140.dp).graphicsLayer{scaleX=scale;scaleY=scale}.reConfigGlass(t.modCardShape,t.modCardBackground,t.borderColor,if(module.enabled)Accent.copy(.18f) else Color.Transparent).clip(t.modCardShape).onClick(s){module.toggle()}.pointerHoverIcon(PointerIcon.Hand)){
        Column(Modifier.fillMaxSize()){
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Icon(module.icon,color=iconColor,modifier=Modifier.size(42.dp));Box(Modifier.align(Alignment.TopEnd).padding(9.dp).size(8.dp).background(dotColor,t.circleShape));Box(Modifier.align(Alignment.TopStart).padding(7.dp).size(25.dp).background(t.componentBackground.copy(.82f),t.buttonShape).border(1.dp,t.borderColor,t.buttonShape).onClick(settingsSource){open()}.pointerHoverIcon(PointerIcon.Hand),contentAlignment=Alignment.Center){Icon("settings",color=t.textColorSecondary,modifier=Modifier.size(13.dp));Text("Module settings",color=Color.Transparent,fontSize=1.sp)}}
            Box(Modifier.fillMaxWidth().height(36.dp).background(footerColor),contentAlignment=Alignment.Center){Text(module.name,color=titleColor,fontSize=14.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(horizontal=6.dp))}
        }
    }
}

@Composable private fun ModuleEditor(module:ClientModule,back:()->Unit){
    val t=LocalTheme.current;module.revision
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(50.dp).background(Accent.copy(.16f),t.modCardShape),contentAlignment=Alignment.Center){Icon(module.icon,color=Accent,modifier=Modifier.size(28.dp))};Column{Text(module.name,color=t.textColor,fontSize=22.sp,fontWeight=FontWeight.SemiBold);Text(module.description,color=t.textColorSecondary,fontSize=13.sp)}}
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){GlassButton("Reset","refresh"){module.reset()};SwitchControl(module.enabled){module.toggle()};GlassButton("Back","left-arrow",back)}
        }
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(module.id=="crosshair") item { CrosshairPreview(module) }
            items(module.settings,key={it.id}){SettingRow(module,it)}
            if(module.id=="waypoints") item { WaypointManager() }
            if(module.id=="hitbox") item { HitboxCategoryEditor(back) }
            if(module.id=="team_highlight") item { TeamHighlightEditor() }
        }
    }
}

@Composable private fun SettingRow(module:ClientModule,setting:ModuleSetting){
    val t=LocalTheme.current;var value by remember(setting.id,module.revision){mutableStateOf(module.value(setting))}
    Row(Modifier.fillMaxWidth().heightIn(min=68.dp).reConfigGlass(t.sideBarNavigationEntryShape,t.componentBackground,t.borderColor).padding(horizontal=16.dp,vertical=11.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f).padding(end=20.dp)){Text(setting.title,color=t.textColor,fontWeight=FontWeight.Medium);Text(if(setting.kind==SettingKind.KEY)"Click, then press a keyboard key" else settingHint(setting,value),color=t.textColorSecondary,fontSize=12.sp)}
        when(setting.kind){
            SettingKind.TOGGLE->SwitchControl(value.toBoolean()){value=it.toString();module.set(setting,value)}
            SettingKind.NUMBER->{val n=value.toFloatOrNull()?.coerceIn(setting.min,setting.max)?:setting.min;Row(Modifier.width(310.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){SliderControl(n,{v->value=v.toString();module.set(setting,value)},setting.min,setting.max,if(setting.max-setting.min<=5f).05f else if(setting.max-setting.min>200)10f else 1f,Modifier.weight(1f));ValuePill(if(n%1f==0f)n.toInt().toString() else "%.1f".format(n))}}
            SettingKind.CHOICE->GlassButton(value,"down"){val i=setting.choices.indexOf(value).coerceAtLeast(0);value=setting.choices[(i+1)%setting.choices.size];module.set(setting,value)}
            SettingKind.KEY->{val listening=KeyCapture.moduleId==module.id&&KeyCapture.settingId==setting.id;GlassButton(if(listening)"Press a key…" else KeyCapture.label(module.key(setting.id)),"keyboard"){if(listening)KeyCapture.cancel() else KeyCapture.begin(module.id,setting.id)}}
            SettingKind.COLOR->ColorControl(value){value=it;module.set(setting,it)}
            SettingKind.TEXT->StyledInput(value){value=it;module.set(setting,it)}
            SettingKind.INFO->Text(setting.default,color=t.textColorSecondary,fontSize=12.sp,modifier=Modifier.widthIn(max=330.dp))
        }
    }
}

private fun settingHint(s:ModuleSetting,v:String)=when(s.kind){SettingKind.NUMBER->"Range ${s.min.toInt()}–${s.max.toInt()}";SettingKind.TOGGLE->if(v.toBoolean())"Enabled" else "Disabled";SettingKind.COLOR->"Module color";else->v}

@Composable internal fun ColorControl(value:String,change:(String)->Unit){
    val parsed=runCatching{Color((value.removePrefix("#").toLong(16) or 0xFF000000).toInt())}.getOrDefault(Accent)
    var expanded by remember { mutableStateOf(false) }
    val model = remember { ColorPickerModel(parsed, false) }
    LaunchedEffect(value, expanded) { if (!expanded) model.applyFrom(parsed) }
    Box {
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(28.dp).background(parsed,LocalTheme.current.circleShape).border(1.dp,LocalTheme.current.borderColor,LocalTheme.current.circleShape))
            GlassButton(value,"paintbrush"){ expanded = !expanded }
        }
        if (expanded) Popup(alignment=Alignment.TopEnd,offset=IntOffset(0,40),onDismissRequest={expanded=false},properties=PopupProperties(focusable=true)) {
            ColorPickerPopup(model=model,onColorChanged={change("#%06X".format(it.toArgb() and 0xFFFFFF))},onClose={expanded=false},chromaCapable=false)
        }
    }
}

@Composable private fun StyledInput(value:String,change:(String)->Unit){val t=LocalTheme.current;BasicTextField(value,change,singleLine=true,textStyle=TextStyle(color=t.textColor,fontSize=13.sp,fontFamily=t.typography.family),cursorBrush=SolidColor(Accent),modifier=Modifier.width(210.dp).trackTextInputFocus().reConfigGlass(t.buttonShape,t.modCardBackground,t.borderColor).padding(horizontal=12.dp,vertical=9.dp))}
@Composable private fun ValuePill(value:String){val t=LocalTheme.current;Box(Modifier.widthIn(min=58.dp).reConfigGlass(t.buttonShape,t.modCardBackground,t.borderColor).padding(horizontal=10.dp,vertical=7.dp),contentAlignment=Alignment.Center){Text(value,color=t.textColor,fontSize=12.sp)}}
@Composable private fun GlassButton(label:String,icon:String,click:()->Unit){val t=LocalTheme.current;val s=rememberInteractionSource();val h by s.collectIsHoveredAsState();val bg by animateColorAsState(if(h)Accent.copy(.14f) else t.modCardBackground);Row(Modifier.height(34.dp).reConfigGlass(t.buttonShape,bg,if(h)Accent.copy(.55f) else t.borderColor,if(h)Accent.copy(.08f) else Color.Transparent).onClick(s,click).pointerHoverIcon(PointerIcon.Hand).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Icon(icon,color=t.textColor,modifier=Modifier.size(14.dp));Text(label,color=t.textColor,fontSize=13.sp)}}

@Composable fun ReConfigSettingsScreen(){
    // Use the framework's native config renderer so ReConfig preferences receive
    // the same controls, spacing, search metadata and persistence as every config.
    Preferences()
}

private fun saveUiConfig(){runCatching{OneConfigConfig.INSTANCE?.save()}}
@Composable private fun LiveSwitch(title:String,initial:Boolean,change:(Boolean)->Unit){val t=LocalTheme.current;var value by remember(title){mutableStateOf(initial)};Row(Modifier.fillMaxWidth().height(68.dp).reConfigGlass(t.sideBarNavigationEntryShape,t.componentBackground,t.borderColor).padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(title,color=t.textColor);SwitchControl(value){value=it;change(it)}}}
@Composable private fun LiveSlider(title:String,initial:Float,min:Float,max:Float,change:(Float)->Unit){val t=LocalTheme.current;var value by remember(title){mutableStateOf(initial)};Row(Modifier.fillMaxWidth().height(68.dp).reConfigGlass(t.sideBarNavigationEntryShape,t.componentBackground,t.borderColor).padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(title,color=t.textColor);Row(Modifier.width(320.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){SliderControl(value,{value=it;change(it)},min,max,if(max<=2f).05f else 1f,Modifier.weight(1f));ValuePill(if(max<=2f)"%.2f".format(value) else value.toInt().toString())}}}
