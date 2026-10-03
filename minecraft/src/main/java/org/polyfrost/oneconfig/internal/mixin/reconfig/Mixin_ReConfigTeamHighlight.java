/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's Team Highlight source; see ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.polyfrost.oneconfig.internal.reconfig.CombatRepository;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.combat.HighlightConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class Mixin_ReConfigTeamHighlight {
    @Unique private HighlightConfig.TrackedPlayer reconfig$tracked() {
        if (!ModuleAccess.enabled("team_highlight") || !((Object)this instanceof Player player)) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || player == mc.player || player.level() != mc.level) return null;
        return CombatRepository.getHighlights().find(player.getGameProfile().name()).orElse(null);
    }
    @Unique private boolean reconfig$visible(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player.isInvisible() || mc.level == null || mc.player == null) return false;
        return mc.level.clip(new ClipContext(mc.gameRenderer.getMainCamera().position(), player.getEyePosition(),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, mc.player)).getType() == HitResult.Type.MISS;
    }
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void reconfig$glow(CallbackInfoReturnable<Boolean> cir) {
        if (reconfig$tracked() != null) cir.setReturnValue(reconfig$visible((Player)(Object)this));
    }
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void reconfig$color(CallbackInfoReturnable<Integer> cir) {
        HighlightConfig.TrackedPlayer tracked = reconfig$tracked();
        if (tracked != null && reconfig$visible((Player)(Object)this))
            cir.setReturnValue(CombatRepository.getHighlights().color(tracked.role));
    }
}
