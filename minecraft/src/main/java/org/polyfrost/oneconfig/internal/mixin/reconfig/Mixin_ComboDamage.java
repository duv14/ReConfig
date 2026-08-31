/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.polyfrost.oneconfig.internal.reconfig.modules.ModuleHuds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class Mixin_ComboDamage {
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void reconfig$confirmedDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        ModuleHuds.damage(packet);
    }
}
