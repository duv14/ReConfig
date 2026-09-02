/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.polyfrost.oneconfig.internal.reconfig.*;
import org.polyfrost.oneconfig.internal.mixin.reconfig.GameRendererFovAccessor;

/** Screen-space world labels; does not modify server waypoints or locator-bar data. */
public final class WaypointRenderer {
    private WaypointRenderer() {}
    public static void render(GuiGraphics g,DeltaTracker ticks) {
        Minecraft mc=Minecraft.getInstance();
        if(!ModuleAccess.enabled("waypoints")||mc.player==null||mc.level==null||mc.options.hideGui||mc.screen!=null)return;
        var context=WaypointRepository.INSTANCE.getContext();
        var store=WaypointRepository.INSTANCE.getStore();
        if(context==null||store==null)return;
        var camera=mc.gameRenderer.getMainCamera();var pos=camera.position();
        float fov=((GameRendererFovAccessor)mc.gameRenderer).reconfig$getFov(camera,ticks.getGameTimeDeltaPartialTick(false),true);
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        int shown=0;
        for(var e:store.visible(context.getWorld(),context.getDimension())) {
            double dx=e.x()-pos.x,dy=e.y()+1-pos.y,dz=e.z()-pos.z;
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(distance>ModuleAccess.number("waypoints","max_distance",4096))continue;
            var projected=WaypointProjection.project(dx,dy,dz,camera.yRot(),camera.xRot(),fov,w,h);
            if(projected==null||projected.x()<8||projected.x()>w-8||projected.y()<8||projected.y()>h-40)continue;
            String label=e.name()+" · "+Math.round(distance)+" m";
            int x=(int)projected.x(),y=(int)projected.y(),half=mc.font.width(label)/2;
            x=Math.max(half+5,Math.min(w-half-5,x));
            g.fill(x-half-4,y-3,x+half+4,y+11,0xB0161E27);
            g.fill(x-2,y-10,x+3,y-5,e.color()|0xFF000000);
            g.drawString(mc.font,label,x-half,y,e.color()|0xFF000000,true);
            if(Boolean.parseBoolean(ModuleAccess.choice("waypoints","coordinates","false"))) {
                String coords=(int)e.x()+", "+(int)e.y()+", "+(int)e.z();
                g.drawString(mc.font,coords,x-mc.font.width(coords)/2,y+12,0xFFB4BBC8,true);
            }
            if(++shown>=32)break;
        }
    }
}
