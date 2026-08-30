/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.modules.EffectMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public class Mixin_ReConfigFog {
    // Only distance fog is changed. Lava, water and status-effect fog remain intact.
    @ModifyVariable(method = "updateBuffer", at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 1)
    private float reconfig$start(float original) {
        return ModuleAccess.enabled("fog") ? 0f : original;
    }
    @ModifyVariable(method = "updateBuffer", at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 1)
    private float reconfig$end(float original) {
        return ModuleAccess.enabled("fog") ? EffectMath.fogEnd(ModuleAccess.number("fog", "fog_distance", 96), ModuleAccess.number("fog", "fog_opacity", 65)) : original;
    }
}
