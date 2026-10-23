/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.SoundPlayedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SoundManager.class)
public class Mixin_SoundPlayedEvent {
    @org.spongepowered.asm.mixin.Unique
    private final java.util.Map<String, Long> reconfig$spearSounds = new java.util.HashMap<>();
    @org.spongepowered.asm.mixin.injection.Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void reconfig$spearDuplicates(SoundInstance sound,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.client.sounds.SoundEngine.PlayResult> ci) {
        if (!org.polyfrost.oneconfig.internal.reconfig.ModuleAccess.enabled("spear_sound_fix")) {
            reconfig$spearSounds.clear(); return;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (sound == null || sound.isLooping() || mc.player == null || mc.level == null) {
            reconfig$spearSounds.clear(); return;
        }
        String id = sound.getIdentifier().toString();
        if (!id.startsWith("minecraft:item.spear.")) return;
        double dx = sound.getX()-mc.player.getX(), dy = sound.getY()-mc.player.getY(), dz = sound.getZ()-mc.player.getZ();
        if (!sound.isRelative() && dx*dx+dy*dy+dz*dz > 4) return;
        long now = System.nanoTime();
        long cooldown = (long)(org.polyfrost.oneconfig.internal.reconfig.ModuleAccess.number("spear_sound_fix", "cooldown_ms", 180)*1_000_000L);
        Long last = reconfig$spearSounds.get(id);
        if (last != null && now-last < cooldown) {
            ci.setReturnValue(net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED);
        } else {
            reconfig$spearSounds.put(id, now);
        }
    }

    @ModifyVariable(
            method = "play",
            at = @At("HEAD"),
            argsOnly = true
    )
    private SoundInstance onPlaySoundCallback(SoundInstance value) {
        if (value != null && value.getSound() != null) {
            String name = value.getSound().getLocation().toString();
            SoundPlayedEvent event = new SoundPlayedEvent(name, null, value);
            EventManager.INSTANCE.post(event);

            Object modified = event.getSound();
            if (modified instanceof SoundInstance) {
                return org.polyfrost.oneconfig.internal.reconfig.modules.BetterSounds.replace((SoundInstance) modified);
            }
        }

        return org.polyfrost.oneconfig.internal.reconfig.modules.BetterSounds.replace(value);
    }

}
