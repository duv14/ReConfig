/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.compose.layout

sealed class PolySize {

    object Wrap : PolySize()

    data class Px(val value: Float) : PolySize()

    data class Percent(val value: Float) : PolySize()

    object Fill : PolySize()

}
