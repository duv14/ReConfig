# Working Modules, Social, and UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a standalone Fabric 1.21.11 ReConfig build whose requested modules, HUDs, changelog, social actions, and iOS-style UI are functional and persistent.

**Architecture:** Keep `ModuleCatalog` as persistent UI state, add a Minecraft-side edge-triggered runtime, and isolate feature behavior into focused controllers and mixins. Keep social state in the existing Cloudflare Worker/D1 service, add only authenticated endpoints, and share reusable Compose glass styling across screens.

**Tech Stack:** Kotlin/JVM, Java 21, Fabric 1.21.11, Mojang mappings, Compose Multiplatform/Skiko, Mixins, Cloudflare Workers, D1 SQLite, Node test runner, Python `unittest` source regressions.

**Spec:** `docs/superpowers/specs/2026-08-29-working-modules-social-ui-design.md`

## Global Constraints

- Standalone Fabric mod for Minecraft 1.21.11; no OneConfig installation dependency.
- Final archive base is `ReConfig`, with `ReConfig-fabric12111-1.0.0.jar` collected by `buildAndCollect`.
- Module state, settings, HUD layout, waypoints, and social identity persist across restarts.
- Runtime changes are client-only and restore vanilla state when disabled or disconnected.
- Cloudflare endpoint remains hardcoded as `https://reconfig-chat.duv14-reconfig-api.workers.dev`.
- New sound assets must explicitly allow redistribution and must be listed in `ATTRIBUTIONS.md`.

---

### Task 1: Changelog State Machine

**Files:**
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/Changelog.kt`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: `RemoteChangelogs.refresh(force: Boolean = false)` and finite loading/error/retry states.

- [ ] Add regressions asserting cache-first loading, `finally { loading=false }`, forced retry, response timeout, and a Retry button.
- [ ] Run the focused Python tests and confirm they fail because refresh is currently one-shot and does not use `finally`.
- [ ] Refactor refresh to read cache first, use bounded HTTP fetch, reject empty parsed feeds, always clear loading in `finally`, and support `force=true`.
- [ ] Render cached data during refresh and render a styled Retry action on empty/error.
- [ ] Run all Python regressions and commit the changelog slice.

### Task 2: Typed Settings and Key Capture

**Files:**
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt`
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/KeyCapture.kt`
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: `ClientModule.key(id: String): Int`, `setKey(id: String, key: Int)`, and stable IDs `toggle_key`, `send_key`, `message`.

- [ ] Add tests for multiple key settings, exact Auto Text settings, controls-style capture by `(moduleId, settingId)`, and persistent `set` calls.
- [ ] Confirm tests fail against the single-key placeholder implementation.
- [ ] Generalize key helpers/capture to setting IDs and update the settings renderer so every key row records independently.
- [ ] Replace Auto Text placeholders with its three exact settings.
- [ ] Run regressions and commit.

### Task 3: Edge-Triggered Module Runtime and Auto Text

**Files:**
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ReConfigModuleRuntime.kt`
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/AutoTextController.kt`
- Modify: `minecraft/src/main/java/org/polyfrost/oneconfig/internal/OneConfig.java`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: `ReConfigModuleRuntime.start()`, rising-edge key dispatch, and `AutoTextController.onSend(mc)`.

- [ ] Add source regressions requiring startup registration, rising-edge state, focus guards, toggle handling, and normal player chat connection send.
- [ ] Confirm failure because the runtime is currently disconnected.
- [ ] Register a client tick listener, track prior key states, ignore key capture/screens, toggle modules, and dispatch actions.
- [ ] Implement Auto Text validation and signed chat send through the active player connection.
- [ ] Run regressions and commit.

### Task 4: Core View Modules

**Files:**
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ViewModuleController.kt`
- Create: `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigFog.java`
- Create: `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigFov.java`
- Create: `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigHurtCam.java`
- Modify: `minecraft/src/main/resources/mixins.oneconfigv1.json`
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Consumes: runtime module state.
- Produces: fog distance/opacity, FOV 30–180, hurt-cam suppression, and synchronized vanilla hitboxes.

- [ ] Add exact setting and mixin regressions for Fog, FOV, Hitbox, and Hurt Cam.
- [ ] Confirm failure against placeholders and absent mixins.
- [ ] Add typed settings and narrowly targeted render/game-renderer injections.
- [ ] Mirror vanilla entity hitbox render state bidirectionally from the tick controller.
- [ ] Run regressions and commit.

### Task 5: Render Effects

