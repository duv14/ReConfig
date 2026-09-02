/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import java.util.regex.Pattern;

public final class AddressRedactor {
    private AddressRedactor() {}
    public static String mask(String text, String address) {
        if (text == null || address == null || address.isBlank()) return text;
        String result = Pattern.compile(Pattern.quote(address), Pattern.CASE_INSENSITIVE).matcher(text).replaceAll("[hidden server]");
        String host = address;
        if (address.startsWith("[") && address.contains("]")) host = address.substring(0, address.indexOf(']') + 1);
        else if (address.indexOf(':') == address.lastIndexOf(':') && address.contains(":")) host = address.substring(0, address.indexOf(':'));
        return host.isBlank() ? result : Pattern.compile(Pattern.quote(host), Pattern.CASE_INSENSITIVE).matcher(result).replaceAll("[hidden server]");
    }
}
