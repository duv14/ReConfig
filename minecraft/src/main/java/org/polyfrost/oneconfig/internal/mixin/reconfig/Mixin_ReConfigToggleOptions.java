/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.ToggleKeyMapping;

@Mixin(ToggleKeyMapping.class)
public abstract class Mixin_ReConfigToggleOptions {
    // Kept as a stable mixin target for existing installations. ReConfig 3.1.4
    // handles movement toggles explicitly in ReConfigModuleRuntime.
}
