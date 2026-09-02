/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Camera;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.modules.ZoomController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class Mixin_ReConfigFov {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void reconfig$fov(Camera camera, float partialTick, boolean dynamic, CallbackInfoReturnable<Float> cir) {
        // `dynamic == false` is the first-person hand projection. Leave it at
        // vanilla scale so both hands remain correctly framed at wide world FOVs.
        if (!dynamic) return;
        float base = ModuleAccess.enabled("fov") ? ModuleAccess.number("fov", "fov", 120f) : cir.getReturnValue();
        if (ModuleAccess.enabled("zoom")) base = ZoomController.apply(base);
        cir.setReturnValue(base);
    }
}
