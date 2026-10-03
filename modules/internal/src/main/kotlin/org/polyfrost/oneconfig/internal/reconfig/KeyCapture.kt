/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared keyboard-listening state used by the controls-style keybind button. */
object KeyCapture {
    private const val GLFW_KEY_ESCAPE = 256
    private const val GLFW_KEY_BACKSPACE = 259
    private const val GLFW_KEY_DELETE = 261
    var moduleId by mutableStateOf<String?>(null)
        private set
    var settingId by mutableStateOf<String?>(null)
        private set

    fun begin(moduleId: String, settingId: String) { this.moduleId = moduleId; this.settingId = settingId }
    fun cancel() { moduleId = null; settingId = null }

    /** Returns true when the event was consumed by key capture. */
    fun consume(key: Int, state: Int): Boolean {
        val id = moduleId ?: return false
        val settingId = settingId ?: return false
        if (state != 1) return true
        when (key) {
            GLFW_KEY_ESCAPE -> cancel()
            GLFW_KEY_BACKSPACE, GLFW_KEY_DELETE -> { ModuleCatalog.byId(id)?.setKey(settingId,0); cancel() }
            else -> { ModuleCatalog.byId(id)?.setKey(settingId,key); cancel() }
        }
        return true
    }

    fun label(key: Int): String = when (key) {
        0 -> "Unbound"
        32 -> "Space"
        256 -> "Escape"
        257 -> "Enter"
        258 -> "Tab"
        259 -> "Backspace"
        261 -> "Delete"
        262 -> "Right Arrow"
        263 -> "Left Arrow"
        264 -> "Down Arrow"
        265 -> "Up Arrow"
        340 -> "Left Shift"
        341 -> "Left Ctrl"
        342 -> "Left Alt"
        344 -> "Right Shift"
        345 -> "Right Ctrl"
        346 -> "Right Alt"
        in 290..314 -> "F${key - 289}"
        in 48..57, in 65..90 -> key.toChar().toString()
        else -> "Key $key"
    }
}
