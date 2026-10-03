/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.compose

import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager

/**
 * Warms up the Compose UI by rendering [OneConfigInterface] so the first screen open doesn't stutter
 */
object ComposePreloader {
    @Volatile
    private var gpuWarmed = false

    fun preloadGpuWarmup() {
        if (gpuWarmed) return
        gpuWarmed = true
        NotificationsManager.ensureInitialized()
    }
}
