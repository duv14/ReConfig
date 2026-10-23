/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig;

/** Shared pixel mask for the editor preview and in-game rendering. */
public final class CrosshairPattern {
    private CrosshairPattern() {}
    public static boolean sample(String shape,int x,int y,int length,int gap,int thickness,boolean dot) {
        return sample(shape,x,y,length,length,gap,thickness,dot);
    }
    public static boolean sample(String shape,int x,int y,int width,int height,int gap,int thickness,boolean dot) {
        width=Math.max(1,Math.min(30,width));height=Math.max(1,Math.min(30,height));gap=Math.max(0,Math.min(20,gap));thickness=Math.max(1,Math.min(10,thickness));
        double ax=Math.abs(x+.5),ay=Math.abs(y+.5),half=thickness/2.0;
        if(dot && ax<=half && ay<=half)return true;
        return switch(shape) {
            case "Dot" -> ax<=half && ay<=half;
            case "Circle" -> Math.abs(Math.hypot((x+.5)/(gap+width/2.0),(y+.5)/(gap+height/2.0))-1)*Math.min(gap+width/2.0,gap+height/2.0)<=half;
            case "Chevron" -> ax<=width && Math.abs(y-ax*height/width+gap)<=half;
            default -> (ax<=half && ay>=gap && ay<=gap+height)||(ay<=half && ax>=gap && ax<=gap+width);
        };
    }
}
