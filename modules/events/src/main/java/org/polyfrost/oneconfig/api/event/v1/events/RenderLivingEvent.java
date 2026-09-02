/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.api.event.v1.events;

public abstract class RenderLivingEvent extends Event.Cancellable {

    private final Object entity;
    private final float partialTicks;
    private final double x, y, z;

    public RenderLivingEvent(Object entity, float partialTicks, double x, double y, double z) {
        this.entity = entity;
        this.partialTicks = partialTicks;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Object getEntity() {
        return entity;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public static class Pre extends RenderLivingEvent {
        public Pre(Object entity, float partialTicks, double x, double y, double z) {
            super(entity, partialTicks, x, y, z);
        }
    }

    public static class Post extends RenderLivingEvent {
        public Post(Object entity, float partialTicks, double x, double y, double z) {
            super(entity, partialTicks, x, y, z);
        }
    }

}