**Files:**
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/HitColorController.kt`
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/ParticleController.kt`
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/MotionBlurController.kt`
- Create: `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigEntityColor.java`
- Create: `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigParticles.java`
- Create: `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigMotionBlur.java`
- Modify: `minecraft/src/main/resources/mixins.oneconfigv1.json`
- Modify: `ModuleCatalog.kt`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: timed entity hit overlays, particle alpha/scale, and resettable frame accumulation.

- [ ] Add tests for exact settings and render hooks, including resize/world-change blur reset.
- [ ] Confirm failure.
- [ ] Implement controllers and mixins with no-op guards when render/world state is unavailable.
- [ ] Add HSV+alpha color wheel UI and persist ARGB values.
- [ ] Run regressions and commit.

### Task 6: Privacy, Weather, Crosshair

**Files:**
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/PrivacyWeatherController.kt`
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/CrosshairController.kt`
- Create corresponding focused mixins under `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/`
- Modify: `ModuleCatalog.kt`, `ReConfigModules.kt`, `mixins.oneconfigv1.json`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: local nameplate replacement/hiding, client time/weather override, particle-independent snow mode, and custom crosshair renderer/editor.

- [ ] Add setting/hook/editor regressions and confirm they fail.
- [ ] Implement nickname, weather/time, and vanilla-crosshair suppression hooks.
- [ ] Implement persisted live crosshair editor for shape, dimensions, outline, ARGB, dot, and spread.
- [ ] Run regressions and commit.

### Task 7: HUD Registry, Item Counter, WAILA

**Files:**
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/HudLayoutStore.kt`
- Create: `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ReConfigHudRuntime.kt`
- Modify existing Edit HUD routes/components to expose ReConfig HUD elements.
- Modify: `ModuleCatalog.kt`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: versioned persisted `HudLayout(x, y, scale)`, drag/scale editor, Item Counter, and WAILA elements.

- [ ] Add tests for both HUD registrations, notices, atomic persistence, drag, and scale.
- [ ] Confirm failure.
- [ ] Implement registry/store and connect it to Edit HUD.
- [ ] Render held-stack total and crosshair target localized name/type.
- [ ] Run regressions and commit.

### Task 8: Freelook, Trajectory Helpers, and Waypoints

**Files:**
- Create focused runtime files under `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/` for Freelook, projectile trajectories, and waypoints.
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/WaypointStore.kt`
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigWaypoints.kt`
- Modify routes, catalog, runtime, and mixins.
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: safe perspective restoration, wind-charge/pearl trajectory renderers, and server/world/dimension-scoped waypoint CRUD/rendering.

- [ ] Add regressions for Hold/Toggle Freelook, perspective restoration, non-automating trajectory controllers, waypoint fields, atomic store, and manager route.
- [ ] Confirm failure.
- [ ] Implement Freelook camera detachment and cleanup.
- [ ] Implement physics previews/cooldown indicators without input automation.
- [ ] Implement waypoint manager, world labels, distance, edge indicators, filtering, and optional death points.
- [ ] Run regressions and commit.

### Task 9: Better Sounds and Catalog Cleanup

**Files:**
- Add OGG assets under `modules/internal/src/main/resources/assets/oneconfig/sounds/reconfig/`.
- Modify sound registration/runtime, `ModuleCatalog.kt`, icon assets, and `ATTRIBUTIONS.md`.
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: independently toggleable eating, hit, wind-charge, and mace-hit sounds.

- [ ] Select only explicitly redistributable source files and record author, source URL, and license.
- [ ] Add tests for four sound toggles, sound registrations, attribution entries, and absence of PvP Info/Zoom.
- [ ] Confirm failure.
- [ ] Normalize/convert assets to OGG and hook local sound events.
- [ ] Remove PvP Info and Zoom from catalog/search/assets.
- [ ] Run regressions and commit.

### Task 10: Social Backend Actions

**Files:**
- Create: `backend/migrations/0004_message_deletion.sql`
- Modify: `backend/src/worker.js`
- Modify: `backend/test/worker.test.js`

**Interfaces:**
- Produces: `POST /v2/friends/cancel` and authenticated `DELETE /v2/messages/:id` restricted to sender ownership.

- [ ] Add Node tests for outgoing cancel, forbidden foreign deletion, own deletion, and conversation removal.
- [ ] Confirm failures.
- [ ] Add the migration/index and endpoints with ownership checks.
- [ ] Run Node tests and commit.

### Task 11: Social Client and Conversation Layout

**Files:**
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt`
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/navigation/graph/ReConfigClient.kt`
- Modify invitation address resolver in the Minecraft integration.
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Consumes: cancel/delete endpoints.
- Produces: sender-aware bubbles, correct bottom scroll, red cancellation, delete bin, and active multiplayer address invites.

- [ ] Add regressions for sender UUID branching, right/blue local bubbles, left/gray remote bubbles, local-only bin, reverse-scroll removal, and active connection address.
- [ ] Confirm failure.
- [ ] Implement client API calls and stable message keys.
- [ ] Correct conversation auto-scroll rules and invitation detection.
- [ ] Run regressions and commit.

### Task 12: Shared iOS Glass Styling and Final Verification

**Files:**
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/ReConfigGlass.kt`
- Modify module, setting, friend, dialog, changelog, and social UI components.
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces: reusable gradient border/surface modifier and shared motion timings.

- [ ] Add regressions requiring shared modifier reuse and semantic destructive colors.
- [ ] Confirm failure.
- [ ] Implement light-top/dark-bottom glass border and apply it consistently without flattening existing shapes.
- [ ] Run Python and Node suites plus `git diff --check`.
- [ ] Run `gradlew.bat clean buildAndCollect` under Java 21 where dependency downloads are available.
- [ ] Package source, backend migration/deploy instructions, and the collected ReConfig JAR.

