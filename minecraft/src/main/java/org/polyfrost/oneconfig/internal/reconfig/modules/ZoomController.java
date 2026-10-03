/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;

public final class ZoomController {
    private static final SmoothZoom ZOOM = new SmoothZoom();
    private static long previousFrame;
    private static boolean active;
    private ZoomController() {}
    public static void activate(boolean value) { active = value; }
    public static float apply(float fov) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (!ModuleAccess.enabled("zoom")) {
            ZOOM.reset(); previousFrame = now; active = false; return fov;
        }
        if (mc.player == null || mc.level == null) {
            ZOOM.reset(); previousFrame = now; active = false; return fov;
        }
        double dt = previousFrame == 0 ? 0 : (now - previousFrame) / 1_000_000_000.0;
        previousFrame = now;
        double factor = active && mc.screen == null
            ? 1 / Math.max(1, ModuleAccess.number("zoom", "magnification", 4)) : 1;
        double ratio = ZOOM.step(factor, dt, ModuleAccess.number("zoom", "animation_ms", 150) / 1000.0);
        if (ratio > .99999 && factor == 1) return fov;
        // Scale the projection, not the angle, for consistent optical magnification.
        return (float) Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(Math.min(179, fov)) / 2) * ratio));
    }
    public static double sensitivityScale() { return ZOOM.value(); }
}
