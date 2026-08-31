/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's supplied Hitbox Categories source. See ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.reconfig.combat;

import java.util.Locale;
import java.util.regex.Pattern;

public final class HexColor {
    private static final Pattern RGB = Pattern.compile("#[0-9A-Fa-f]{6}");

    private HexColor() {}

    public static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }
        String trimmed = value.trim();
        if (!RGB.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Expected color in #RRGGBB format: " + value);
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static int parseArgb(String value) {
        String normalized = normalize(value);
        int rgb = Integer.parseInt(normalized.substring(1), 16);
        return 0xFF000000 | rgb;
    }

    public static String fromArgb(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }
}
