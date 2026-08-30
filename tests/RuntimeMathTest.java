/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
import org.polyfrost.oneconfig.internal.reconfig.modules.EffectMath;
import org.polyfrost.oneconfig.internal.reconfig.modules.FreeLookState;
import org.polyfrost.oneconfig.internal.reconfig.modules.PredictionPolicy;

public class RuntimeMathTest {
    public static void main(String[] args) {
        check(EffectMath.alpha(0x80ABCDEF, 50) == 0x40ABCDEF, "particle alpha preserves RGB");
        check(EffectMath.alpha(0xFFABCDEF, 0) == 0x00ABCDEF, "transparent particles");
        check(EffectMath.fogEnd(96, 100) == 96, "full fog distance");
        check(EffectMath.fogEnd(96, 50) > 96, "lower opacity extends fog transition");
        check(EffectMath.color("#66AFFF", -1) == 0xFF66AFFF, "RGB input");
        check(EffectMath.color("invalid", -1) == -1, "bad color fallback");
        check(EffectMath.retention(100, 0, 1.0/60) > EffectMath.retention(35, 0, 1.0/60), "stronger blur retains more history");
        check(EffectMath.retention(70, 100, 1.0/60) < EffectMath.retention(70, 0, 1.0/60), "responsiveness shortens trails");
        check(Math.abs(Math.pow(EffectMath.retention(70, 40, 1.0/120), 2) - EffectMath.retention(70, 40, 1.0/60)) < 1e-8, "frame-rate independent decay");
        FreeLookState state = new FreeLookState();
        state.begin(30, 10);
        state.turn(20, 2000);
        check(state.yaw() == 33 && state.pitch() == 90, "mouse delta and pitch clamp");
        state.end(); state.turn(10, 10);
        check(!state.active() && state.yaw() == 33, "inactive camera ignores input");
        check(PredictionPolicy.matches(true, true, 2, 1_000_000_000L), "own nearby matching projectile confirms prediction");
        check(!PredictionPolicy.matches(false, true, 2, 1), "another player cannot confirm prediction");
        check(!PredictionPolicy.matches(true, false, 2, 1), "wind charge cannot confirm pearl");
        check(!PredictionPolicy.matches(true, true, 100, 1), "unrelated distant spawn is not matched");
        check(!PredictionPolicy.matches(true, true, 2, 6_000_000_000L), "expired predictions cannot match");
        state.begin(-100, -20);
        check(state.yaw() == -100 && state.pitch() == -20, "new session resets rotation");
        System.out.println("RuntimeMathTest: passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
