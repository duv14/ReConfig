from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

def source(path):
    return (ROOT / path).read_text(encoding='utf-8')

class RequestedModulesTest(unittest.TestCase):
    def test_requested_module_cards_exist(self):
        catalog = source('modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt')
        ids = [
            'target_hud','speedometer','light_level','clock_hud','durability_alerts',
            'low_health_warning','projectile_counter','session_stats','auto_gg','chat_timestamps',
            'shulker_tooltip','totem_pop_customizer','browser_music_hud','reach_indicator','freelook'
        ]
        for module_id in ids:
            self.assertIn(f'ClientModule("{module_id}"', catalog)

    def test_requested_huds_are_registered_with_editor(self):
        huds = source('minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ModuleHuds.kt')
        for cls in ['TargetHud','SpeedometerHud','LightLevelHud','ClockHud','DurabilityAlertHud',
                    'LowHealthHud','ProjectileCounterHud','SessionStatsHud','TotemPopHud','BrowserMusicHud']:
            self.assertIn(f'class {cls}', huds)
            self.assertIn(f'{cls}()', huds)
        self.assertIn('ReConfigHudVisibility.gameplayOverlayVisible()', huds)

    def test_qol_runtime_features_are_real_not_placeholder_cards(self):
        qol = source('minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/QualityOfLifeModules.kt')
        self.assertIn('ItemTooltipCallback.EVENT.register', qol)
        self.assertIn('DataComponents.CONTAINER', qol)
        self.assertIn('ClientboundEntityEventPacket', qol)
        self.assertIn('packet.eventId.toInt() == 35', qol)
        self.assertIn('sendChat(message)', qol)
        self.assertIn('Component.text("[$stamp] ").append(event.message)', qol)

    def test_pause_menu_entry_and_blue_default_accent(self):
        pause = source('minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/PauseMenuIntegration.kt')
        config = source('modules/internal/src/main/java/org/polyfrost/oneconfig/internal/OneConfigConfig.java')
        theme = source('modules/internal/src/main/java/org/polyfrost/oneconfig/internal/ThemeConfig.java')
        self.assertIn('screen !is PauseScreen', pause)
        self.assertIn('Component.literal("◆ ReConfig")', pause)
        self.assertIn('0xFF00A6FF', config)
        self.assertIn('0xFF00A6FF', theme)

    def test_hud_api_snapshot_includes_availability(self):
        api = source('modules/hud/api/hud.api')
        self.assertIn('public fun isAvailable ()Z', api)

    def test_release_build_skips_the_unsupported_26_2_binary_nodes(self):
        build = source('build.gradle.kts')
        self.assertIn('unsupportedBinaryNodes', build)
        self.assertIn('":minecraft:26.2-fabric"', build)
        self.assertIn('":bootstrap:26.2-fabric"', build)
        self.assertIn('tasks.configureEach { enabled = false }', build)
