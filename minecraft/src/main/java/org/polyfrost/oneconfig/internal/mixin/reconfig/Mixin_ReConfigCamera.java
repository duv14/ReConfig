/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Camera;
import org.polyfrost.oneconfig.internal.reconfig.modules.FreeLookController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class Mixin_ReConfigCamera {
    // Change orientation BEFORE vanilla performs its third-person obstruction raycast.
    @ModifyArgs(method = "setup", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), require = 1)
    private void reconfig$rotation(Args args) {
        if (FreeLookController.STATE.active()) {
            args.set(0, FreeLookController.STATE.yaw());
            args.set(1, FreeLookController.STATE.pitch());
        }
    }
}
