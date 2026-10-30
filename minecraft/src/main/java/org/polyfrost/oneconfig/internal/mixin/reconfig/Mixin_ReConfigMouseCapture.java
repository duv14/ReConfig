package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.internal.reconfig.KeyCapture;
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 2000)
public class Mixin_ReConfigMouseCapture {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$capture(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (window == Platform.compatibility().windowHandle() &&
            (KeyCapture.consumeMouse(button.button(), action) || KeybindRecordingBus.consumeMouse(button.button(), action))) ci.cancel();
    }
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$scroll(long window, double x, double y, CallbackInfo ci) {
        if (window == Platform.compatibility().windowHandle() &&
            (KeyCapture.isRecording() || KeybindRecordingBus.isRecording())) ci.cancel();
    }
}
