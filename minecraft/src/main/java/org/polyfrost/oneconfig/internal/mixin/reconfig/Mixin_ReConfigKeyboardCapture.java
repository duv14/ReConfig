package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.internal.reconfig.BindingInput;
import org.polyfrost.oneconfig.internal.reconfig.KeyCapture;
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen;
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recording owns input before fullscreen/debug/screenshot and other vanilla shortcuts. */
@Mixin(value = KeyboardHandler.class, priority = 2000)
public class Mixin_ReConfigKeyboardCapture {
    @Unique private final BindingInput.CaptureGate reconfig$recorded = new BindingInput.CaptureGate();
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$capture(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window != Platform.compatibility().windowHandle()) return;
        if (KeyCapture.consume(event.key(), action)) { ci.cancel(); return; }
        Object screen = Platform.screen().current();
        boolean recording = KeybindRecordingBus.isRecording() && screen instanceof ComposeScreen;
        if (!reconfig$recorded.consume(event.key(), action, recording)) return;
        if (recording) {
            if (action == 1) ((ComposeScreen) screen).keyPressed(event);
            else if (action == 0) ((ComposeScreen) screen).keyReleased(event);
        }
        ci.cancel();
    }
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$text(long window, CharacterEvent event, CallbackInfo ci) {
        if (window == Platform.compatibility().windowHandle() &&
            (KeyCapture.getBlocksText() || KeybindRecordingBus.getBlocksText() || reconfig$recorded.blocksText())) ci.cancel();
    }
}
