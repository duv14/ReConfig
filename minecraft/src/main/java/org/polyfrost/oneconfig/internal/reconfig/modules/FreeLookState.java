/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

/** Camera-only orientation. Never writes the player's rotation. */
public final class FreeLookState {
    private boolean active;
    private float yaw, pitch;
    public void begin(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; active = true; }
    public void end() { active = false; }
    public boolean active() { return active; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public void turn(double dx, double dy) {
        if (!active) return;
        yaw += (float) (dx * .15);
        pitch = EffectMath.clamp(pitch + (float) (dy * .15), -90, 90);
    }
}
