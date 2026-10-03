from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


def source(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class PublishReadyRepairsTest(unittest.TestCase):
    def test_all_huds_share_reconfig_visibility_policy(self):
        text_huds = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ModuleHuds.kt")
        visual_huds = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/VisualModuleHuds.kt")
        manager = source("modules/hud/src/main/kotlin/org/polyfrost/oneconfig/api/hud/v1/HudManager.kt")
        self.assertIn("object ReConfigHudVisibility", text_huds)
        self.assertIn("fun gameplayOverlayVisible() = !ShellState.uiOpen || HudManager.isEditorOpen", text_huds)
        self.assertIn("val isEditing: Boolean get() = isEditorOpen", manager)
        self.assertNotIn("val isEditing: Boolean get() = isEditorOpen || isConfigUiOpen", manager)
        self.assertGreaterEqual(text_huds.count("ReConfigHudVisibility.gameplayOverlayVisible()"), 3)
        self.assertIn("ReConfigHudVisibility.gameplayOverlayVisible()", visual_huds)

    def test_social_replies_and_custom_emoji_assets_are_wired_end_to_end(self):
        social = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt")
        worker = source("backend/src/worker.js")
        migrations = source("backend/migrations/0005_message_replies.sql")
        emoji_ids = ("rose", "pray", "cute", "kiss", "kiss-closed", "sob-blue", "sob")
        for emoji_id in emoji_ids:
            self.assertIn(f'ReConfigEmoji("{emoji_id}"', social)
            self.assertTrue((ROOT / f"modules/internal/src/main/resources/assets/reconfig/emojis/{emoji_id}.png").is_file())
        self.assertIn('private const val EMOJI_PREFIX = "reconfig-emoji:"', social)
        self.assertIn("replyingTo?.id", social)
        self.assertIn('event.key==Key.Enter||event.key==Key.NumPadEnter', social)
        self.assertIn('replyTo=Number(b.replyTo||0)', worker)
        self.assertIn('LEFT JOIN messages_v2 r ON r.id=m.reply_to', worker)
        self.assertIn('ADD COLUMN reply_to INTEGER', migrations)


    def test_ui_ambience_controls_are_removed(self):
        config = source("modules/internal/src/main/java/org/polyfrost/oneconfig/internal/OneConfigConfig.java")
        sounds = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/sound/UiSounds.kt")
        for stale in ("enableUIAmbience", "uiAmbienceVolume", '"UI Ambience"'):
            self.assertNotIn(stale, config)
        self.assertNotIn("startAmbience", sounds)


    def test_crosshair_replaces_vanilla_only_when_custom_render_succeeds(self):
        renderer = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules/CustomCrosshair.java")
        mixin = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigCrosshair.java")
        self.assertIn("getGuiScaledWidth() / 2", renderer)
        self.assertIn("getGuiScaledHeight() / 2", renderer)
        self.assertIn("graphics.fill", renderer)
        self.assertIn("if (CustomCrosshair.render(graphics)) ci.cancel();", mixin)
        self.assertIn('@Inject(method = "renderCrosshair", at = @At("HEAD")', mixin)


    def test_fullbright_uses_reversible_lightmap_policy(self):
        mixin = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigFullbright.java")
        self.assertIn("FULL_BRIGHT", mixin)
        self.assertIn('ModuleAccess.enabled("fullbright")', mixin)
        self.assertNotIn("@Redirect", mixin)
        self.assertIn("@WrapOperation", mixin)
        self.assertIn("option == Minecraft.getInstance().options.gamma()", mixin)


    def test_release_metadata_and_changelog_exist(self):
        metadata = source("bootstrap/src/main/resources/fabric.mod.json")
        changelog = source("CHANGELOG.md")
        self.assertIn('"fabric-api":', metadata)
        self.assertIn('"license": "GPL-3.0-only"', metadata)
        self.assertIn("3.2.0", changelog)

    def test_social_sync_is_bounded_and_rate_limit_aware(self):
        social = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt")
        self.assertIn("conversationCursors", social)
        self.assertNotIn('get("/v2/messages?with=${friend.uuid}&after=0")', social)
        self.assertIn("rateLimitedUntil", social)
        self.assertIn("60_000L", social)
        self.assertIn("openConversation", social)
        self.assertIn("ReConfig service is temporarily busy", social)

    def test_backend_hides_d1_quota_errors_and_uses_profile_fallbacks(self):
        worker = source("backend/src/worker.js")
        self.assertIn("api.ashcon.app", worker)
        self.assertIn("retryAfterSeconds", worker)
        self.assertNotIn("server error: ${detail}", worker)

    def test_freelook_uses_entity_turn_and_current_camera_alignment(self):
        camera = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigCamera.java")
        look = source("minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigLookInput.java")
        mixins = source("minecraft/src/main/resources/mixins.oneconfigv1.json")
        self.assertIn('method = "alignWithEntity"', camera)
        self.assertIn("ordinal = 1", camera)
        self.assertIn('method = "turn"', look)
        self.assertNotIn("Mixin_ReConfigMouseCamera", mixins)

    def test_gpu_preloader_does_not_create_offscreen_compose_scene(self):
        preloader = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/compose/ComposePreloader.kt")
        font = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/compose/SkiaFontRenderer.kt")
        self.assertNotIn("CanvasLayersComposeScene", preloader)
        self.assertNotIn("Surface.makeRenderTarget", preloader)
        self.assertNotIn("preloadGpuWarmup()", font)

    def test_reach_indicator_is_a_persisted_hud_module(self):
        catalog = source("modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt")
        huds = source("minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ModuleHuds.kt")
        self.assertIn('ClientModule("reach_indicator"', catalog)
        self.assertIn("class ReachIndicatorHud", huds)
        self.assertIn("player.eyePosition.distanceTo", huds)
        self.assertIn('ModuleTextHud("reach_indicator"', huds)
