# ReConfig Functional Modules Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a standalone Fabric 1.21.11 ReConfig build with a persistent typed module engine, control-style key capture, real module hooks, and restored OneConfig/iOS 26 UI styling.

**Architecture:** A focused common module core owns descriptors, typed values, persistence, and key state. The Minecraft node supplies runtime adapters and Mixins. Compose screens render the same descriptors with existing OneConfig components and never implement gameplay behavior themselves.

**Tech Stack:** Java 21, Kotlin 2.3, Fabric 1.21.11, Fabric API, Mixin, Compose Multiplatform/Skia, Gson, Gradle/Stonecutter.

**Spec:** `docs/superpowers/specs/2026-08-28-reconfig-functional-modules-rebuild-design.md`

## Global Constraints

- All 18 requested modules have settings, a keybind, and a runtime adapter.
- Configuration survives a complete process restart and saves after every mutation.
- The full ReConfig wordmark stays in the in-game UI; the blue `R` replaces icon-sized OneConfig logo graphics only.
- Existing OneConfig visual assets/components remain the UI foundation.
- ReConfig remains standalone from a separately installed OneConfig mod.

---

### Task 1: Typed Settings and Persistence Core

**Files:**
- Replace: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleCatalog.kt`
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ModuleStore.kt`
- Test: `tests/test_reconfig_regressions.py`

**Interfaces:**
- Produces typed setting descriptors, `ModuleCatalog`, validated mutation, immediate persistence, reload, and key-capture state.

- [ ] Extend tests to require typed Boolean/number/choice/color/text/key descriptors, all 18 IDs, and immediate persistence.
- [ ] Run tests and observe descriptor/persistence failures.
- [ ] Implement the typed store, atomic save/reload, validation, and immutable values.
- [ ] Run tests and verify they pass.

### Task 2: Control-Style Key Capture

**Files:**
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/KeyCapture.kt`
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt`
- Modify: `minecraft/src/main/kotlin/dev/duv14/reconfig/modules/ReConfigModuleRuntime.kt`

**Interfaces:**
- Produces `beginCapture`, `capture`, `cancel`, `unbind`, and readable key labels.

- [ ] Add failing tests for next-key capture, Escape cancellation, Delete/Backspace unbind, and event consumption.
- [ ] Implement capture state and runtime dispatch precedence.
- [ ] Replace key text input with an animated capture button.
- [ ] Verify tests and Compose source checks.

### Task 3: Runtime Adapter Registry

**Files:**
- Replace: `minecraft/src/main/kotlin/dev/duv14/reconfig/modules/ReConfigModuleRuntime.kt`
- Create: `minecraft/src/main/kotlin/dev/duv14/reconfig/modules/ModuleAdapters.kt`
- Create: focused Mixins under `minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/`
- Modify: `minecraft/src/main/resources/mixins.oneconfigv1.json`

**Interfaces:**
- Consumes typed module snapshots and key states.
- Produces one named runtime adapter per requested module plus guarded Mixin entrypoints.

- [ ] Add failing descriptor tests requiring a runtime adapter for every module ID.
- [ ] Implement activation dispatch and adapters for chat, options, HUD, weather, camera, sounds, particles, entity visuals, names, waypoints, zoom, and crosshair.
- [ ] Add exact Mixins for render/input behavior not exposed through events.
- [ ] Verify adapter coverage, Mixin JSON, and source compilation where the Gradle toolchain is available.

### Task 4: OneConfig/iOS 26 Modules Grid

**Files:**
- Replace: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt`
- Reuse: existing cards, filters, settings controls, shapes, shadows, icons, and animations under `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/`
- Add: module icons under `modules/internal/src/main/resources/assets/oneconfig/reconfig/modules/`

**Interfaces:**
- Consumes ModuleCatalog descriptors and mutations.
- Produces a four-column OneConfig-style grid and dedicated settings editor.

- [ ] Add source tests rejecting generic numeric/key editors and requiring filters, four columns, blue footers, switches, sliders, dropdowns, color control, and key capture.
- [ ] Implement the Mods-style cards and native controls by adapting existing OneConfig components.
- [ ] Implement live module editor state, reset, enable, and back navigation.
- [ ] Verify source checks and Compose compilation where available.

### Task 5: Branding and General Settings

**Files:**
- Modify: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/Sidebar.kt`
- Restore: `modules/internal/src/main/resources/assets/oneconfig/brand/reconfig-logo.png`
- Keep: `bootstrap/src/main/resources/reconfig-icon.png` as the square blue `R`
- Modify icon references and general settings screen.

**Interfaces:**
- Produces correct wordmark/icon separation and styled persistent general settings.

- [ ] Add failing asset/reference tests for wordmark in Sidebar and blue `R` only in icon-sized references.
- [ ] Restore the wordmark and preserve upstream styling assets.
- [ ] Rebuild general Settings with switches/sliders/dropdowns/color controls and immediate persistence.
- [ ] Verify branding and settings tests.

### Task 6: Integration and Artifact

**Files:**
- Update: `README.md`, `BUILDING.md`
- Create: final source ZIP.

**Interfaces:**
- Produces deployable source with Gradle wrappers and exact user build instructions.

- [ ] Run pure regression/module tests.
- [ ] Run backend tests.
- [ ] Validate JSON, Mixin configuration, assets, archive entries, and Gradle project directories.
- [ ] Run `gradlew.bat clean buildAndCollect` on a networked Java 21 Windows environment and fix all compiler failures before claiming a JAR build.
- [ ] Package and integrity-test the final archive.

