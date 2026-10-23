/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenEffectRenderer.class)
public class Mixin_ReConfigTotemAnimation {
    @Shadow private ItemStack itemActivationItem;
    @Shadow private int itemActivationTicks;
    @Unique private int reconfig$duration = 40;
    @Inject(method = "displayItemActivation", at = @At("TAIL"), require = 1)
    private void reconfig$duration(CallbackInfo ci) {
        reconfig$duration = reconfig$active() ? Math.max(4, Math.min(200,
            Math.round(ModuleAccess.number("totem_pop_customizer", "duration_s", 2f) * 20))) : 40;
        if (reconfig$active()) itemActivationTicks = reconfig$duration;
    }
    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), require = 1)
    private void reconfig$restoreVanilla(CallbackInfo ci) {
        if (!reconfig$active() && reconfig$duration != 40) {
            itemActivationTicks = Math.max(0, Math.round(itemActivationTicks * 40f / reconfig$duration));
            reconfig$duration = 40;
        }
    }
    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(intValue = 40), require = 1)
    private int reconfig$elapsedDuration(int original) { return reconfig$active() ? reconfig$duration : original; }
    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(floatValue = 40f), require = 1)
    private float reconfig$normalizedDuration(float original) { return reconfig$active() ? reconfig$duration : original; }

    private boolean reconfig$active() {
        return ModuleAccess.enabled("totem_pop_customizer") && itemActivationItem != null && itemActivationItem.is(Items.TOTEM_OF_UNDYING);
    }
    @WrapOperation(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), require = 1)
    private void reconfig$size(PoseStack pose, float x, float y, float z, Operation<Void> original) {
        float size = reconfig$active() ? Math.max(.1f, Math.min(3f, ModuleAccess.number("totem_pop_customizer", "animation_scale", 1f))) : 1f;
        original.call(pose, x * size, y * size, z * size);
    }
    @WrapOperation(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), require = 1)
    private void reconfig$position(PoseStack pose, float x, float y, float z, Operation<Void> original) {
        if (reconfig$active()) {
            x += ModuleAccess.number("totem_pop_customizer", "animation_x", 0f);
            y += ModuleAccess.number("totem_pop_customizer", "animation_y", 0f);
        }
        original.call(pose, x, y, z);
    }
}
