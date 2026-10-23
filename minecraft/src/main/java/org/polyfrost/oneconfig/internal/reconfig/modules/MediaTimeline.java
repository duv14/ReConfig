/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;
/** Bounded interpolation between operating-system media snapshots. */
public final class MediaTimeline {
    private MediaTimeline() {}
    public static long position(long position, long duration, boolean playing, long sampledAt, long now) {
        long elapsed = playing ? Math.max(0, Math.min(10000, now - sampledAt)) / 1000 : 0;
        long result = Math.max(0, Math.min(86400, position)) + elapsed;
        return duration > 0 ? Math.min(result, duration) : result;
    }
    public static double progress(long position, long duration) {
        return duration <= 0 ? 0 : Math.max(0, Math.min(1, (double)position / duration));
    }
}
