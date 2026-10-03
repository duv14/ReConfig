/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's supplied Hitbox Categories source. See ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.reconfig.combat;

public final class HitboxColorResolver {
    private HitboxColorResolver() {}

    public static int resolve(int baseColor, int rangeColor, int hurtColor, boolean aimedAndInRange, boolean hurtFlashing) {
        if (hurtFlashing) return hurtColor;
        if (aimedAndInRange) return rangeColor;
        return baseColor;
    }
}
