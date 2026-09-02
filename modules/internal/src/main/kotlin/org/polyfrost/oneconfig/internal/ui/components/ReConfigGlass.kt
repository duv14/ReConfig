/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

/** Light-catching top edge and darker lower surface shared by ReConfig glass panels. */
fun Modifier.reConfigGlass(shape: Shape, surface: Color, edge: Color, tint: Color = Color.Transparent): Modifier {
    val topTint = if (tint.alpha > 0f) tint.copy(alpha = tint.alpha * .42f) else Color.White.copy(alpha = .012f)
    return background(
        brush = Brush.verticalGradient(
            listOf(topTint.compositeOver(surface.copy(alpha = .97f)), surface, Color.Black.copy(alpha = .10f).compositeOver(surface))
        ),
        shape = shape,
    ).border(
        width = 1.dp,
        brush = Brush.verticalGradient(listOf(edge.copy(alpha = .28f), edge.copy(alpha = .13f), Color.Black.copy(alpha = .26f))),
        shape = shape,
    )
}
