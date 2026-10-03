/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.gui.components.ChatComponent;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class Mixin_ReConfigStreamerChat {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void reconfig$hideChat(CallbackInfo ci) {
        if (ModuleAccess.enabled("streamer_mode") && Boolean.parseBoolean(ModuleAccess.choice("streamer_mode", "hide_chat", "true"))) ci.cancel();
    }
}
