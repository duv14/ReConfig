/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

/** Correlates a local melee attempt with server damage; never treats aiming as a hit. */
public final class ConfirmedReach {
    private int target = -1;
    private long attemptedAt, confirmedAt;
    private double pending, distance = Double.NaN;
    public void attempt(int entity, double meters, long now) {
        target = -1;
        if (!Double.isFinite(meters) || meters < 0 || meters > 64) return;
        target = entity; pending = meters; attemptedAt = now;
    }
    public void confirm(int victim, int attacker, int localPlayer, long now) {
        if (victim != target || attacker != localPlayer || now < attemptedAt || now - attemptedAt > 2000) return;
        distance = pending; confirmedAt = now; target = -1;
    }
    public boolean visible(long now, long duration) {
        return Double.isFinite(distance) && now >= confirmedAt && now - confirmedAt < duration;
    }
    public double distance() { return distance; }
    public void clear() { target = -1; distance = Double.NaN; }
}
