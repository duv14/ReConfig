# ReConfig Client Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone Fabric 1.21.11 ReConfig client with reliable friends, same-server WhatsApp-style messaging, editable settings for every requested functional module, and fully ReConfig-branded navigation/settings.

**Architecture:** A typed module registry owns persistent module settings and lifecycle hooks; focused Fabric client hooks apply gameplay/render behavior. A typed social client talks to a versioned Worker API with explicit friendship state, presence, same-server authorization, message history, and invitations. Compose screens consume immutable state from these services.

**Tech Stack:** Java 21, Kotlin 2.3, Fabric 1.21.11, Compose Multiplatform, Mixin, Cloudflare Workers, D1, Node test runner, Gradle/Stonecutter.

**Spec:** `docs/superpowers/specs/2026-08-28-reconfig-client-expansion-design.md`

## Global Constraints

- Every module is functional and has an editable settings screen.
- Messaging is permitted only while both friends are online on the same normalized server.
- ReConfig is standalone at runtime and targets Fabric 1.21.11.
- Visible product branding is ReConfig; upstream legal notices remain intact.
- No continuous UI ambience plays while the menu is open.

---

### Task 1: Social API v2

**Files:**
- Create: `backend/migrations/0002_social_v2.sql`
- Replace: `backend/src/worker.js`
- Replace: `backend/test/worker.test.js`

**Interfaces:**
- Produces authenticated `/v2/friends`, `/v2/presence`, `/v2/messages`, `/v2/invitations`, and `/v2/events` JSON endpoints.

- [ ] Write failing in-memory D1 tests for request/accept, same-server messaging, rejection across servers, presence, history acknowledgement, and invitations.
- [ ] Run `cd backend && npm test` and verify the new behavior fails.
- [ ] Add the normalized v2 schema and Worker handlers with checked responses and idempotent operations.
- [ ] Run `cd backend && npm test` and verify all tests pass.

### Task 2: Typed Client Social Service

**Files:**
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/social/SocialModels.kt`
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/social/ReConfigSocialClient.kt`
- Test: `modules/internal/src/test/kotlin/org/polyfrost/oneconfig/internal/social/SocialStateReducerTest.kt`

**Interfaces:**
- Produces `SocialSnapshot`, `FriendState`, `Conversation`, `sendRequest`, `acceptRequest`, `declineRequest`, `removeFriend`, `sendMessage`, `invite`, and `heartbeat`.

- [ ] Write reducer tests proving errors do not create optimistic fake state and incoming requests become accepted only after server confirmation.
- [ ] Run the internal module test and observe expected failure.
- [ ] Implement per-account credentials, checked HTTP responses, snapshot polling, presence heartbeat, and error propagation.
- [ ] Run internal tests and verify they pass.

### Task 3: Social UI

**Files:**
- Replace: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSocial.kt`
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/components/PlayerHead.kt`

**Interfaces:**
- Consumes the Task 2 social client.
- Produces Friends and Messaging screens with full-shell modal state.

- [ ] Add UI-state tests for contextual friend actions and same-server composer eligibility.
- [ ] Implement cached UUID skin heads, presence dots, explicit request controls, Message/Invite buttons, unread conversation list, bubbles, timestamps, and full-shell add-friend modal.
- [ ] Verify UI-state tests pass and Compose sources compile.

### Task 4: Module Registry and Editable Configuration

**Files:**
- Create: `minecraft/src/main/kotlin/dev/duv14/reconfig/modules/Module.kt`
- Create: `minecraft/src/main/kotlin/dev/duv14/reconfig/modules/ModuleRegistry.kt`
- Create: `minecraft/src/main/kotlin/dev/duv14/reconfig/modules/ModuleSettings.kt`
- Create: `minecraft/src/test/kotlin/dev/duv14/reconfig/modules/ModuleRegistryTest.kt`

**Interfaces:**
- Produces stable module ids, enable state, keybind modes, typed Boolean/number/color/text/list settings, JSON persistence, lifecycle dispatch, and conflict reporting.

- [ ] Write failing serialization, clamping, keybind, and lifecycle tests.
- [ ] Implement the registry and settings types.
- [ ] Verify registry tests pass.

### Task 5: Functional Gameplay and Render Modules

**Files:**
- Create focused implementations under `minecraft/src/main/kotlin/dev/duv14/reconfig/modules/impl/`.
- Create focused hooks under `minecraft/src/main/java/dev/duv14/reconfig/mixin/`.
- Modify: `minecraft/src/main/resources/oneconfig.mixins.json`

**Interfaces:**
- Consumes Task 4 settings/lifecycle APIs.
- Produces functional Auto Text Hot Key, Better Sounds, Fog Customizer, FOV Changer, Hitbox, Hit Color, Hurt Cam, Item Counter, Motion Blur, Nick Hider, Particle Changer, PvP Info, WAILA, Weather Changer, Waypoints, Freelook, Zoom, and Custom Crosshair modules.

- [ ] Write pure behavior tests for interpolation, transforms, filtering, counters, key modes, waypoint scoping, crosshair geometry, and freelook restoration.
- [ ] Implement each module with its complete settings schema and scoped Fabric/Mixin hook.
- [ ] Run module tests and compile the 1.21.11 target.

### Task 6: Modules and Settings UI

**Files:**
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigModules.kt`
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/screens/ReConfigSettings.kt`
- Create: `modules/internal/src/main/kotlin/org/polyfrost/oneconfig/internal/ui/navigation/graph/ReConfigClient.kt`
- Modify navigation, routes, sidebar, shell, logo, metadata, and UI sound files.

**Interfaces:**
- Consumes Tasks 4–5 module descriptors.
- Produces Quality of Life → Modules and Miscellaneous → Settings, animated cards, per-module editor pages, keybind editing, conflict display, live crosshair editor, and reset confirmation.

- [ ] Write navigation/descriptor tests asserting every requested module has a settings page.
- [ ] Implement categories, grids, editors, animations, enlarged logo, full-screen modal host, and visible branding cleanup.
- [ ] Remove ambience startup/refresh paths while retaining event sounds.
- [ ] Verify navigation tests and Compose compilation.

### Task 7: Integration, Deployment Notes, and Artifact

**Files:**
- Modify: `backend/README.md`
- Modify: `README.md`
- Create: `BUILDING.md`

**Interfaces:**
- Produces tested source ZIP and exact D1 migration/deployment/build instructions.

- [ ] Run backend tests.
- [ ] Run Gradle formatting/static checks and `buildAndCollect` where network dependencies are available.
- [ ] Scan visible resources/metadata for stale OneConfig branding while preserving legal notices and namespaces required by the upstream architecture.
- [ ] Extract the final ZIP into a clean short path and validate every Gradle project directory and archive entry.
- [ ] Document `npx wrangler d1 migrations apply reconfig-chat --remote`, `npx wrangler deploy`, and `gradlew.bat buildAndCollect`.

