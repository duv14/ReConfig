/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.polyfrost.oneconfig.internal.reconfig.modules.HitTintState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(LivingEntityRenderState.class)
public class Mixin_ReConfigHitState implements HitTintState {
    @Unique private int reconfig$tint;
    public int reconfig$getTint() { return reconfig$tint; }
    public void reconfig$setTint(int tint) { reconfig$tint = tint; }
}
