/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.particle.SingleQuadParticle;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.modules.EffectMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(QuadParticleRenderState.class)
public class Mixin_ReConfigParticles {
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private SingleQuadParticle.Layer reconfig$layer(SingleQuadParticle.Layer layer) {
        if (ModuleAccess.enabled("particles") && ModuleAccess.number("particles", "particle_opacity", 100) < 100 && layer == SingleQuadParticle.Layer.OPAQUE)
            return SingleQuadParticle.Layer.TRANSLUCENT;
        return layer;
    }
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 7, require = 1)
    private float reconfig$size(float size) {
        return ModuleAccess.enabled("particles") ? size * ModuleAccess.number("particles", "particle_size", 100) / 100f : size;
    }
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private int reconfig$alpha(int color) {
        return ModuleAccess.enabled("particles") ? EffectMath.alpha(color, ModuleAccess.number("particles", "particle_opacity", 100)) : color;
    }
}
