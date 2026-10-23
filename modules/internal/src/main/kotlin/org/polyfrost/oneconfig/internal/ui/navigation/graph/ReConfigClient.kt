/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.ReConfigModulesScreen
import org.polyfrost.oneconfig.internal.ui.screens.ReConfigSettingsScreen
import org.polyfrost.oneconfig.internal.ui.screens.YourProfileScreen
import org.polyfrost.oneconfig.internal.ui.screens.PublicServersScreen

@Serializable data object ModulesGraph
@Serializable data object ModulesRoute
@Serializable data object SettingsGraph
@Serializable data object SettingsRoute
@Serializable data object YourProfileGraph
@Serializable data object YourProfileRoute
@Serializable data object PublicServersGraph
@Serializable data object PublicServersRoute

fun NavGraphBuilder.reConfigClientGraphs() {
    navigation<ModulesGraph>(startDestination = ModulesRoute) { composable<ModulesRoute> { ReConfigModulesScreen() } }
    navigation<SettingsGraph>(startDestination = SettingsRoute) { composable<SettingsRoute> { ReConfigSettingsScreen() } }
    navigation<YourProfileGraph>(startDestination = YourProfileRoute) { composable<YourProfileRoute> { YourProfileScreen() } }
    navigation<PublicServersGraph>(startDestination = PublicServersRoute) { composable<PublicServersRoute> { PublicServersScreen() } }
}
