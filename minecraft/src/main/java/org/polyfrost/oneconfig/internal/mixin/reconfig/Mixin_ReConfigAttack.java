/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.polyfrost.oneconfig.internal.reconfig.modules.HitFlash;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MultiPlayerGameMode.class)
public class Mixin_ReConfigAttack {
    @Inject(method = "attack", at = @At("TAIL"), require = 1)
    private void reconfig$attack(Player player, Entity target, CallbackInfo ci) {
        HitFlash.attack(target);
        if (target instanceof Player && org.polyfrost.oneconfig.internal.reconfig.ModuleAccess.enabled("hitbox"))
            org.polyfrost.oneconfig.internal.reconfig.CombatRepository.getFlashes().record(target.getUUID(), System.currentTimeMillis());
    }
}
