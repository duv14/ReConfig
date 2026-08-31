/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.function.BooleanSupplier;

@Mixin(ToggleKeyMapping.class)
public abstract class Mixin_ReConfigToggleOptions {
    @Redirect(method = "setDown", at = @At(value = "INVOKE", target = "Ljava/util/function/BooleanSupplier;getAsBoolean()Z"))
    private boolean reconfig$togglePolicy(BooleanSupplier vanilla) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            if ((Object) this == mc.options.keySprint && ModuleAccess.enabled("toggle_sprint")
                || (Object) this == mc.options.keyShift && ModuleAccess.enabled("toggle_sneak")) return true;
        }
        // Scope the override to key input; option widgets and serialization see vanilla values.
        return vanilla.getAsBoolean();
    }
}
