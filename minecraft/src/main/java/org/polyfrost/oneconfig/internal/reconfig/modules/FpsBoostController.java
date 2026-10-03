/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;

/** Applies reversible vanilla render-distance limits while the module is enabled. */
public final class FpsBoostController {
    private static boolean applied;
    private static int previousChunks;
    private static double previousEntities;

    private FpsBoostController() {}

    public static void update(Minecraft minecraft) {
        boolean enabled = ModuleAccess.enabled("fps_boost");
        if (enabled && !applied) {
            previousChunks = minecraft.options.renderDistance().get();
            previousEntities = minecraft.options.entityDistanceScaling().get();
            applied = true;
        }
        if (enabled) {
            int chunks = Math.round(ModuleAccess.number("fps_boost", "chunk_distance", 8));
            double entities = ModuleAccess.number("fps_boost", "entity_distance", 75) / 100.0;
            if (minecraft.options.renderDistance().get() != chunks) minecraft.options.renderDistance().set(chunks);
            if (Math.abs(minecraft.options.entityDistanceScaling().get() - entities) > 0.001) minecraft.options.entityDistanceScaling().set(entities);
        } else if (applied) {
            minecraft.options.renderDistance().set(previousChunks);
            minecraft.options.entityDistanceScaling().set(previousEntities);
            applied = false;
        }
    }
}
