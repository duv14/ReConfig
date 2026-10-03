/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.api.event.v1.events;

public class SoundPlayedEvent implements Event {

    private final String name;
    private final Object category;
    private Object sound;

    public SoundPlayedEvent(String name, Object category, Object sound) {
        this.name = name;
        this.category = category;
        this.sound = sound;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCategory() {
        return (T) category;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSound() {
        return (T) sound;
    }

    public void setSound(Object sound) {
        this.sound = sound;
    }

}
