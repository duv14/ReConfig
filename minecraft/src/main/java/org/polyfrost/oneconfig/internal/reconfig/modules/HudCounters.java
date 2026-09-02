/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import java.util.ArrayDeque;

/** Client-thread counters using monotonic milliseconds supplied by the caller. */
public final class HudCounters {
    private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
    private int streak, target = -1;
    private long lastDamage;

    public void click(int button, long now) {
        if (button != 0 && button != 1) return;
        cps(button, now);
        (button == 0 ? left : right).addLast(now);
    }

    public int cps(int button, long now) {
        ArrayDeque<Long> queue = button == 0 ? left : right;
        while (!queue.isEmpty() && now - queue.peekFirst() >= 1000) queue.removeFirst();
        return queue.size();
    }

    public void damage(int victim, int attacker, int localPlayer, long now) {
        if (victim == localPlayer) { streak = 0; target = -1; return; }
        if (attacker != localPlayer) return;
        if (victim != target || now - lastDamage >= 3000) streak = 0;
        target = victim;
        lastDamage = now;
        streak++;
    }

    public int combo(long now) {
        if (now - lastDamage >= 3000) { streak = 0; target = -1; }
        return streak;
    }

    public void clear() { left.clear(); right.clear(); streak = 0; target = -1; }
}
