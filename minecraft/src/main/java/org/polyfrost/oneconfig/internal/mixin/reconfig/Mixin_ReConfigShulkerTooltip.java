/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.polyfrost.oneconfig.internal.reconfig.modules.ShulkerPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public class Mixin_ReConfigShulkerTooltip {
    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true, require = 1)
    private void reconfig$contents(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        ShulkerPreview preview = ShulkerPreview.create((ItemStack)(Object)this);
        if (preview != null) cir.setReturnValue(Optional.of(preview));
    }
}
