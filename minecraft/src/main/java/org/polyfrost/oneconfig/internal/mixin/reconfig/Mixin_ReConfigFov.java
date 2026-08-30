/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class Mixin_ReConfigFov {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void reconfig$fov(CallbackInfoReturnable<Float> cir) {
        if (ModuleAccess.enabled("fov")) cir.setReturnValue(ModuleAccess.number("fov", "fov", 120f));
    }
}
