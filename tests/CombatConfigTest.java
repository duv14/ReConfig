/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Regression coverage for duv14's integrated combat modules.
 */
import org.polyfrost.oneconfig.internal.reconfig.combat.*;
import java.util.*;

public final class CombatConfigTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        HitboxCategoriesConfig config = HitboxCategoriesConfig.defaults();
        check(config.getCategories().stream().allMatch(c -> c.getPlayers().isEmpty()));
        HitboxCategory one = config.addCategory("One", "#aabbcc");
        HitboxCategory two = config.addCategory("Two", "#123456");
        config.assignPlayer(one.getId(), "Example");
        config.assignPlayer(two.getId(), "example");
        check(!one.containsPlayer("example") && two.containsPlayer("EXAMPLE"));
        two.setName("Renamed"); two.setHurtColor("#123abc");
        check(two.getName().equals("Renamed") && two.getHurtArgb() == 0xFF123ABC);
        try { config.setHitboxThickness(Float.NaN); throw new AssertionError(); } catch (IllegalArgumentException expected) { }
        config.sanitize(); check(config.findCategoryForPlayer("EXAMPLE").orElseThrow() == two);
        check(HitboxColorResolver.resolve(1,2,3,true,true)==3);
        check(HitboxColorResolver.resolve(1,2,3,true,false)==2);
        check(DistanceLineWidth.forDistance(2.5f,100) == 0.5f);
        HitFlashTracker flashes = new HitFlashTracker(1000); UUID id = UUID.randomUUID();
        flashes.record(id,100); check(flashes.isFlashing(id,1099)); check(!flashes.isFlashing(id,1100));
        HighlightConfig highlights = new HighlightConfig();
        highlights.setRole("Example",HighlightConfig.PlayerRole.ALLY);
        highlights.setRole("EXAMPLE",HighlightConfig.PlayerRole.ENEMY);
        check(highlights.trackedPlayers.size()==1 && highlights.find("example").orElseThrow().role==HighlightConfig.PlayerRole.ENEMY);
        highlights.trackedPlayers.add(null); highlights.trackedPlayers.add(new HighlightConfig.TrackedPlayer("bad name",null,null));
        highlights.allyColor=0; highlights.sanitize(); check(highlights.trackedPlayers.size()==1 && highlights.allyColor==0);
        highlights.remove("example"); check(highlights.trackedPlayers.isEmpty());
        System.out.println("CombatConfigTest passed");
    }
}
