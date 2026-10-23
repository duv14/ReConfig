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
    private static double scrollFactor = 1;
    private ZoomController() {}
    public static void activate(boolean value) { if (value && !active) scrollFactor = 1; active = value; }
    public static boolean scroll(double amount) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || !ModuleAccess.enabled("zoom") || mc.screen != null || mc.player == null || !Double.isFinite(amount) || amount == 0) return false;
        double base = Math.max(1, ModuleAccess.number("zoom", "magnification", 4));
        scrollFactor = Math.max(1 / base, Math.min(64 / base, scrollFactor * Math.pow(1.15, Math.max(-10, Math.min(10, amount)))));
        return true;
    }
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
            ? 1 / Math.max(1, Math.min(64, ModuleAccess.number("zoom", "magnification", 4) * scrollFactor)) : 1;
        double ratio = ZOOM.step(factor, dt, ModuleAccess.number("zoom", "animation_ms", 150) / 1000.0);
        if (ratio > .99999 && factor == 1) return fov;
        // Scale the projection, not the angle, for consistent optical magnification.
        return (float) Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(Math.min(179, fov)) / 2) * ratio));
    }
    public static double sensitivityScale() { return ZOOM.value(); }
}
