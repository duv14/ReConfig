/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import org.polyfrost.oneconfig.internal.ui.navigation.graph.FriendsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.MessagesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModulesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.SettingsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogGraph

data class NavigationRoute(
    val id: String,
    val icon: String,
    val route: Any,
)

class NavigationGroup(
    val id: String,
    vararg val routes: NavigationRoute
)

val NavigationGroups = listOf(
    NavigationGroup(
        id = "Social",
        NavigationRoute(
            id = "friends",
            icon = "profiles",
            route = FriendsGraph
        ),
        NavigationRoute(
            id = "messages",
            icon = "text",
            route = MessagesGraph
        )
    ),
    NavigationGroup(
        id = "Quality of Life",
        NavigationRoute(id = "modules", icon = "qol", route = ModulesGraph)
    ),
    NavigationGroup(
        id = "Updates",
        NavigationRoute(id = "changelog", icon = "changelog", route = ChangeLogGraph)
    ),
    NavigationGroup(
        id = "Miscellaneous",
        NavigationRoute(id = "settings", icon = "settings", route = SettingsGraph)
    )
)

fun searchPlaceholder(destination: NavDestination?): String {
    val section = NavigationGroups
        .asSequence()
        .flatMap { it.routes.asSequence() }
        .firstOrNull { def ->
            destination?.hierarchy?.any { it.hasRoute(def.route::class) } == true
        }
    return if (section != null) "Search ${section.id}..." else "Search..."
}
