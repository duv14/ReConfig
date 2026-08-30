/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.api.event.v1.events;

public abstract class HudEvent implements Event {
    public final boolean opened;

    private HudEvent(boolean opened) {
        this.opened = opened;
    }

    public static final class Tab extends HudEvent {
        public static final Tab OPENED = new Tab(true);
        public static final Tab CLOSED = new Tab(false);

        private Tab(boolean opened) {
            super(opened);
        }
    }

    public static final class Debug extends HudEvent {
        public static final Debug OPENED = new Debug(true);
        public static final Debug CLOSED = new Debug(false);

        private Debug(boolean opened) {
            super(opened);
        }
    }

    public boolean component1() {
        return opened;
    }
}
