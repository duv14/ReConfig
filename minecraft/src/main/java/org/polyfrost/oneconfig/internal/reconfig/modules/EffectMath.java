/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

public final class EffectMath {
    private EffectMath() {}
    public static float clamp(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
    public static int alpha(int argb, float percent) {
        int alpha = Math.round((argb >>> 24) * clamp(percent, 0, 100) / 100f);
        return (argb & 0xFFFFFF) | (alpha << 24);
    }
    public static float fogEnd(float distance, float opacity) {
        return clamp(distance, 2, 512) / Math.max(.001f, clamp(opacity, 0, 100) / 100f);
    }
    public static double retention(float strength, float responsiveness, double seconds) {
        double base = clamp(strength, 0, 100) / 100.0 * .95 * (1 - clamp(responsiveness, 0, 100) / 200.0);
        return Math.pow(base, Math.max(.001, Math.min(.25, seconds)) * 60);
    }
    public static int color(String text, int fallback) {
        try {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            if (hex.length() != 6) return fallback;
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (RuntimeException ignored) { return fallback; }
    }
}
