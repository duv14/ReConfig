/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.api.event.v1.events;

public class PlayerInteractEvent extends Event.Cancellable {

    public enum Action {
        LEFT,
        RIGHT
    }

    public enum Type {
        BLOCK,
        ENTITY,
        AIR
    }

    private final Object player;
    private final Action action;
    private final Type type;

    public PlayerInteractEvent(Object player, Action action, Type type) {
        this.player = player;
        this.action = action;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPlayer() {
        return (T) player;
    }

    public Action getAction() {
        return action;
    }

    public Type getType() {
        return type;
    }

}
