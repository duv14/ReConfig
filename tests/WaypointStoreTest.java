/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
import java.nio.file.*;
import org.polyfrost.oneconfig.internal.reconfig.WaypointStore;

public class WaypointStoreTest {
    public static void main(String[] args) throws Exception {
        Path file = Path.of(args[0], "waypoints.properties");
        WaypointStore s = new WaypointStore(file);
        var a = s.add("server-a", "minecraft:overworld", "Home", 10,64,-20,0xFF88BBFF);
        s.add("server-b", "minecraft:overworld", "Other server", 0,64,0,-1);
        s.add("server-a", "minecraft:the_nether", "Portal", 1,80,2,-1);
        if(s.visible("server-a","minecraft:overworld").size()!=1)throw new AssertionError("world isolation");
        s = new WaypointStore(file);
        if(!s.all().get(0).name().equals("Home"))throw new AssertionError("restart persistence");
        s.put(new WaypointStore.Entry(a.id(),a.world(),a.dimension(),"New home",11,65,-21,a.color(),false));
        if(!s.visible("server-a","minecraft:overworld").isEmpty())throw new AssertionError("hidden markers");
        try { s.add("server-a","minecraft:overworld","Bad",Double.NaN,0,0,-1); throw new AssertionError("NaN accepted"); } catch(IllegalArgumentException expected){}
        if(s.all().size()!=3)throw new AssertionError("invalid mutation changed store");
        s.remove(a.id());
        if(new WaypointStore(file).all().size()!=2)throw new AssertionError("delete not persisted");
        System.out.println("WaypointStoreTest passed");
    }
}
