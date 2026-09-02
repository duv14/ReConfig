from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source(path):
    return (ROOT / path).read_text(encoding="utf-8")


def test_release_version_is_314():
    assert "version=3.1.4" in source("gradle.properties")


def test_emoji_transport_uses_asset_tokens_not_unicode():
    social = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt")
    assert "reconfig-emoji:" in social
    assert 'select(emoji)' not in social


def test_global_search_is_reconfig_only():
    search = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/SearchResultsScreen.kt")
    assert "ReConfigModuleSearchResults(normalized)" in search
    assert "SearchCorpus.search" not in search


def test_crosshair_draws_inside_vanilla_crosshair_pass():
    mixin = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigCrosshair.java")
    assert "CustomCrosshair.render(graphics)" in mixin
    before_tail = mixin.split('method = "render"', 1)[0]
    assert "CustomCrosshair.render(graphics)" in before_tail


def test_toggle_sprint_no_longer_redirects_option_supplier():
    mixin = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigToggleOptions.java")
    assert "BooleanSupplier" not in mixin
    assert "@Redirect" not in mixin


def test_huds_are_hidden_during_reconfig_except_editor():
    visual = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/VisualModuleHuds.kt")
    assert "ShellState.uiOpen && !HudManager.isEditing" in visual


def test_right_shift_close_is_focus_aware():
    social = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt")
    assert "trackTextInputFocus()" in social


def test_social_button_scale_animation_is_fully_declared():
    social = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt")
    assert "import androidx.compose.animation.core.animateFloatAsState" in social
    button = social.split("private fun SocialButton", 1)[1].split("private fun EmojiPicker", 1)[0]
    assert "val pressed by source.collectIsPressedAsState()" in button
    assert "val scale by animateFloatAsState" in button


def test_keystrokes_local_renderer_is_composable():
    huds = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/VisualModuleHuds.kt")
    keystrokes = huds.split("class KeystrokesHud", 1)[1]
    assert "@Composable\n        fun key(" in keystrokes


def test_social_polling_uses_tab_aware_full_refresh_and_lightweight_background_sync():
    social = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt")
    assert "fun setTabVisible(visible: Boolean)" in social
    assert "if (socialTabVisible && System.currentTimeMillis() - lastFullRefresh >= 10_000L) refresh()" in social
    assert "backgroundSync()" in social
    assert 'get("/v2/sync?after=' in social


def test_fps_boost_is_a_real_catalog_module_not_a_performance_claim():
    catalog = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt")
    assert 'ClientModule("fps_boost","FPS Boost"' in catalog
    assert "double your frame rate" not in catalog.lower()


def test_freelook_overrides_camera_after_vanilla_alignment_without_mouse_handler_conflict():
    camera = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigCamera.java")
    mixins = source("minecraft/src/main/resources/mixins.oneconfigv1.json")
    assert "shift = At.Shift.AFTER" in camera
    assert "setRotation(FreeLookController.STATE.yaw(), FreeLookController.STATE.pitch())" in camera
    assert "Mixin_ReConfigMouseCamera" not in mixins


def test_settings_route_uses_native_reconfig_config_tree():
    screen = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt")
    preferences = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/Preferences.kt")
    assert "@Composable fun ReConfigSettingsScreen(){" in screen
    assert "Preferences()" in screen.split("@Composable fun ReConfigSettingsScreen(){", 1)[1].split("}", 1)[0]
    assert 'PREFERENCES_ID = "reconfig.json"' in preferences
