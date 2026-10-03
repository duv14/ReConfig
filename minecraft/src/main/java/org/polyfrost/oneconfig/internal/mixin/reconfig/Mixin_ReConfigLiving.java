/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.modules.HitFlash;
import org.polyfrost.oneconfig.internal.reconfig.modules.HitTintState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class Mixin_ReConfigLiving {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"), require = 1)
    private void reconfig$state(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
        ((HitTintState) state).reconfig$setTint(HitFlash.tint(entity));
        if (entity == Minecraft.getInstance().player && ModuleAccess.enabled("nick_hider")) {
            state.nameTag = Boolean.parseBoolean(ModuleAccess.choice("nick_hider", "hide_nickname", "true"))
                ? null : Component.literal(ModuleAccess.choice("nick_hider", "nickname", "Player"));
        }
    }
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void reconfig$name(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (entity == Minecraft.getInstance().player && ModuleAccess.enabled("nick_hider")) {
            cir.setReturnValue(!Boolean.parseBoolean(ModuleAccess.choice("nick_hider", "hide_nickname", "true")));
        }
    }
    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true, require = 1)
    private void reconfig$tint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        int tint = ((HitTintState) state).reconfig$getTint();
        if (tint != 0) cir.setReturnValue((cir.getReturnValueI() & 0xFF000000) | (tint & 0xFFFFFF));
    }
    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true, require = 1)
    private static void reconfig$overlay(LivingEntityRenderState state, float white, CallbackInfoReturnable<Integer> cir) {
        if (((HitTintState) state).reconfig$getTint() != 0) cir.setReturnValue(OverlayTexture.NO_OVERLAY);
    }
}
