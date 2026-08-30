/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.oneconfig.internal.reconfig.modules.MotionBlur;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class Mixin_ReConfigMotionBlur {
    @Inject(method = "renderLevel", at = @At("TAIL"), require = 1)
    private void reconfig$blur(DeltaTracker ticks, CallbackInfo ci) { MotionBlur.draw(); }
    @Inject(method = "render", at = @At("HEAD"), require = 1)
    private void reconfig$clearWorld(DeltaTracker ticks, boolean renderLevel, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) MotionBlur.reset();
    }
    @Inject(method = "close", at = @At("HEAD"), require = 1)
    private void reconfig$close(CallbackInfo ci) { MotionBlur.reset(); }
}
