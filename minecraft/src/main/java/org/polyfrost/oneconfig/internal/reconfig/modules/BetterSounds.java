/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.mixin.reconfig.AbstractSoundAccessor;

public final class BetterSounds {
    private BetterSounds() {}

    public static SoundInstance replace(SoundInstance sound) {
        if (sound == null || !ModuleAccess.enabled("better_sounds") || sound.isLooping()) {
            return sound;
        }

        String id = sound.getIdentifier().toString();
        String category = switch (id) {
            case "minecraft:entity.generic.eat", "minecraft:entity.player.burp" -> "eating";
            case "minecraft:entity.player.attack.strong", "minecraft:entity.player.attack.crit", "minecraft:entity.player.attack.knockback", "minecraft:entity.player.attack.sweep", "minecraft:entity.player.attack.weak" -> "hits";
            case "minecraft:entity.wind_charge.throw", "minecraft:entity.wind_charge.wind_burst" -> "wind_charges";
            case "minecraft:item.mace.smash_air", "minecraft:item.mace.smash_ground", "minecraft:item.mace.smash_ground_heavy" -> "mace_hits";
            default -> null;
        };
        if (category == null || !Boolean.parseBoolean(ModuleAccess.choice("better_sounds", category, "true"))) {
            return sound;
        }

        // Only AbstractSoundInstance exposes safe, unresolved event levels.
        if (!(sound instanceof AbstractSoundAccessor)) {
            return sound;
        }
        return new Replacement(sound, category);
    }

    private static final class Replacement extends AbstractSoundInstance {
        Replacement(SoundInstance original, String category) {
            super(Identifier.fromNamespaceAndPath("reconfig", "gameplay." + category), original.getSource(), SoundInstance.createUnseededRandom());
            // At play(HEAD), getVolume()/getPitch() would dereference an unresolved Sound.
            AbstractSoundAccessor raw = (AbstractSoundAccessor) original;
            volume = raw.reconfig$rawVolume();
            pitch = raw.reconfig$rawPitch();
            x = original.getX();
            y = original.getY();
            z = original.getZ();
            attenuation = original.getAttenuation();
            relative = original.isRelative();
            delay = original.getDelay();
            looping = false;
        }
    }
}
