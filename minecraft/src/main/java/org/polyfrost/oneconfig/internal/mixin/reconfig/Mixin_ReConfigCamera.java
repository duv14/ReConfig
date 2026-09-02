/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Camera;
import org.polyfrost.oneconfig.internal.reconfig.modules.FreeLookController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class Mixin_ReConfigCamera {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    // Vanilla first aligns the camera to the player. Override that camera-only
    // rotation immediately afterwards, leaving the player and movement vector alone.
    @Inject(method = "setup", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0,
        shift = At.Shift.AFTER), require = 1)
    private void reconfig$rotation(CallbackInfo ci) {
        if (FreeLookController.STATE.active()) {
            setRotation(FreeLookController.STATE.yaw(), FreeLookController.STATE.pitch());
        }
    }
}
