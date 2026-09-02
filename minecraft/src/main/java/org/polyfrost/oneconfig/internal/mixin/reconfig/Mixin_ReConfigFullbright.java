/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class Mixin_ReConfigFullbright {
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object reconfig$gamma(OptionInstance<?> option) {
        if (option == Minecraft.getInstance().options.gamma() && ModuleAccess.enabled("fullbright")) return 16.0;
        return option.get();
    }
}
