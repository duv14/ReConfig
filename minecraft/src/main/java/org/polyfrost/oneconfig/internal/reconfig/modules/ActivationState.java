/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

/** A held key never becomes a fresh press when a menu closes. */
public final class ActivationState {
    private boolean down;
    private boolean latched;
    public boolean update(boolean enabled, boolean pressed, boolean hold, boolean blocked) {
        boolean edge = pressed && !down;
        down = pressed;
        if (!enabled || blocked) { latched = false; return false; }
        if (hold) { latched = false; return pressed; }
        if (edge) latched = !latched;
        return latched;
    }
}
