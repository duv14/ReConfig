/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.particle.SingleQuadParticle;
import org.polyfrost.oneconfig.internal.reconfig.modules.ParticleRenderSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuadParticleRenderState.class)
public class Mixin_ReConfigParticles {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$invisible(CallbackInfo ci) {
        if (ParticleRenderSettings.state().invisible()) ci.cancel();
    }
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private SingleQuadParticle.Layer reconfig$layer(SingleQuadParticle.Layer layer) {
        if (ParticleRenderSettings.state().fading() && layer == SingleQuadParticle.Layer.OPAQUE)
            return SingleQuadParticle.Layer.TRANSLUCENT;
        return layer;
    }
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 7, require = 1)
    private float reconfig$size(float size) {
        return ParticleRenderSettings.state().scale(size);
    }
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private int reconfig$alpha(int color) {
        return ParticleRenderSettings.state().tint(color);
    }
}
