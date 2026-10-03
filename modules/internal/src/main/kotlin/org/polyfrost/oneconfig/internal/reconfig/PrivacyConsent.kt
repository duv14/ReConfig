/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URI
import java.util.prefs.Preferences
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper

object PrivacyConsent {
    private const val VERSION = 1
    private val preferences = Preferences.userRoot().node("dev/duv14/reconfig")
    var accepted by mutableStateOf(preferences.getInt("privacyPolicyVersion", 0) >= VERSION)
        private set

    fun accept() {
        preferences.putInt("privacyPolicyVersion", VERSION)
        runCatching { preferences.flush() }
        accepted = true
    }

    fun readMore() {
        DesktopHelper.browse(URI.create("https://github.com/duv14/ReConfig/blob/main/PRIVACY.md"))
    }
}
