/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.FriendsScreen
import org.polyfrost.oneconfig.internal.ui.screens.MessagesScreen

@Serializable data object FriendsGraph
@Serializable data object FriendsRoute
@Serializable data object MessagesGraph
@Serializable data object MessagesRoute

fun NavGraphBuilder.socialGraphs() {
    navigation<FriendsGraph>(startDestination = FriendsRoute) {
        composable<FriendsRoute> { FriendsScreen() }
    }
    navigation<MessagesGraph>(startDestination = MessagesRoute) {
        composable<MessagesRoute> { MessagesScreen() }
    }
}
