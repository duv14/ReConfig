/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
import org.polyfrost.oneconfig.internal.reconfig.CrosshairPattern;
public class CrosshairPatternTest {
    public static void main(String[] args) {
        if(!CrosshairPattern.sample("Cross",0,5,7,3,2,false))throw new AssertionError("cross arm");
        if(CrosshairPattern.sample("Cross",0,0,7,3,2,false))throw new AssertionError("center gap");
        if(!CrosshairPattern.sample("Dot",0,0,1,0,1,false))throw new AssertionError("thin dot invisible");
        if(!CrosshairPattern.sample("Circle",4,0,8,0,2,false))throw new AssertionError("ring missing");
        if(CrosshairPattern.sample("Circle",0,0,8,0,2,false))throw new AssertionError("ring filled");
        if(!CrosshairPattern.sample("Cross",0,0,7,3,2,true))throw new AssertionError("center dot missing");
        System.out.println("CrosshairPatternTest passed");
    }
}
