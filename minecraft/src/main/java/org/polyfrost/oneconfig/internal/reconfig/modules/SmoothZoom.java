/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

public final class SmoothZoom {
    private double value = 1;
    public double value() { return value; }
    public void reset() { value = 1; }
    public double step(double target, double seconds, double response) {
        if (!Double.isFinite(target) || !Double.isFinite(seconds) || !Double.isFinite(response)) return value;
        target = Math.max(.025, Math.min(1, target));
        double blend = -Math.expm1(-Math.max(0, seconds) / Math.max(.001, response));
        value += (target - value) * blend;
        return value;
    }
}
