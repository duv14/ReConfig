/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.navigation

import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogEntryRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.FriendsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.FriendsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.MessagesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.MessagesRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModulesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModulesRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.SettingsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.SettingsRoute
import java.util.prefs.Preferences

object ReConfigRouteMemory {
    private const val RESTORE_WINDOW_MS = 2 * 60 * 60 * 1000L
    private val prefs = Preferences.userRoot().node("dev/duv14/reconfig/navigation")

    fun rememberPage(route: Any?, now: Long = System.currentTimeMillis()) {
        val id = idFor(route) ?: return
        prefs.put("lastPage", id)
        prefs.putLong("closedAt", now)
        runCatching { prefs.flush() }
    }

    fun routeForRememberedPage(now: Long = System.currentTimeMillis()): Any {
        val closedAt = prefs.getLong("closedAt", 0L)
        if (closedAt <= 0L || now - closedAt !in 0..RESTORE_WINDOW_MS) return FriendsGraph
        return routeFor(prefs.get("lastPage", "friends"))
    }

    private fun idFor(route: Any?) = when (route) {
        FriendsGraph, FriendsRoute -> "friends"
        MessagesGraph, MessagesRoute -> "messages"
        ModulesGraph, ModulesRoute -> "modules"
        ChangeLogGraph, ChangeLogRoute, is ChangeLogEntryRoute -> "changelog"
        SettingsGraph, SettingsRoute -> "settings"
        else -> null
    }

    private fun routeFor(id: String): Any = when (id) {
        "messages" -> MessagesGraph
        "modules" -> ModulesGraph
        "changelog" -> ChangeLogGraph
        "settings" -> SettingsGraph
        else -> FriendsGraph
    }
}
