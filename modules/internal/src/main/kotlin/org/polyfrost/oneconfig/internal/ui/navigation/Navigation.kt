/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.navigation

import androidx.navigation.NavGraphBuilder
import org.polyfrost.oneconfig.internal.ui.navigation.graph.socialGraphs
import org.polyfrost.oneconfig.internal.ui.navigation.graph.reConfigClientGraphs
import org.polyfrost.oneconfig.internal.ui.navigation.graph.changeLogGraph

fun NavGraphBuilder.navigation() {
    socialGraphs()
    reConfigClientGraphs()
    changeLogGraph()
}
