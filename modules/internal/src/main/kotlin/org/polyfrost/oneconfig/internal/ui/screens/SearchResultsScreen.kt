/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.navigation.NavigationGroups
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun SearchResultsScreen(query: String) {
    val theme = LocalTheme.current
    val normalized = query.trim()
    val hasReConfigModules = org.polyfrost.oneconfig.internal.reconfig.ModuleCatalog.modules.any {
        it.name.contains(normalized, true) || it.description.contains(normalized, true) ||
            it.id.replace('_', ' ').contains(normalized, true) ||
            it.settings.any { setting -> setting.title.contains(normalized, true) }
    }
    val pages = NavigationGroups.flatMap { it.routes.asList() }.filter { it.id.contains(normalized, true) }
    if (!hasReConfigModules && pages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No ReConfig results for \"$normalized\"", color = theme.textColorSecondary, fontSize = 15.sp)
        }
        return
    }
    Column {
        pages.forEach { page ->
            Text(page.id.replaceFirstChar { it.titlecase() }, color = theme.textColor,
                modifier = Modifier.clickable { LocalNavController.wrapper.navigate(page.route) }.padding(12.dp))
        }
        if (hasReConfigModules) ReConfigModuleSearchResults(normalized)
    }
}
