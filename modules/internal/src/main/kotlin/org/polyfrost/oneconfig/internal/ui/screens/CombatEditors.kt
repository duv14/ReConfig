/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Integrated editors for duv14's Hitbox Categories and Team Highlight.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.reconfig.CombatRepository
import org.polyfrost.oneconfig.internal.reconfig.combat.HighlightConfig.PlayerRole
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.settings.SliderControl
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable private fun CombatInput(value: String, change: (String) -> Unit) {
    val t=LocalTheme.current
    BasicTextField(value, change, singleLine=true,textStyle=TextStyle(color=t.textColor,fontSize=13.sp,fontFamily=t.typography.family),cursorBrush=SolidColor(Accent),
        modifier=Modifier.width(190.dp).reConfigGlass(t.buttonShape,t.modCardBackground,t.borderColor).padding(horizontal=12.dp,vertical=10.dp))
}
@Composable private fun CombatButton(label: String, action: () -> Unit) {
    val t=LocalTheme.current
    val source=rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    Box(Modifier.height(36.dp).reConfigGlass(t.buttonShape,if(hovered) Accent.copy(.14f) else t.modCardBackground,if(hovered) Accent.copy(.55f) else t.borderColor)
        .onClick(source,action).padding(horizontal=12.dp),contentAlignment=Alignment.Center) { Text(label,color=t.textColor,fontSize=13.sp) }
}
@Composable private fun CombatPanel(content: @Composable ColumnScope.() -> Unit) {
    val t=LocalTheme.current
    Column(Modifier.fillMaxWidth().reConfigGlass(t.sideBarNavigationEntryShape,t.componentBackground,t.borderColor).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
@Composable internal fun HitboxCategoryEditor() {
    var revision by remember { mutableStateOf(0) }; revision
    val config=CombatRepository.hitboxes
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun edit(action: () -> Unit) { runCatching { action(); check(CombatRepository.save()) { CombatRepository.error ?: "Cannot save settings" } }.onSuccess { error=null }.onFailure { error=it.message }; revision++ }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Hitbox categories",color=LocalTheme.current.textColor)
        Text("Rename categories, choose normal / hurt colors and assign usernames. Hurt flash lasts one second.",color=LocalTheme.current.textColorSecondary)
        CombatPanel {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("Thickness",color=LocalTheme.current.textColor,modifier=Modifier.weight(1f))
                SliderControl(config.hitboxThickness.coerceIn(.1f,10f),{ value -> edit { config.hitboxThickness=value } },.1f,10f,.1f,Modifier.width(240.dp))
                Text("%.1f×".format(config.hitboxThickness),color=LocalTheme.current.textColor,modifier=Modifier.width(48.dp))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("Aimed / in-range color",color=LocalTheme.current.textColor); ColorControl(config.rangeColor) { edit { config.rangeColor=it } } }
        }
        CombatPanel {
            Text("New category name",color=LocalTheme.current.textColorSecondary,fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { CombatInput(name){name=it.take(64)}; CombatButton("Add category") { edit { config.addCategory(name,"#55FF55"); name="" } } }
        }
        config.categories.toList().forEach { category -> key(category.id) {
            var draftName by remember { mutableStateOf(category.name) }
            var player by remember { mutableStateOf("") }
            CombatPanel {
                Text("Category name",color=LocalTheme.current.textColorSecondary,fontSize=12.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { CombatInput(draftName){draftName=it.take(64)}; CombatButton("Rename") { edit { category.name=draftName } }; CombatButton("Delete") { edit { config.removeCategory(category.id) } } }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("Normal color",color=LocalTheme.current.textColor); ColorControl(category.color) { edit { category.color=it } } }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("Hurt color",color=LocalTheme.current.textColor); ColorControl(category.hurtColor) { edit { category.hurtColor=it } } }
                Text("Minecraft username",color=LocalTheme.current.textColorSecondary,fontSize=12.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { CombatInput(player){player=it.take(16)}; CombatButton("Assign player") { edit { require(player.trim().matches(Regex("[A-Za-z0-9_]{1,16}"))) { "Enter a Minecraft username" }; config.assignPlayer(category.id,player); player="" } } }
                if(category.players.isEmpty()) Text("No players assigned",color=LocalTheme.current.textColorSecondary,fontSize=12.sp)
                category.players.toList().forEach { username -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { Text(username,color=LocalTheme.current.textColor,modifier=Modifier.weight(1f)); CombatButton("Remove") { edit { category.removePlayer(username) } } } }
            }
        } }
        (error ?: CombatRepository.error)?.let { Text(it,color=LocalTheme.current.textColorSecondary) }
    }
}

@Composable internal fun TeamHighlightEditor() {
    var revision by remember { mutableStateOf(0) }; revision
    val config=CombatRepository.highlights
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(PlayerRole.TEAMMATE) }
    var error by remember { mutableStateOf<String?>(null) }
    fun edit(action: () -> Unit) { runCatching { action(); check(CombatRepository.save()) { CombatRepository.error ?: "Cannot save settings" } }.onSuccess { error=null }.onFailure { error=it.message }; revision++ }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Tracked players",color=LocalTheme.current.textColor)
        Text("Highlights require line of sight and never reveal invisible tracked players.",color=LocalTheme.current.textColorSecondary)
        CombatPanel {
            PlayerRole.values().forEach { selected -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text(selected.name.lowercase().replaceFirstChar { it.uppercase() }+" color",color=LocalTheme.current.textColor); ColorControl("#%06X".format(config.color(selected))) { value -> edit { val color=value.removePrefix("#").toInt(16); when(selected) { PlayerRole.TEAMMATE -> config.teammateColor=color; PlayerRole.ALLY -> config.allyColor=color; PlayerRole.ENEMY -> config.enemyColor=color } } } } }
        }
        CombatPanel {
            Text("Minecraft username",color=LocalTheme.current.textColorSecondary,fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { CombatInput(name){name=it.take(16)}; CombatButton(role.name.lowercase().replaceFirstChar { it.uppercase() }) { role=PlayerRole.values()[(role.ordinal+1)%3] } }
            CombatButton("Add / update player") { edit { config.setRole(name,role); name="" } }
        }
        if(config.trackedPlayers.isEmpty()) Text("No tracked players yet",color=LocalTheme.current.textColorSecondary,fontSize=12.sp)
        config.trackedPlayers.toList().forEach { player -> CombatPanel { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { Text(player.name,color=LocalTheme.current.textColor,modifier=Modifier.weight(1f)); CombatButton(player.role.name.lowercase().replaceFirstChar { it.uppercase() }) { edit { config.setRole(player.name,PlayerRole.values()[(player.role.ordinal+1)%3]) } }; CombatButton("Remove") { edit { config.remove(player.name) } } } } }
        (error ?: CombatRepository.error)?.let { Text(it,color=LocalTheme.current.textColorSecondary) }
    }
}
