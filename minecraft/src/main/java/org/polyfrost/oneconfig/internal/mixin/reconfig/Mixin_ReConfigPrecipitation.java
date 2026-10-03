/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherEffectRenderer.class)
public class Mixin_ReConfigPrecipitation {
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$precipitation(Level level, BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (!ModuleAccess.enabled("weather")) return;
        switch (ModuleAccess.choice("weather", "weather", "Vanilla")) {
            case "Snow" -> cir.setReturnValue(Biome.Precipitation.SNOW);
            case "Rain" -> cir.setReturnValue(Biome.Precipitation.RAIN);
            case "Clear" -> cir.setReturnValue(Biome.Precipitation.NONE);
        }
    }
}
