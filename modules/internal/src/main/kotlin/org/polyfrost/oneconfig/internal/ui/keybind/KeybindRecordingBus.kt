/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.ui.keybind

object KeybindRecordingBus {
    private val mouseGate = org.polyfrost.oneconfig.internal.reconfig.BindingInput.CaptureGate()
    @Volatile private var mouseHandler: ((Int) -> Unit)? = null
    fun setMouseHandler(handler: (Int) -> Unit) { mouseHandler = handler }
    fun clearMouseHandler(handler: (Int) -> Unit) { if (mouseHandler === handler) mouseHandler = null }
    @JvmStatic fun consumeMouse(button: Int, action: Int): Boolean {
        val handler = mouseHandler
        val recording = isRecording && handler != null
        if (!mouseGate.consume(org.polyfrost.oneconfig.internal.reconfig.BindingInput.mouseCode(button), action, recording)) return false
        if (recording && action == 1 && button in 0..7) {
            mouseHandler = null
            escapeHandler = null
            handler?.invoke(button)
        }
        return true
    }
    @JvmStatic val blocksText get() = isRecording || mouseGate.blocksText()
    @Volatile
    private var escapeHandler: (() -> Unit)? = null

    fun setEscapeHandler(handler: () -> Unit) {
        escapeHandler = handler
    }

    fun clearEscapeHandler(handler: () -> Unit) {
        if (escapeHandler === handler) escapeHandler = null
    }

    @JvmStatic
    val isRecording get() = escapeHandler != null

    @JvmStatic
    fun consumeEscape(): Boolean {
        val handler = escapeHandler ?: return false
        handler()
        return true
    }
}
