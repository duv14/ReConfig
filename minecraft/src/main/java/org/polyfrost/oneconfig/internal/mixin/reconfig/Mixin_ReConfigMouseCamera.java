/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.polyfrost.oneconfig.internal.reconfig.modules.FreeLookController;
import org.polyfrost.oneconfig.internal.reconfig.modules.ZoomController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class Mixin_ReConfigMouseCamera {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void reconfig$freeCamera(double delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!FreeLookController.STATE.active() || mc.player == null || mc.screen != null) return;
        double sensitivity = mc.options.sensitivity().get() * .6 + .2;
        double scale = sensitivity * sensitivity * sensitivity * 8 * ZoomController.sensitivityScale();
        double vertical = mc.options.invertMouseY().get() ? -1 : 1;
        double horizontal = mc.options.invertMouseX().get() ? -1 : 1;
        FreeLookController.STATE.turn(accumulatedDX * scale * horizontal, accumulatedDY * scale * vertical);
        accumulatedDX = 0;
        accumulatedDY = 0;
        // Consume mouse movement before vanilla updates the player or its tutorial hooks.
        ci.cancel();
    }
}
