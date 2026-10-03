/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.sound

object UiSoundDucking {
    private const val DUCK = 0.0f
    const val FADE_MS = 400f

    @Volatile
    private var active = false

    @Volatile
    private var changedAt = 0L

    fun setActive(value: Boolean) {
        if (value == active) return
        active = value
        changedAt = System.currentTimeMillis()
    }

    @JvmStatic
    fun musicVolumeMultiplier(): Float = 1f

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
}
