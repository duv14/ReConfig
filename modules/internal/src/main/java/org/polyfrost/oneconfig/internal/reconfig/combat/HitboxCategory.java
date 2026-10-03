/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's supplied Hitbox Categories source. See ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.reconfig.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class HitboxCategory {
    public static final String DEFAULT_HURT_COLOR = "#FFFFFF";

    private String id;
    private String name;
    private String color;
    private String hurtColor;
    private List<String> players;

    public HitboxCategory() {
        this("category", "Category", "#FFFFFF", DEFAULT_HURT_COLOR, new ArrayList<>());
    }

    public HitboxCategory(String id, String name, String color, List<String> players) {
        this(id, name, color, DEFAULT_HURT_COLOR, players);
    }

    public HitboxCategory(String id, String name, String color, String hurtColor, List<String> players) {
        this.id = requireNonBlank(id, "id");
        this.name = requireNonBlank(name, "name");
        this.color = HexColor.normalize(color);
        this.hurtColor = HexColor.normalize(hurtColor);
        this.players = new ArrayList<>();
        if (players != null) {
            for (String player : players) {
                addPlayer(player);
            }
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public int getArgb() { return HexColor.parseArgb(color); }
    public String getHurtColor() { return hurtColor; }
    public int getHurtArgb() { return HexColor.parseArgb(hurtColor); }
    public List<String> getPlayers() { return Collections.unmodifiableList(players); }

    public void setName(String name) {
        this.name = requireNonBlank(name, "name");
    }

    public void setColor(String color) {
        this.color = HexColor.normalize(color);
    }

    public void setHurtColor(String hurtColor) {
        this.hurtColor = HexColor.normalize(hurtColor);
    }

    public boolean containsPlayer(String username) {
        String normalized = normalizePlayer(username);
        return players.stream().anyMatch(player -> normalizePlayer(player).equals(normalized));
    }

    public void addPlayer(String username) {
        String clean = requireNonBlank(username, "username");
        if (!containsPlayer(clean)) {
            players.add(clean);
        }
    }

    public boolean removePlayer(String username) {
        String normalized = normalizePlayer(username);
        return players.removeIf(player -> normalizePlayer(player).equals(normalized));
    }

    void sanitize() {
        id = requireNonBlank(id, "id");
        name = requireNonBlank(name, "name");
        color = normalizeOrDefault(color, "#FFFFFF");
        hurtColor = normalizeOrDefault(hurtColor, DEFAULT_HURT_COLOR);
        List<String> oldPlayers = players == null ? List.of() : players;
        players = new ArrayList<>();
        for (String player : oldPlayers) {
            if (player != null && !player.isBlank()) addPlayer(player);
        }
    }

    private static String normalizeOrDefault(String value, String fallback) {
        try {
            return HexColor.normalize(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static String normalizePlayer(String value) {
        return Objects.requireNonNull(value, "username").trim().toLowerCase(Locale.ROOT);
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value.trim();
    }
}
