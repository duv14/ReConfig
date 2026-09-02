/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class Mixin_ReConfigHurtCam {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void reconfig$disableHurtCam(CallbackInfo ci) {
        if (ModuleAccess.enabled("hurt_cam")) ci.cancel();
    }
}
