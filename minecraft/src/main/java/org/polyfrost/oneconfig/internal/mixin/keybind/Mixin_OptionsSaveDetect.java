/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.Options;
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class Mixin_OptionsSaveDetect {
    @Inject(method = "save", at = @At("RETURN"))
    private void oneconfig$captureSavedControls(CallbackInfo ci) {
        MinecraftKeybindProfiles.onOptionsSaved();
    }
}
