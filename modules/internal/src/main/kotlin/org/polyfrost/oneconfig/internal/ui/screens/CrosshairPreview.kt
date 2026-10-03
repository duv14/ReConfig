/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.reconfig.ClientModule
import org.polyfrost.oneconfig.internal.reconfig.CrosshairPattern
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.reConfigGlass
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable fun CrosshairPreview(module: ClientModule) {
    module.revision
    val theme = LocalTheme.current
    fun value(id:String) = module.value(module.settings.first { it.id==id })
    val length=value("length").toFloatOrNull()?.toInt()?:7
    val gap=value("gap").toFloatOrNull()?.toInt()?:3
    val thickness=value("thickness").toFloatOrNull()?.toInt()?:2
    val opacity=(value("crosshair_opacity").toFloatOrNull()?:100f).coerceIn(0f,100f)/100f
    val color=runCatching { Color((value("crosshair_color").removePrefix("#").toLong(16) or 0xFF000000).toInt()).copy(alpha=opacity) }.getOrDefault(Color.White)
    val shape=value("shape");val dot=value("center_dot").toBoolean();val outline=value("outline").toBoolean()
    Column(Modifier.fillMaxWidth().reConfigGlass(theme.modCardShape,theme.modCardBackground,theme.borderColor).padding(16.dp)) {
        Text(if(module.enabled) "Live crosshair preview" else "Preview — enable the module to show it in-game",color=theme.textColor)
        if(opacity==0f) Text("Opacity is 0%: your crosshair is invisible.",color=theme.textColorSecondary)
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val scale=1.25.dp.toPx()
            for(pass in (if(outline)0 else 1)..1) for(y in -65..65) for(x in -65..65) {
                var fill=CrosshairPattern.sample(shape,x,y,length,gap,thickness,dot)
                if(pass==0 && !fill) for(dy in -1..1) for(dx in -1..1)
                    if(CrosshairPattern.sample(shape,x+dx,y+dy,length,gap,thickness,dot))fill=true
                if(fill)drawRect(if(pass==0)Color.Black.copy(alpha=opacity) else color,
                    Offset(size.width/2+x*scale,size.height/2+y*scale),Size(scale,scale))
            }
        }
    }
}
