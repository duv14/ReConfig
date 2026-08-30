/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.polyfrost.oneconfig.internal.reconfig.modules.FreeLookController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class Mixin_ReConfigLookInput {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$turn(double dx, double dy, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player && FreeLookController.STATE.active()) {
            FreeLookController.STATE.turn(dx, dy);
            ci.cancel();
        }
    }
}
