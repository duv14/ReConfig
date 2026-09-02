/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig;

/** Camera-relative perspective projection; null means behind the camera. */
public final class WaypointProjection {
    private WaypointProjection() {}
    public record Point(double x,double y) {}
    public static Point project(double dx,double dy,double dz,double yaw,double pitch,double fov,int width,int height) {
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
        double right=-Math.cos(y)*dx-Math.sin(y)*dz;
        double horizontal=-Math.sin(y)*dx+Math.cos(y)*dz;
        double depth=horizontal*Math.cos(p)-dy*Math.sin(p);
        double up=horizontal*Math.sin(p)+dy*Math.cos(p);
        if(!Double.isFinite(depth)||depth<=0.05)return null;
        double focal=height/(2*Math.tan(Math.toRadians(Math.max(1,Math.min(179,fov)))/2));
        return new Point(width/2.0+right*focal/depth,height/2.0-up*focal/depth);
    }
}
