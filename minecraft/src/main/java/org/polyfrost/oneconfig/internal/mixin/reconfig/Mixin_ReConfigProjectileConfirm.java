/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.polyfrost.oneconfig.internal.reconfig.modules.ProjectilePrediction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public class Mixin_ReConfigProjectileConfirm {
    @Inject(method = "addEntity", at = @At("TAIL"), require = 1)
    private void reconfig$confirm(Entity entity, CallbackInfo ci) { ProjectilePrediction.confirm(entity); }
}
