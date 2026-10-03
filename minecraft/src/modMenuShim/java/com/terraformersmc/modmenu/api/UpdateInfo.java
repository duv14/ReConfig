/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package com.terraformersmc.modmenu.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface UpdateInfo {
    boolean isUpdateAvailable();

    @Nullable
    default Component getUpdateMessage() {
        return null;
    }

    String getDownloadLink();

    UpdateChannel getUpdateChannel();
}
