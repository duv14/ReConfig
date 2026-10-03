/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import java.nio.file.Path

data class WaypointContext(val world: String, val dimension: String, val x: Double, val y: Double, val z: Double)

/** Minecraft publishes context; the UI/storage layer has no Minecraft dependency. */
object WaypointRepository {
    @Volatile var context: WaypointContext? = null
    @Volatile var error: String? = null
    val store: WaypointStore? by lazy {
        runCatching { WaypointStore(Path.of(System.getProperty("user.dir"),"config","reconfig","waypoints.properties")) }
            .onFailure { error = it.message }.getOrNull()
    }
}
