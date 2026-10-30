package org.polyfrost.oneconfig.internal.reconfig.modules;

/** Render hooks read this snapshot; no Preferences, parsing or Compose reads per quad. */
public final class ParticleRenderSettings {
    private ParticleRenderSettings() {}
    private static volatile State current = new State(false, 1f, 1f);
    public static State state() { return current; }
    public static void update(boolean enabled, float sizePercent, float opacityPercent) {
        float size = Float.isFinite(sizePercent) ? Math.max(10, Math.min(300, sizePercent)) / 100f : 1f;
        float opacity = Float.isFinite(opacityPercent) ? Math.max(0, Math.min(100, opacityPercent)) / 100f : 1f;
        State old = current;
        if (old.enabled != enabled || old.sizeFactor != size || old.opacityFactor != opacity)
            current = new State(enabled, size, opacity);
    }
    public record State(boolean enabled, float sizeFactor, float opacityFactor) {
        public boolean fading() { return enabled && opacityFactor < 1f; }
        public boolean invisible() { return enabled && opacityFactor == 0f; }
        public float scale(float size) { return enabled && sizeFactor != 1f ? size * sizeFactor : size; }
        public int tint(int color) {
            if (!fading()) return color;
            return (color & 0xFFFFFF) | (Math.round((color >>> 24) * opacityFactor) << 24);
        }
    }
}
