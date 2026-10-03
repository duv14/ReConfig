/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
import org.polyfrost.oneconfig.internal.reconfig.WaypointProjection;
public class WaypointProjectionTest {
    public static void main(String[] args) {
        var center=WaypointProjection.project(0,0,10,0,0,90,800,600);
        if(center==null || center.x()!=400 || center.y()!=300)throw new AssertionError("front center");
        if(WaypointProjection.project(0,0,-10,0,0,90,800,600)!=null)throw new AssertionError("behind camera");
        var right=WaypointProjection.project(-10,0,10,0,0,90,800,600);
        if(right==null || Math.abs(right.x()-700)>0.01)throw new AssertionError("right side");
        var west=WaypointProjection.project(-10,0,0,90,0,90,800,600);
        if(west==null || Math.abs(west.x()-400)>0.01)throw new AssertionError("yaw");
        System.out.println("WaypointProjectionTest passed");
    }
}
