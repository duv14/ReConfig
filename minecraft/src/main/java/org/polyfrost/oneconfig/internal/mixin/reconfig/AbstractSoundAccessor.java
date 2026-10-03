/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read per-event levels without resolving or sampling a sound twice. */
@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundAccessor {
    @Accessor("volume") float reconfig$rawVolume();
    @Accessor("pitch") float reconfig$rawPitch();
}
