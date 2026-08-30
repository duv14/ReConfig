/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.polyfrost.oneconfig.internal.reconfig.modules.CustomCrosshair;
import org.polyfrost.oneconfig.internal.reconfig.modules.WaypointRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public class Mixin_ReConfigCrosshair {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$crosshair(GuiGraphics graphics, DeltaTracker ticks, CallbackInfo ci) {
        if (CustomCrosshair.active()) ci.cancel();
    }
    // Separate from renderCrosshair: third-person and crosshair-hiding mods can skip
    // the vanilla function entirely. Only the base image is suppressed above.
    @Inject(method = "render", at = @At("TAIL"), require = 1)
    private void reconfig$overlay(GuiGraphics graphics, DeltaTracker ticks, CallbackInfo ci) {
        CustomCrosshair.render(graphics);
        WaypointRenderer.render(graphics, ticks);
    }
}
