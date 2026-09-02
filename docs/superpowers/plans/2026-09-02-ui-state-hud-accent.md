# ReConfig UI State, HUD Background, and Accent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two-hour page memory, closable Hitbox Categories, optional HUD panels, and persisted accent-driven UI/logo coloring.

**Architecture:** Keep navigation memory in a small persisted route-state object consumed by Shell. Put common HUD panel visibility in the shared ReConfig HUD base. Store accent in the existing ReConfig config tree and derive theme/logo color from it.

**Tech Stack:** Kotlin, Java, Compose Multiplatform, OneConfig configuration/HUD APIs, Python source regressions.

**Spec:** `docs/superpowers/specs/2026-09-02-ui-state-hud-accent-design.md`

## Global Constraints

- Default accent is #406CAB.
- Page memory expires after two hours and never restores a module-detail route.
- Zoom receives no HUD-background option.
- Existing saved module and UI configuration remains compatible.

---

### Task 1: Persist Top-Level Navigation

**Files:** Modify `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/shell/Shell.kt`; create a focused route-memory source; test in `tests/test_v315_suggestions.py`.

- [ ] Write and run a failing source regression for route normalization and two-hour expiry.
- [ ] Implement persisted route id/timestamp and restore it as Shell's start destination.
- [ ] Record sidebar navigation without recording detail screens.
- [ ] Run the regression.

### Task 2: HUD Background Controls

**Files:** Modify `minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/modules/VisualModuleHuds.kt`; test in `tests/test_v315_suggestions.py`.

- [ ] Write and run a failing regression requiring a Show Background option on each ReConfig HUD.
- [ ] Add the shared persisted option and conditionally draw fill/outline.
- [ ] Verify Zoom is excluded and run tests.

### Task 3: Hitbox Editor Dismissal

**Files:** Modify `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/CombatEditors.kt`; test in `tests/test_v315_suggestions.py`.

- [ ] Write and run a failing regression for visible close/back and Escape behavior.
- [ ] Implement navigation back to Modules.
- [ ] Run the regression.

### Task 4: Native Accent and Logo Tint

**Files:** Modify `modules/internal/src/main/java/org/polyfrost/oneconfig/internal/OneConfigConfig.java`, theme sources, and logo rendering; test in `tests/test_v315_suggestions.py`.

- [ ] Write and run failing regressions for #406CAB persistence and dynamic logo tint.
- [ ] Add the native color preference and connect it to the animated theme.
- [ ] Tint the wordmark through the configured accent without replacing its shape.
- [ ] Run regressions.

### Task 5: Verify and Package

**Files:** Update source ZIP.

- [ ] Run every Python and Node regression.
- [ ] Run Gradle build when available; otherwise report the blocked distribution download accurately.
- [ ] Validate and package the source archive.
