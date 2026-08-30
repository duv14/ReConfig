/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.sound

interface UiSoundService {
    fun play(event: UiSoundEvent, theme: UiSoundTheme, volume: Float)

    fun startAmbience(theme: UiSoundTheme, volume: Float)

    fun stopAmbience()
}
