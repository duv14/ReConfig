/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's supplied Team Highlight source. See ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.reconfig.combat;

import java.util.*;

/** Pure persisted model; the ReConfig module supplies the enabled state. */
public final class HighlightConfig {
    public enum PlayerRole { TEAMMATE, ALLY, ENEMY }
    public static final class TrackedPlayer {
        public String name;
        public String uuid;
        public PlayerRole role;
        public TrackedPlayer(String name, String uuid, PlayerRole role) {
            this.name = name; this.uuid = uuid; this.role = role;
        }
    }
    public int teammateColor = 0x55FF55;
    public int allyColor = 0x5599FF;
    public int enemyColor = 0xFF5555;
    public List<TrackedPlayer> trackedPlayers = new ArrayList<>();
    public Optional<TrackedPlayer> find(String name) {
        return trackedPlayers.stream().filter(p -> p.name.equalsIgnoreCase(name.trim())).findFirst();
    }
    public void setRole(String name, PlayerRole role) {
        if (name == null || !name.trim().matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Enter a Minecraft username (1–16 letters, digits or underscore)");
        Objects.requireNonNull(role, "role");
        find(name).ifPresentOrElse(p -> p.role = role, () -> trackedPlayers.add(new TrackedPlayer(name.trim(), null, role)));
    }
    public void remove(String name) { trackedPlayers.removeIf(p -> p.name.equalsIgnoreCase(name)); }
    public int color(PlayerRole role) { return role == PlayerRole.TEAMMATE ? teammateColor : role == PlayerRole.ALLY ? allyColor : enemyColor; }
    public void sanitize() {
        teammateColor &= 0xFFFFFF; allyColor &= 0xFFFFFF; enemyColor &= 0xFFFFFF;
        if (trackedPlayers == null) trackedPlayers = new ArrayList<>();
        Set<String> names = new HashSet<>();
        trackedPlayers.removeIf(p -> p == null || p.name == null || p.role == null || !p.name.trim().matches("[A-Za-z0-9_]{1,16}") || !names.add(p.name.trim().toLowerCase(Locale.ROOT)));
        trackedPlayers.forEach(p -> p.name = p.name.trim());
    }
}
