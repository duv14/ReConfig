# ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
# See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]


class ReConfigRegressionTests(unittest.TestCase):
    def test_release_build_only_targets_minecraft_12111(self):
        settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
        workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")
        build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('both("26.2")', settings)
        self.assertIn("buildAndCollect", workflow)
        self.assertIn(':bootstrap:1.21.11-fabric', build)

    def test_global_search_is_not_hard_disabled(self):
        shell = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/shell/Shell.kt").read_text(encoding="utf-8")
        self.assertNotIn("val isSearching = false", shell)
        modules = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt").read_text(encoding="utf-8")
        results = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/SearchResultsScreen.kt").read_text(encoding="utf-8")
        self.assertIn("ReConfigModuleSearchResults", modules)
        self.assertIn("ReConfigModuleSearchResults(normalized)", results)
        self.assertNotIn("SearchCorpus.search", results)

    def test_legacy_huds_respect_availability(self):
        renderer = (ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/hud/LegacyHudRenderer.kt").read_text(encoding="utf-8")
        self.assertIn("hud.isAvailable()", renderer)

    def test_waila_includes_registry_id(self):
        huds = (ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ModuleHuds.kt").read_text(encoding="utf-8")
        self.assertIn("BuiltInRegistries.BLOCK.getKey", huds)
    def test_runtime_mixins_are_registered_and_present(self):
        import json
        resources = ROOT / "minecraft/src/main/resources"
        entries = json.loads((resources / "mixins.oneconfigv1.json").read_text())["client"]
        expected = ["LookInput", "Camera", "Fog", "Particles", "Precipitation", "HitState", "Living", "Attack", "Crosshair", "ProjectileUse", "ProjectileConfirm", "MotionBlur"]
        for name in expected:
            qualified = "reconfig.Mixin_ReConfig" + name
            self.assertIn(qualified, entries)
            self.assertTrue((ROOT / "minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin" / (qualified.replace(".", "/") + ".java")).exists())

    def test_hud_providers_and_visibility_are_wired(self):
        runtime = (ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ReConfigModuleRuntime.kt").read_text()
        self.assertIn("ModuleHuds.register()", runtime)
        self.assertIn("ModuleHuds.tick(mc)", runtime)
        huds = (ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ModuleHuds.kt").read_text()
        self.assertIn("class ItemCounterHud", huds)
        self.assertIn("class WailaHud", huds)
        manager = (ROOT / "modules/hud/src/main/kotlin/org/polyfrost/oneconfig/api/hud/v1/HudManager.kt").read_text()
        self.assertIn("if (!hud.isAvailable()) return false", manager)

    def test_gameplay_sound_resources_exist(self):
        import json
        resource = ROOT / "minecraft/src/main/resources/assets/reconfig"
        events = json.loads((resource / "sounds.json").read_text())
        for category in ["eating", "hits", "wind_charges", "mace_hits", "shield_break"]:
            self.assertIn("gameplay." + category, events)
            for entry in events["gameplay." + category]["sounds"]:
                name = entry if isinstance(entry, str) else entry["name"]
                namespace, path = name.split(":", 1)
                self.assertEqual("reconfig", namespace)
                sound = resource / "sounds" / (path + ".ogg")
                self.assertEqual(b"OggS", sound.read_bytes()[:4])

    def test_collector_targets_only_requested_minecraft(self):
        build = (ROOT / "build.gradle.kts").read_text()
        self.assertIn('it.path == ":bootstrap:1.21.11-fabric"', build)
        self.assertNotIn('dependsOn(":bootstrap:assembleAllNodes")', build)

    def test_particle_opacity_uses_blended_layer(self):
        source = (ROOT / "minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigParticles.java").read_text()
        self.assertIn("SingleQuadParticle.Layer.TRANSLUCENT", source)
        self.assertIn("EffectMath.alpha", source)

    def test_distributable_jar_uses_reconfig_name(self):
        bootstrap_build = (ROOT / "buildSrc/src/main/kotlin/oneconfig-bootstrap.gradle.kts").read_text()
        self.assertIn('base.archivesName.set("ReConfig-${project.name}")', bootstrap_build)
        self.assertNotIn('base.archivesName.set("OneConfig-${project.name}")', bootstrap_build)

    def test_hidden_add_friend_dialog_does_not_create_focusable_popup(self):
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        self.assertIn("if (popupMounted) {", social)
        self.assertIn("Popup(", social.split("if (popupMounted) {", 1)[1])

    def test_module_changes_are_flushed_to_persistent_storage(self):
        catalog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt").read_text()
        self.assertIn("prefs.flush()", catalog)

    def test_enabled_persistence_helper_does_not_clash_with_kotlin_property_setter(self):
        catalog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt").read_text()
        self.assertNotIn("fun setEnabled(", catalog)
        self.assertIn("fun updateEnabled(", catalog)

    def test_runtime_assets_no_longer_reference_oneconfig_logo(self):
        referenced = []
        for path in ROOT.rglob("*"):
            if not path.is_file() or any(part in {".git", ".gradle", "build", "node_modules"} for part in path.parts):
                continue
            if path.suffix.lower() not in {".kt", ".java", ".json", ".info"}:
                continue
            if "oneconfig-icon.svg" in path.read_text(errors="ignore"):
                referenced.append(str(path.relative_to(ROOT)))
        self.assertEqual([], referenced)

    def test_sidebar_uses_reconfig_wordmark(self):
        sidebar = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/Sidebar.kt").read_text()
        self.assertIn("reconfig-wordmark.png", sidebar)

    def test_module_page_is_a_real_four_column_card_grid(self):
        screen = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt").read_text()
        self.assertIn("GridCells.Fixed(4)", screen)
        self.assertIn("filteredModules", screen)
        self.assertIn("SliderControl", screen)
        self.assertIn("SwitchControl", screen)

    def test_key_capture_is_controls_style(self):
        capture = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/KeyCapture.kt").read_text()
        self.assertIn("GLFW_KEY_ESCAPE", capture)
        self.assertIn("GLFW_KEY_BACKSPACE", capture)
        self.assertIn("settingId", capture)
        self.assertIn("setKey(settingId", capture)

    def test_catalog_contains_requested_modules_and_each_has_a_toggle_keybind(self):
        catalog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt").read_text()
        ids = []
        for line in catalog.splitlines():
            marker = 'ClientModule("'
            if marker in line:
                ids.append(line.split(marker, 1)[1].split('"', 1)[0])
        self.assertEqual(35, len(ids))
        self.assertEqual(len(ids), len(set(ids)))
        module_lines = [line for line in catalog.splitlines() if 'ClientModule("' in line]
        self.assertTrue(all("key(" in line for line in module_lines))
        self.assertNotIn('ClientModule("pvp_info"', catalog)
        self.assertIn('ClientModule("zoom"', catalog)
        for module in ('team_highlight', 'cps', 'fps', 'keystrokes', 'armor_status', 'effect_status',
                       'coordinates', 'combo_counter', 'inventory_hud', 'memory_monitor', 'server_status',
                       'fullbright', 'toggle_sprint', 'toggle_sneak', 'streamer_mode'):
            self.assertIn(module, ids)
        self.assertIn('ClientModule("wind_charge_optimizer"', catalog)
        self.assertIn('ClientModule("pearl_optimizer"', catalog)

    def test_requested_modules_no_longer_use_placeholder_settings(self):
        catalog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt").read_text()
        self.assertNotIn("placeholders(", catalog)
        expected = {
            "better_sounds": ["eating", "hits", "wind_charges", "mace_hits"],
            "fog": ["fog_distance", "fog_opacity"],
            "fov": ["fov"],
            "hit_color": ["flash_duration", "hit_color"],
            "motion_blur": ["strength", "responsiveness"],
            "nick_hider": ["nickname", "hide_nickname"],
            "particles": ["particle_opacity", "particle_size"],
            "weather": ["time", "weather"],
            "freelook": ["mode"],
            "crosshair": ["shape", "length", "gap", "thickness", "crosshair_color"],
        }
        for module_id, setting_ids in expected.items():
            line = next(line for line in catalog.splitlines() if f'ClientModule("{module_id}"' in line)
            for setting_id in setting_ids:
                self.assertIn(f'"{setting_id}"', line)

    def test_auto_text_has_three_real_persistent_settings(self):
        catalog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt").read_text()
        auto_text = next(line for line in catalog.splitlines() if 'ClientModule("auto_text"' in line)
        self.assertIn('key("toggle_key","Enable / disable keybind"', auto_text)
        self.assertIn('key("send_key","Send message keybind"', auto_text)
        self.assertIn('text("message","Message"', auto_text)
        self.assertNotIn("placeholders(", auto_text)

    def test_key_capture_is_wired_into_reconfig_screen(self):
        screen = (ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/compose/impls/OneConfigUIScreen.kt").read_text()
        self.assertIn("KeyCapture.consume(key, 1)", screen)

    def test_module_runtime_uses_edge_triggered_keys_and_auto_text_chat(self):
        runtime_path = ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ReConfigModuleRuntime.kt"
        auto_path = ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/AutoTextController.kt"
        entry = (ROOT / "minecraft/src/main/java/org/polyfrost/oneconfig/internal/OneConfig.java").read_text()
        self.assertTrue(runtime_path.exists())
        self.assertTrue(auto_path.exists())
        runtime = runtime_path.read_text()
        auto = auto_path.read_text()
        self.assertIn("ClientTickEvents.END_CLIENT_TICK.register", runtime)
        self.assertIn("previouslyDown", runtime)
        self.assertIn('module.key("toggle_key")', runtime)
        self.assertIn('module.key("send_key")', runtime)
        self.assertIn("KeyCapture.moduleId", runtime)
        self.assertIn("mc.screen != null", runtime)
        self.assertIn('setting.id == "message"', auto)
        self.assertIn("connection.sendChat(message)", auto)
        self.assertIn("ReConfigModuleRuntime.start()", entry)

    def test_fov_hurt_cam_weather_and_hitboxes_have_runtime_hooks(self):
        base = ROOT / "minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig"
        mixins = (ROOT / "minecraft/src/main/resources/mixins.oneconfigv1.json").read_text()
        runtime = (ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ReConfigModuleRuntime.kt").read_text()
        for filename, registration in (
            ("Mixin_ReConfigFov.java", "reconfig.Mixin_ReConfigFov"),
            ("Mixin_ReConfigHurtCam.java", "reconfig.Mixin_ReConfigHurtCam"),
            ("Mixin_ReConfigWeather.java", "reconfig.Mixin_ReConfigWeather"),
        ):
            self.assertTrue((base / filename).exists())
            self.assertIn(registration, mixins)
        fov_mixin = (base / "Mixin_ReConfigFov.java").read_text()
        self.assertIn("CallbackInfoReturnable<Float>", fov_mixin)
        self.assertIn("ModuleAccess.number", fov_mixin)
        self.assertIn("syncHitboxes", runtime)
        self.assertIn("DebugScreenEntries.ENTITY_HITBOXES", runtime)
        self.assertIn("mc.debugEntries", runtime)
        self.assertIn("entries.isCurrentlyEnabled", runtime)
        self.assertIn("entries.setStatus", runtime)
        self.assertNotIn("shouldRenderHitBoxes", runtime)
        self.assertNotIn("setRenderHitBoxes", runtime)

    def test_module_cards_open_settings_without_toggling(self):
        screen = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt").read_text()
        card = screen.split("@Composable private fun ModuleCard", 1)[1].split("@Composable private fun ModuleEditor", 1)[0]
        self.assertIn("onClick(s){module.toggle()}", card)
        self.assertIn("Module settings", card)
        self.assertIn("open()", card)
        self.assertIn("tween(180, easing = FastOutSlowInEasing)", card)
        self.assertIn("1.006f", card)
        self.assertIn("animateColorAsState", card)
        self.assertIn("reConfigGlass(t.modCardShape", card)

    def test_add_friend_popup_has_real_visible_lifecycle(self):
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        friends = social.split("fun FriendsScreen()", 1)[1].split("private fun FriendRow", 1)[0]
        self.assertIn("var popupMounted", friends)
        self.assertIn("visible = dialog", friends)
        self.assertIn("delay(190)", friends)
        self.assertIn("slideInVertically", friends)

    def test_social_tracks_new_messages_and_uses_notifications(self):
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        self.assertIn("initializedConversations", social)
        self.assertIn("Notifications.info(friend.name", social)
        self.assertIn("animateScrollToItem", social)

    def test_social_messages_are_sender_aligned_deletable_and_scroll_to_bottom(self):
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        self.assertIn("val sender: String", social)
        self.assertIn("if (line.mine) Arrangement.End else Arrangement.Start", social)
        self.assertIn("if(line.mine)Accent", social)
        self.assertIn("line.sender", social)
        self.assertIn('MessageDeleteButton', social)
        self.assertIn('entrance.animateTo(1f, tween(280, easing = EaseOutCubic))', social)
        self.assertIn('translationY = (1f - entrance.value) * 12.dp.toPx()', social)
        self.assertNotIn('SocialButton("trash"', social)
        self.assertIn("deleteMessage(line.id)", social)
        self.assertIn("identityUuid", social)
        self.assertIn('recipientId == friendId', social)
        self.assertIn('senderId != friendId', social)
        self.assertIn('if(target.toList()!=parsed)', social)
        self.assertIn("Accent.copy(.34f)", social)
        self.assertIn("rememberLazyListState", social)
        self.assertIn("animateScrollToItem(lines.lastIndex)", social)
        self.assertNotIn("AnimatedContent(targetState=lines.toList()", social)

    def test_social_can_cancel_requests_and_reads_active_server_directly(self):
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        screen = (ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/compose/impls/OneConfigUIScreen.kt").read_text()
        self.assertIn('post("/v2/friends/cancel"', social)
        self.assertIn('"Cancel Request"', social)
        self.assertIn("destructive = true", social)
        self.assertIn("mc.currentServer?.ip", screen)

    def test_shared_ios_glass_style_is_used_across_reconfig_surfaces(self):
        glass_path = ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/ReConfigGlass.kt"
        modules = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt").read_text()
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        self.assertTrue(glass_path.exists())
        glass = glass_path.read_text()
        self.assertIn("Brush.verticalGradient", glass)
        self.assertIn("reConfigGlass", modules)
        self.assertIn("reConfigGlass", social)
        self.assertIn("edge.copy(alpha = .28f)", glass)
        self.assertNotIn("edge.copy(alpha = .82f)", glass)

    def test_settings_controls_use_the_soft_shared_outline(self):
        modules = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt").read_text()
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        self.assertGreaterEqual(modules.count("reConfigGlass("), 7)
        self.assertIn("SocialInput", social)
        social_input = social.split("private fun SocialInput", 1)[1]
        self.assertIn("reConfigGlass(", social_input)
        slider = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/settings/SliderControl.kt").read_text()
        self.assertIn(".border(1.dp", slider)

    def test_each_module_has_a_custom_reconfig_svg_icon(self):
        catalog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt").read_text()
        icon_dir = ROOT / "modules/internal/src/main/resources/assets/oneconfig/ico/reconfig-modules"
        import re
        icons = re.findall(r'ClientModule\("[^"]+","[^"]+","[^"]+","([^"]+)"', catalog)
        self.assertTrue(icons)
        for name in icons:
            with self.subTest(icon=name):
                self.assertEqual(ET.parse(icon_dir / (name + ".svg")).getroot().tag, "{http://www.w3.org/2000/svg}svg")

    def test_schema_repair_uses_a_new_migration_filename(self):
        migration = ROOT / "backend/migrations/0003_social_schema_repair.sql"
        self.assertTrue(migration.exists())
        source = migration.read_text()
        for table in ("sessions", "friendships", "presence", "messages_v2", "invitations"):
            self.assertIn(f"CREATE TABLE IF NOT EXISTS {table}", source)

    def test_updates_category_routes_to_remote_changelog(self):
        routes = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/navigation/Routes.kt").read_text()
        navigation = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/navigation/Navigation.kt").read_text()
        self.assertIn('id = "Updates"', routes)
        self.assertIn('id = "changelog"', routes)
        self.assertIn("ChangeLogGraph", routes)
        self.assertIn("changeLogGraph()", navigation)

    def test_social_registration_starts_with_minecraft_not_the_ui(self):
        entry = (ROOT / "minecraft/src/main/java/org/polyfrost/oneconfig/internal/OneConfig.java").read_text()
        bootstrap_path = ROOT / "minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ReConfigSocialBootstrap.kt"
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        self.assertTrue(bootstrap_path.exists())
        bootstrap = bootstrap_path.read_text()
        self.assertIn("ReConfigSocialBootstrap.start()", entry)
        self.assertIn("Minecraft.getInstance()", bootstrap)
        self.assertIn("ReConfigSocialService.start(name, uuid)", bootstrap)
        self.assertIn("object ReConfigSocialService", social)
        self.assertIn("fun start(name:String,uuid:String)", social)

    def test_friend_heads_resolve_by_minecraft_username_before_uuid(self):
        heads = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/PlayerHead.kt").read_text()
        social = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt").read_text()
        self.assertIn("fun PlayerHead(username: String, uuid: String", heads)
        self.assertIn('listOf(username, uuid.replace("-", ""))', heads)
        self.assertIn("PlayerHead(friend.name,friend.uuid", social)

    def test_changelog_fetches_domain_and_caches_last_success(self):
        changelog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/Changelog.kt").read_text()
        self.assertIn("https://duv14.com/changelogsreconfig", changelog)
        self.assertIn('class="changelog"', changelog)
        self.assertIn("withContext(Dispatchers.IO)", changelog)
        self.assertIn("changelog-cache.html", changelog)
        self.assertNotIn("ONECONFIG UPDATE", changelog)

    def test_changelog_cannot_remain_stuck_loading_and_can_retry(self):
        changelog = (ROOT / "modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/Changelog.kt").read_text()
        self.assertIn("suspend fun refresh(force: Boolean = false)", changelog)
        self.assertIn("finally {", changelog)
        self.assertIn("loading = false", changelog.split("finally {", 1)[1])
        self.assertIn("Duration.ofSeconds(5)", changelog)
        self.assertIn("RemoteChangelogs.refresh(force = true)", changelog)
        self.assertIn('Text("Retry"', changelog)

    def test_standalone_changelog_html_has_editable_articles(self):
        html = ROOT / "changelogsreconfig.html"
        self.assertTrue(html.exists())
        root = ET.parse(html).getroot()
        entries = [node for node in root.iter() if node.tag.endswith("article") and node.attrib.get("class") == "changelog"]
        self.assertGreaterEqual(len(entries), 2)
        for entry in entries:
            descendants = list(entry.iter())
            self.assertTrue(any(node.tag.endswith("img") for node in descendants))
            self.assertTrue(any(node.tag.endswith("h2") for node in descendants))
            self.assertTrue(any(node.tag.endswith("div") and node.attrib.get("class") == "description" for node in descendants))


if __name__ == "__main__":
    unittest.main()
