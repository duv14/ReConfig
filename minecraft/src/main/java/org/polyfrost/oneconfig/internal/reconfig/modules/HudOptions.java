package org.polyfrost.oneconfig.internal.reconfig.modules;

import java.util.StringJoiner;

/** Small allocation-bounded formatting and render-cap policies, independent of Minecraft. */
public final class HudOptions {
    private HudOptions() {}
    public static String projectiles(int arrows, int pearls, int wind, int snowballs, int eggs,
            boolean showArrows, boolean showPearls, boolean showWind,
            boolean showSnowballs, boolean showEggs, boolean hideZero) {
        StringJoiner result = new StringJoiner(" • ");
        add(result, "Arrows", arrows, showArrows, hideZero);
        add(result, "Pearls", pearls, showPearls, hideZero);
        add(result, "Wind", wind, showWind, hideZero);
        add(result, "Snowballs", snowballs, showSnowballs, hideZero);
        add(result, "Eggs", eggs, showEggs, hideZero);
        return result.toString();
    }
    private static void add(StringJoiner result, String label, int count, boolean show, boolean hideZero) {
        if (show && (!hideZero || count > 0)) result.add(label + " " + Math.max(0, count));
    }
    public static String duration(int ticks, boolean infinite) {
        if (infinite) return "∞";
        long seconds = (Math.max(0L, ticks) + 19L) / 20L;
        return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
    }
    public static boolean contentVisible(boolean editing, boolean hideEmpty, boolean hasContent) {
        return editing || !hideEmpty || hasContent;
    }
    public static int chunkCap(int previous, int requested) {
        return Math.min(previous, Math.max(2, Math.min(32, requested)));
    }
    public static double entityCap(double previous, double requestedPercent) {
        return Math.min(previous, Math.max(25, Math.min(100, requestedPercent)) / 100.0);
    }
}
