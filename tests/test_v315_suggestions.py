from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source(path):
    return (ROOT / path).read_text(encoding="utf-8")


def test_top_level_page_memory_is_persisted_for_two_hours():
    memory = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/navigation/ReConfigRouteMemory.kt")
    screen = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/compose/impls/OneConfigUIScreen.kt")
    assert "2 * 60 * 60 * 1000L" in memory
    assert 'Preferences.userRoot().node("dev/duv14/reconfig/navigation")' in memory
    assert "routeForRememberedPage" in memory
    assert "route = opening.route" in screen


def test_every_module_hud_can_hide_its_background():
    catalog = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt")
    huds = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/VisualModuleHuds.kt")
    text_huds = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ModuleHuds.kt")
    for module_id in ("item_counter", "waila", "cps", "fps", "keystrokes", "armor_status", "effect_status", "coordinates", "combo_counter", "inventory_hud", "memory_monitor", "server_status"):
        module = catalog.split(f'ClientModule("{module_id}"', 1)[1].split("ClientModule(", 1)[0]
        assert 'toggle("show_background","Show background",true)' in module
    assert 'ModuleAccess.choice(moduleId, "show_background", "true")' in huds
    assert "if (showBackground()) roundedPanel" in huds
    assert 'showBackground = ModuleAccess.choice(moduleId, "show_background", "true").toBoolean()' in text_huds
    assert 'showBackground = ModuleAccess.choice("item_counter", "show_background", "true").toBoolean()' in text_huds
    assert 'showBackground = ModuleAccess.choice("waila", "show_background", "true").toBoolean()' in text_huds


def test_hitbox_editor_has_explicit_close_and_escape():
    editor = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/CombatEditors.kt")
    modules = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt")
    assert "HitboxCategoryEditor(onClose" in editor
    assert "Key.Escape" in editor
    assert 'CombatButton("Close", onClose)' in editor
    assert "HitboxCategoryEditor(back)" in modules


def test_hitbox_categories_are_collapsible_and_have_global_override_order():
    editor = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/CombatEditors.kt")
    config = source("modules/internal/src/main/java/org/polyfrost/oneconfig/internal/reconfig/combat/HitboxCategoriesConfig.java")
    renderer = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigHitboxes.java")
    assert "expandedCategories" in editor
    assert 'CombatButton("Collapse all"' in editor
    assert 'CombatButton("Expand all"' in editor
    assert "category.players.size" in editor
    assert 'Text("Global hitbox color"' in editor
    assert "getGlobalArgb" in config
    assert "colorForPlayer" in config
    assert "config.getGlobalArgb()" in renderer


def test_native_settings_expose_accent_and_tint_wordmark():
    config = source("modules/internal/src/main/java/org/polyfrost/oneconfig/internal/OneConfigConfig.java")
    theme = source("modules/internal/src/main/java/org/polyfrost/oneconfig/internal/ThemeConfig.java")
    sidebar = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/Sidebar.kt")
    assert "public static PolyColor accentColor = new PolyColor(0xFF406CAB);" in config
    assert "ThemeConfig.accentColor = v;" in config
    assert "new PolyColor(0xFF406CAB)" in theme
    assert "ColorFilter.tint(Accent, BlendMode.SrcIn)" in sidebar
