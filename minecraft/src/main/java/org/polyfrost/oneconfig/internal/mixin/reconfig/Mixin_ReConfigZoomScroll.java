/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import net.minecraft.client.MouseHandler;
import org.polyfrost.oneconfig.internal.reconfig.modules.ZoomController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public class Mixin_ReConfigZoomScroll {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomController.scroll(vertical)) ci.cancel();
    }
}
