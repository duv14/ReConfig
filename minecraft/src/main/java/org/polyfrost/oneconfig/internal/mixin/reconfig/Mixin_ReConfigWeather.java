/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class Mixin_ReConfigWeather {
    private boolean reconfig$client() { return (Object) this instanceof ClientLevel && ModuleAccess.enabled("weather"); }

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void reconfig$time(CallbackInfoReturnable<Long> cir) {
        if (!reconfig$client()) return;
        String choice = ModuleAccess.choice("weather", "time", "Vanilla");
        long time = switch (choice) { case "Sunrise" -> 23000L; case "Day" -> 6000L; case "Sunset" -> 12000L; case "Night" -> 14000L; case "Midnight" -> 18000L; default -> -1L; };
        if (time >= 0L) cir.setReturnValue(time);
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void reconfig$rain(CallbackInfoReturnable<Float> cir) {
        if (!reconfig$client()) return;
        String choice = ModuleAccess.choice("weather", "weather", "Vanilla");
        if (choice.equals("Clear")) cir.setReturnValue(0f);
        else if (choice.equals("Rain") || choice.equals("Snow")) cir.setReturnValue(1f);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void reconfig$thunder(CallbackInfoReturnable<Float> cir) {
        if (reconfig$client() && !ModuleAccess.choice("weather", "weather", "Vanilla").equals("Vanilla")) cir.setReturnValue(0f);
    }
}
