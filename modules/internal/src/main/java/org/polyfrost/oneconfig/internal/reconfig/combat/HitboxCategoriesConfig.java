/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's supplied Hitbox Categories source. See ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.reconfig.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class HitboxCategoriesConfig {
    public static final int CURRENT_VERSION = 3;
    public static final float DEFAULT_HITBOX_THICKNESS = 1.0F;
    public static final String DEFAULT_RANGE_COLOR = "#FF0000";

    private int version = CURRENT_VERSION;
    private float hitboxThickness = DEFAULT_HITBOX_THICKNESS;
    private String rangeColor = DEFAULT_RANGE_COLOR;
    private List<HitboxCategory> categories = new ArrayList<>();

    public HitboxCategoriesConfig() {}

    public static HitboxCategoriesConfig defaults() {
        HitboxCategoriesConfig config = new HitboxCategoriesConfig();
        config.categories.add(new HitboxCategory(
                "teammates",
                "Teammates",
                "#55FF55",
                "#FFFFFF",
                List.of()
        ));
        return config;
    }

    public int getVersion() { return version; }
    public float getHitboxThickness() { return hitboxThickness; }
    public String getRangeColor() { return rangeColor; }
    public int getRangeArgb() { return HexColor.parseArgb(rangeColor); }
    public List<HitboxCategory> getCategories() { return Collections.unmodifiableList(categories); }

    public void setHitboxThickness(float hitboxThickness) {
        if (!Float.isFinite(hitboxThickness) || hitboxThickness <= 0.0F) {
            throw new IllegalArgumentException("hitbox thickness must be a positive number");
        }
        this.hitboxThickness = hitboxThickness;
    }

    public void setRangeColor(String rangeColor) {
        this.rangeColor = HexColor.normalize(rangeColor);
    }

    public Optional<HitboxCategory> findCategory(String categoryId) {
        if (categoryId == null) return Optional.empty();
        return categories.stream().filter(category -> category.getId().equals(categoryId)).findFirst();
    }

    public Optional<HitboxCategory> findCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return Optional.empty();
        String normalized = categoryName.trim().toLowerCase(Locale.ROOT);
        return categories.stream()
                .filter(category -> category.getName().trim().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    public Optional<HitboxCategory> findCategoryForPlayer(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return categories.stream().filter(category -> category.containsPlayer(username)).findFirst();
    }

    public HitboxCategory addCategory(String name, String color) {
        HitboxCategory category = new HitboxCategory(
                UUID.randomUUID().toString(), name, color, HitboxCategory.DEFAULT_HURT_COLOR, List.of());
        categories.add(category);
        return category;
    }

    public boolean removeCategory(String categoryId) {
        return categories.removeIf(category -> category.getId().equals(categoryId));
    }

    public void assignPlayer(String categoryId, String username) {
        HitboxCategory target = findCategory(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown category id: " + categoryId));
        for (HitboxCategory category : categories) {
            category.removePlayer(username);
        }
        target.addPlayer(username);
    }

    public void sanitize() {
        version = CURRENT_VERSION;
        if (!Float.isFinite(hitboxThickness) || hitboxThickness <= 0.0F) {
            hitboxThickness = DEFAULT_HITBOX_THICKNESS;
        }
        try {
            rangeColor = HexColor.normalize(rangeColor);
        } catch (IllegalArgumentException ignored) {
            rangeColor = DEFAULT_RANGE_COLOR;
        }
        if (categories == null) categories = new ArrayList<>();

        List<HitboxCategory> sanitized = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> players = new HashSet<>();

        for (HitboxCategory category : categories) {
            if (category == null) continue;
            try { category.sanitize(); } catch (IllegalArgumentException malformed) { continue; }
            if (!ids.add(category.getId())) continue;

            List<String> uniquePlayers = new ArrayList<>(category.getPlayers());
            for (String player : uniquePlayers) {
                String normalized = player.trim().toLowerCase(Locale.ROOT);
                if (!players.add(normalized)) category.removePlayer(player);
            }
            sanitized.add(category);
        }
        categories = sanitized;
    }
}
