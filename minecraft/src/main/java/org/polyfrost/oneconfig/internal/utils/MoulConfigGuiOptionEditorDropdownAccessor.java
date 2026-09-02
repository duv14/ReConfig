/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.utils;

//? moul_compat {
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;

@MoulConfig
public interface MoulConfigGuiOptionEditorDropdownAccessor {

    String[] oneconfig$values();

    boolean oneconfig$useOrdinal();

    Enum<?>[] oneconfig$constants();

}
//? }
