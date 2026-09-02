/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.api.hud.v1

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface LegacyHudMarker {
    val supportsScale: Boolean get() = true
}
