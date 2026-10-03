/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package com.terraformersmc.modmenu.api;

import org.jetbrains.annotations.Nullable;

public interface UpdateChecker {
    @Nullable
    UpdateInfo checkForUpdates();
}
