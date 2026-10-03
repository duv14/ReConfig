/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;

public final class HitFlash {
    private static final Map<Entity, Long> hits = new WeakHashMap<>();
    private HitFlash() {}
    public static void attack(Entity entity) {
        if (ModuleAccess.enabled("hit_color")) hits.put(entity, System.nanoTime());
    }
    public static int tint(Entity entity) {
        if (!ModuleAccess.enabled("hit_color")) { hits.clear(); return 0; }
        Long start = hits.get(entity);
        if (start == null) return 0;
        double elapsed = (System.nanoTime() - start) / 1_000_000_000.0;
        if (elapsed > ModuleAccess.number("hit_color", "flash_duration", 1)) { hits.remove(entity); return 0; }
        return EffectMath.color(ModuleAccess.choice("hit_color", "hit_color", "#66AFFF"), 0xFF66AFFF);
    }
}
