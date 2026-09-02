/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.polyfrost.oneconfig.internal.reconfig.modules.ProjectilePrediction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class)
public class Mixin_ReConfigProjectileUse {
    @Unique private ItemStack reconfig$prediction = ItemStack.EMPTY;
    @Inject(method = "useItem", at = @At("HEAD"), require = 1)
    private void reconfig$prepare(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        reconfig$prediction = ProjectilePrediction.prepare(player, hand);
    }
    @Inject(method = "useItem", at = @At("RETURN"), require = 1)
    private void reconfig$spawn(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction()) ProjectilePrediction.spawn(reconfig$prediction);
        reconfig$prediction = ItemStack.EMPTY;
    }
}
