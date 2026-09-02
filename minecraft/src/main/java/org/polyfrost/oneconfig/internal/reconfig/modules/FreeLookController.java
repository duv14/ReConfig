/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class FreeLookController {
    public static final FreeLookState STATE = new FreeLookState();
    private static CameraType previousPerspective;
    private static Object player;
    private FreeLookController() {}

    public static void update(Minecraft mc, boolean requested) {
        if (player != mc.player && STATE.active()) stop(mc);
        if (!requested || mc.player == null || !mc.player.isAlive() || mc.screen != null) {
            stop(mc);
            return;
        }
        if (!STATE.active()) {
            previousPerspective = mc.options.getCameraType();
            player = mc.player;
            STATE.begin(mc.player.getYRot(), mc.player.getXRot());
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }
    private static void stop(Minecraft mc) {
        if (STATE.active() && previousPerspective != null) mc.options.setCameraType(previousPerspective);
        STATE.end(); previousPerspective = null; player = null;
    }
}
