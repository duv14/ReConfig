/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

/** Dependency-free regression runner: javac + java -ea. */
public final class HudCountersTest {
    public static void main(String[] args) {
        HudCounters c = new HudCounters();
        c.click(0, 100); c.click(0, 150); c.click(1, 200);
        assert c.cps(0, 1000) == 2;
        assert c.cps(1, 1000) == 1;
        assert c.cps(0, 1100) == 1 : "one-second boundary must expire";
        assert c.cps(0, 1150) == 0;
        c.damage(7, 4, 4, 2000);
        assert c.combo(2000) == 1;
        c.damage(7, 4, 4, 2100);
        assert c.combo(2100) == 2;
        c.damage(8, 4, 4, 2200);
        assert c.combo(2200) == 1 : "different target resets streak";
        c.damage(9, 3, 4, 2300);
        assert c.combo(2300) == 1 : "other players must not increment";
        c.damage(4, 3, 4, 2400);
        assert c.combo(2400) == 0 : "taking damage resets streak";
        c.damage(7, 4, 4, 2500);
        assert c.combo(5500) == 0 : "idle streak expires";
        c.clear();
        assert c.cps(1, 5500) == 0;
        System.out.println("HudCountersTest passed");
    }
}
