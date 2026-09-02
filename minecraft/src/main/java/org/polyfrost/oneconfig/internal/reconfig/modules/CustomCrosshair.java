/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.CrosshairPattern;

public final class CustomCrosshair {
    private CustomCrosshair() {}
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return ModuleAccess.enabled("crosshair") && mc.player != null && !mc.player.isSpectator()
            && !mc.options.hideGui && mc.options.getCameraType().isFirstPerson();
    }
    public static boolean render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.screen != null) return false;
        int length = Math.round(ModuleAccess.number("crosshair", "length", 7));
        int gap = Math.round(ModuleAccess.number("crosshair", "gap", 3));
        int thickness = Math.round(ModuleAccess.number("crosshair", "thickness", 2));
        if (flag("dynamic_spread")) gap += Math.min(8, (int) Math.round(mc.player.getDeltaMovement().horizontalDistance() * 20));
        String shape = ModuleAccess.choice("crosshair", "shape", "Cross");
        int radius = Math.min(65, length + gap + thickness + 2);
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        int color = EffectMath.alpha(EffectMath.color(ModuleAccess.choice("crosshair", "crosshair_color", "#FFFFFF"), -1), ModuleAccess.number("crosshair", "crosshair_opacity", 100));
        boolean outline = flag("outline");
        boolean dot = flag("center_dot");
        // Merge horizontal runs: a circle uses tens of submissions, not thousands of pixel quads.
        for (int pass = outline ? 0 : 1; pass < 2; pass++) {
            for (int y = -radius; y <= radius; y++) {
                int start = Integer.MIN_VALUE;
                for (int x = -radius; x <= radius + 1; x++) {
                    boolean filled = x <= radius && sample(shape, x, y, length, gap, thickness, dot);
                    if (pass == 0 && x <= radius && !filled) {
                        for (int dy = -1; dy <= 1 && !filled; dy++)
                            for (int dx = -1; dx <= 1 && !filled; dx++)
                                filled = sample(shape, x + dx, y + dy, length, gap, thickness, dot);
                    }
                    if (filled && start == Integer.MIN_VALUE) start = x;
                    if (!filled && start != Integer.MIN_VALUE) {
                        graphics.fill(cx + start, cy + y, cx + x, cy + y + 1, pass == 0 ? color & 0xFF000000 : color);
                        start = Integer.MIN_VALUE;
                    }
                }
            }
        }
        return true;
    }
    private static boolean flag(String setting) { return Boolean.parseBoolean(ModuleAccess.choice("crosshair", setting, "false")); }
    private static boolean sample(String shape, int x, int y, int length, int gap, int thickness, boolean dot) {
        return CrosshairPattern.sample(shape,x,y,length,gap,thickness,dot);
    }
}
