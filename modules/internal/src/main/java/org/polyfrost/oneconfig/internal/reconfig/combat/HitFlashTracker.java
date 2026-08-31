/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's supplied Hitbox Categories source. See ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.reconfig.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HitFlashTracker {
    private final long durationMillis;
    private final Map<UUID, Long> expiresAtByPlayer = new ConcurrentHashMap<>();

    public HitFlashTracker(long durationMillis) {
        if (durationMillis <= 0L) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        this.durationMillis = durationMillis;
    }

    public void record(UUID playerId, long nowMillis) {
        if (playerId == null) return;
        expiresAtByPlayer.put(playerId, nowMillis + durationMillis);
    }

    public boolean isFlashing(UUID playerId, long nowMillis) {
        if (playerId == null) return false;
        Long expiresAt = expiresAtByPlayer.get(playerId);
        if (expiresAt == null) return false;
        if (nowMillis >= expiresAt) {
            expiresAtByPlayer.remove(playerId, expiresAt);
            return false;
        }
        return true;
    }

    public void clear() {
        expiresAtByPlayer.clear();
    }
}
