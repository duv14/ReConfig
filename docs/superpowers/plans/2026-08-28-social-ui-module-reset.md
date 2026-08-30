# Social UI and Module Reset Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair social storage and messaging, refine animations and module visuals, add custom module icons, and temporarily remove module runtime behavior.

**Architecture:** Keep the existing Compose/Skia shell and Cloudflare Worker, but isolate schema repair, polling notifications, card interaction, and icon resources. Module state remains persistent while all Minecraft behavior adapters are disconnected.

**Tech Stack:** Kotlin, Compose Multiplatform, Fabric 1.21.11, SVG resources, Cloudflare Workers, D1, Node tests, Python regression tests.

**Spec:** `docs/superpowers/specs/2026-08-28-social-ui-module-reset-design.md`

## Global Constraints

- Standalone Fabric 1.21.11 mod; no external OneConfig installation.
- No visible OneConfig branding outside required license attribution.
- Backend URL remains hardcoded.
- All UI state and module placeholder values persist across restart.

---

### Task 1: Repair production social schema

**Files:** Create `backend/migrations/0003_social_schema_repair.sql`; modify `backend/src/worker.js`; test `backend/test/worker.test.js`.

- [ ] Add a failing test for reciprocal requests and safe structured server errors.
- [ ] Add idempotent table/index creation under a new migration filename.
- [ ] Isolate friend, message, and invitation refresh failures.
- [ ] Run `npm test --prefix backend`.

### Task 2: Add message notification delivery

**Files:** Modify `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt`; test `tests/test_reconfig_regressions.py`.

- [ ] Add failing source invariants for message ID tracking and notification calls.
- [ ] Suppress initial-history notifications and notify only for newly received remote messages.
- [ ] Run regression tests.

### Task 3: Refine social and module animations

**Files:** Modify `ReConfigSocial.kt` and `ReConfigModules.kt`; test `tests/test_reconfig_regressions.py`.

- [ ] Add failing checks for eased subtle hover, animated overlay, and animated message items.
- [ ] Implement full-shell overlay and eased transitions.
- [ ] Make module cards open details without toggling state and darken enabled styling.
- [ ] Run regression tests.

### Task 4: Reset module behavior and settings

**Files:** Modify `ModuleCatalog.kt`, `ReConfigModuleRuntime.kt`, `ReConfigHudModules.kt`, `OneConfig.java`, and mixin configuration.

- [ ] Add failing checks that runtime/HUD/mixins are disconnected.
- [ ] Replace module settings with persisted placeholder controls.
- [ ] Remove initialization, HUD rendering, and custom gameplay mixins.
- [ ] Run regression tests.

### Task 5: Create custom SVG icon family

**Files:** Create 18 SVG files under `modules/internal/src/main/resources/assets/oneconfig/ico/reconfig-modules/`; modify `Icon.kt` and `ModuleCatalog.kt`.

- [ ] Add a failing test requiring one unique resource per module.
- [ ] Create rounded 24x24 monochrome SVG symbols using `currentColor`-compatible black paths.
- [ ] Register every resource and verify no inherited fallback is needed.
- [ ] Run regression tests.

### Task 6: Verify and package

**Files:** Update `BUILDING.md`; create a versioned ZIP.

- [ ] Run Python and backend tests, JSON parsing, `git diff --check`, and SVG XML parsing.
- [ ] Verify archive integrity and record SHA-256.
- [ ] Save the ZIP and provide migration/build commands.
