import org.polyfrost.oneconfig.internal.reconfig.modules.HudOptions;

public class HudOptionsTest {
    public static void main(String[] args) {
        check(HudOptions.projectiles(64, 12, 8, 16, 4, false, true, true, false, false, false)
            .equals("Pearls 12 • Wind 8"), "only selected projectile types are displayed");
        check(HudOptions.projectiles(0, 0, 0, 0, 0, true, true, true, true, true, true)
            .isEmpty(), "zero counts can be hidden");
        check(HudOptions.projectiles(64, 12, 8, 16, 4, false, false, false, false, false, false)
            .isEmpty(), "all projectile rows disabled");
        check(HudOptions.projectiles(0, 2, 0, 3, 4, true, true, false, true, true, false)
            .equals("Arrows 0 • Pearls 2 • Snowballs 3 • Eggs 4"), "independent throwable counts");
        check(HudOptions.duration(1, false).equals("0:01"), "last partial second is visible");
        check(HudOptions.duration(1201, false).equals("1:01"), "duration rounds up");
        check(HudOptions.duration(-5, false).equals("0:00"), "expired duration is clamped");
        check(HudOptions.duration(-1, true).equals("∞"), "infinite effects");
        check(HudOptions.chunkCap(4, 8) == 4, "boost never raises low chunk distance");
        check(HudOptions.chunkCap(16, 8) == 8, "boost reduces high chunk distance");
        check(HudOptions.entityCap(.5, 75) == .5, "boost never raises low entity distance");
        check(HudOptions.entityCap(1, 75) == .75, "boost reduces high entity distance");
        check(HudOptions.contentVisible(true, true, false), "empty HUD previews remain movable in the editor");
        check(!HudOptions.contentVisible(false, true, false), "empty gameplay HUD can hide");
        check(HudOptions.contentVisible(false, false, false), "show empty gameplay state when requested");
        check(HudOptions.contentVisible(false, true, true), "active effects remain visible");
        System.out.println("HudOptionsTest: 16 behavior checks passed");
    }
    static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
