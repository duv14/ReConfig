/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's supplied Hitbox Categories source. See ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.reconfig.combat;

public final class DistanceLineWidth {
    private static final double FULL_WIDTH_DISTANCE = 6.0D;
    private static final float MIN_FACTOR = 0.20F;

    private DistanceLineWidth() {}

    public static float forDistance(float configuredWidth, double distance) {
        if (!Float.isFinite(configuredWidth) || configuredWidth <= 0.0F) {
            throw new IllegalArgumentException("configuredWidth must be positive and finite");
        }
        if (!Double.isFinite(distance) || distance <= FULL_WIDTH_DISTANCE) {
            return configuredWidth;
        }

        float factor = (float) (FULL_WIDTH_DISTANCE / distance);
        factor = Math.max(MIN_FACTOR, Math.min(1.0F, factor));
        return configuredWidth * factor;
    }
}
