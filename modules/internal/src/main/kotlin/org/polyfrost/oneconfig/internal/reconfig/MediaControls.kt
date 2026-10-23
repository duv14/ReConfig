/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.internal.reconfig

/** UI boundary for optional, local operating-system transport controls. */
object MediaControls {
    data class State(val title: String = "No supported music session", val playing: Boolean = false,
        val playPause: Boolean = false, val previous: Boolean = false, val next: Boolean = false)
    @Volatile var snapshot: (() -> State)? = null
    @Volatile var command: ((String) -> Boolean)? = null
}
