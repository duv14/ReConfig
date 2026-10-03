/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public abstract class Mixin_ReConfigFullbright {
    private static final double FULL_BRIGHT = 16.0;

    @WrapOperation(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"),
        require = 0
    )
    private Object reconfig$gammaValue(OptionInstance<?> option, Operation<Object> original) {
        Object value = original.call(option);
        if (option == Minecraft.getInstance().options.gamma() && ModuleAccess.enabled("fullbright")) return FULL_BRIGHT;
        return value;
    }
}
