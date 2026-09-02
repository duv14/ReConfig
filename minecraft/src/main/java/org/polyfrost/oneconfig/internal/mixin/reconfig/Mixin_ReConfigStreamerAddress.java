/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.modules.AddressRedactor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
public abstract class Mixin_ReConfigStreamerAddress {
    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence reconfig$maskStyledAddress(FormattedCharSequence sequence) {
        if (sequence == null || !ModuleAccess.enabled("streamer_mode")) return sequence;
        StringBuilder text = new StringBuilder();
        sequence.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
        String original = text.toString();
        String masked = reconfig$maskAddress(original);
        return original.equals(masked) ? sequence : FormattedCharSequence.forward(masked, Style.EMPTY);
    }
    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        at = @At("HEAD"), argsOnly = true)
    private String reconfig$maskAddress(String text) {
        if (!ModuleAccess.enabled("streamer_mode") || !Boolean.parseBoolean(ModuleAccess.choice("streamer_mode", "hide_addresses", "true"))) return text;
        var server = Minecraft.getInstance().getCurrentServer();
        return server == null ? text : AddressRedactor.mask(text, server.ip);
    }
}
