package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client visibility only; the server's teams, objectives and scores are untouched. */
@Mixin(Gui.class)
public class Mixin_ReConfigScoreboard {
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$sidebar(GuiGraphics graphics, DeltaTracker ticks, CallbackInfo ci) {
        if (ModuleAccess.enabled("hide_scoreboard")) ci.cancel();
    }
}
