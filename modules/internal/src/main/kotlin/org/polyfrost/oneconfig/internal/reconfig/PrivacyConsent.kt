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
    private const val VERSION = 2
    const val POLICY_URL = "https://duv14.com/policy"
    const val SUMMARY = "Local settings stay on this computer. Social features send your Minecraft identity, authentication proof and a hashed ReConfig token to our Cloudflare service, and store friendships, messages, presence and invitations. Messages are not end-to-end encrypted. Public profile publication requires a separate opt-in. Media metadata stays local. We do not sell personal data or use behavioural advertising. Contact therealduv14@gmail.com for access or deletion requests."
    private val preferences = Preferences.userRoot().node("dev/duv14/reconfig")
    var accepted by mutableStateOf(preferences.getInt("privacyPolicyVersion", 0) >= VERSION)
        private set

    fun accept() {
        preferences.putInt("privacyPolicyVersion", VERSION)
        runCatching { preferences.flush() }
        accepted = true
    }

    fun readMore() {
        runCatching { DesktopHelper.browse(URI.create(POLICY_URL)) }
    }
}
