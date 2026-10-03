/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.hud

import org.jetbrains.skia.Canvas

object LegacyHudOverlayBridge {
    @Volatile
    @JvmField
    var painter: ((Canvas) -> Unit)? = null
}
