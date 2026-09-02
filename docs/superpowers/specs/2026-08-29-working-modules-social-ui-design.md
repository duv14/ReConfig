# ReConfig Working Modules, Social, and UI Design

## Scope

This release replaces placeholder module settings with working client-side features, repairs the changelog and social screens, and extends the existing ReConfig visual system consistently across modules, friends, and messages. The release remains a standalone Fabric 1.21.11 mod and uses the existing Cloudflare Worker/D1 backend for social data.

All module state and settings persist across restarts. Every module has a controls-style enable/disable keybind. A keybind fires once per physical press, does not fire while ReConfig is recording another key, and does not fire while a text field, chat screen, or other menu owns keyboard focus.

## Runtime Architecture

`ModuleCatalog` remains the UI-facing catalog. It gains stable setting IDs and typed helpers for multiple keybinds, booleans, numbers, text, choices, and colors. A Minecraft-side `ReConfigModuleRuntime` owns tick processing and delegates to small feature controllers. Render-only features use narrowly scoped mixins or Fabric render callbacks. HUD features register with a ReConfig HUD registry that stores position and scale separately from module settings.

Key handling is edge-triggered. The runtime tracks the previous state of registered GLFW keys, toggles a module on the rising edge of its toggle key, and invokes feature actions only on their own rising edges. Conflicting keys are allowed but surfaced in the UI.

Features that visually replace server state—weather, time, fog, FOV, nickname, particles—are client-only. They never send commands or claim to modify the server. State is restored cleanly when a module is disabled or the player leaves a world.

## Changelog Reliability

The changelog cache is read before the network request. If cached entries exist they render immediately while a refresh runs. The remote request has bounded connect and response timeouts and always transitions out of loading in `finally`. Invalid HTML, an empty parsed feed, DNS failures, TLS failures, and timeouts show a compact offline panel with Retry. A retry resets the one-shot guard and starts a new request. A failed refresh never replaces valid cached data.

## Module Definitions

### Auto Text Hot Key

- Enable/disable keybind.
- Send-message keybind.
- Message text, maximum 256 characters.

When enabled and connected to a world, the send key transmits the configured nonblank text through the normal signed Minecraft chat path. It does nothing in single-player without an active player connection, while chat/UI text input is active, or when the message is blank. It does not bypass server chat restrictions or cooldowns.

### Better Sounds

- Enable/disable keybind.
- Eating sounds toggle.
- Hit sounds toggle.
- Wind-charge sounds toggle.
- Mace-hit sounds toggle.

The project bundles new, redistribution-safe sound assets sourced from libraries that explicitly permit redistribution. Asset source, author, URL, and license are recorded in `ATTRIBUTIONS.md`. Each enabled event layers or replaces only the corresponding local feedback sound; disabling the module restores vanilla audio. Volume is normalized so the effects do not clip.

### Fog Customizer

- Enable/disable keybind.
- Fog distance slider.
- Fog opacity slider.

The distance control adjusts client render fog start/end within safe render-distance bounds. Opacity blends the fog contribution rather than changing world geometry. Disabling restores vanilla fog parameters immediately.

### FOV Changer

- Enable/disable keybind.
- FOV slider with a range beyond vanilla's 120-degree cap, clamped to a safe 30–180 range.

The effective gameplay FOV is overridden while enabled without permanently rewriting Minecraft's own FOV option. Disabling restores the user's vanilla value.

### Hitbox

- Enable/disable keybind.

The module mirrors Minecraft's entity hitbox renderer—the same feature normally toggled by `F3+B`. If hitboxes are toggled externally, ReConfig observes that state and updates the module UI instead of fighting it.

### Hit Color

- Enable/disable keybind.
- Flash duration slider.
- Color setting using a responsive HSV color wheel with alpha.

When an entity is confirmed as hit by the local player, its rendered model receives a full-body color overlay for the configured duration. The overlay is render-only and does not change entity data.

### Hurt Cam

- Enable/disable keybind.

While enabled, the local player's damage camera tilt is suppressed. Damage, knockback, sounds, and all other game behavior remain vanilla.

### Item Counter

- Enable/disable keybind.
- Read-only notice: "You can move the HUD through the Edit HUD button on the sidebar."

The HUD displays the total count of the currently held item across the player's inventory above the hotbar. Edit HUD supports drag positioning and a scale slider. Position and scale persist.

### Motion Blur

- Enable/disable keybind.
- Strength slider.
- Responsiveness slider.

A post-processing accumulation pass blends recent rendered frames. Strength controls blend contribution and responsiveness controls decay speed. The framebuffer is reset on resize, world change, GUI transitions, and module disable. Conservative defaults avoid ghosting and the pass is skipped when Minecraft's rendering state is incompatible.

### Nick Hider

- Enable/disable keybind.
- Replacement nickname text.
- Hide nickname checkbox.

The local player's third-person nameplate is either hidden or replaced with the configured text. This is client-side privacy only and does not spoof the authenticated username to servers or other players.

### Particle Changer

- Enable/disable keybind.
- Particle opacity slider.
- Particle size slider.

Particle rendering applies the configured alpha and scale client-side. Disabling restores vanilla rendering.

### WAILA

- Enable/disable keybind.
- Read-only Edit HUD notice.

The HUD shows the localized name and type of the block, entity, or player under the crosshair. It updates from the normal client hit result, supports drag/scale in Edit HUD, and stores its layout.

### Weather Changer

- Enable/disable keybind.
- Time-of-day choice/slider.
- Weather choice: Vanilla, Clear, Rain, or Snow.

The selected time and weather are visual client overrides. Snow uses biome-aware precipitation rendering where feasible; it does not alter server weather or block state.

### Waypoints

- Enable/disable keybind.
- Waypoint manager opened from module settings.

The manager creates, edits, searches, enables, and deletes waypoints with name, coordinates, dimension, color, icon, and optional distance visibility. World rendering shows a clean beacon/label with distance, and an optional HUD edge indicator appears when off-screen. Waypoints are grouped by server/world identity and dimension, persist locally, and never teleport the player or send server commands. Death-waypoint creation is optional and disabled by default.

### Freelook

- Enable/disable keybind.
- Mode: Hold or Toggle.

Freelook switches to third person and detaches camera yaw/pitch from player rotation while active. Releasing the key in Hold mode or pressing again in Toggle mode restores the previous camera perspective and orientation safely.

### Wind Charge Optimizer

- Enable/disable keybind.

Provides a client-only trajectory preview and timing/cooldown feedback for the held wind charge. It does not automate aiming, clicking, movement, or server packets.

### Pearl Optimizer

- Enable/disable keybind.

Provides a client-only ender-pearl trajectory preview and cooldown feedback. It does not automate throws or modify server physics.

### Custom Crosshair

- Enable/disable keybind.
- Crosshair editor.

The editor controls shape, length, gap, thickness, outline, color, opacity, center dot, and dynamic spread. A live preview uses the exact renderer used in game. Enabling suppresses the vanilla crosshair and renders the configured one.

### Removed Modules

PvP Info and Zoom are removed from the catalog, routes, search results, stored placeholder UI, and icon set. Existing preference nodes may remain harmlessly on disk so the update does not perform destructive filesystem operations.

## Social Behavior

Messages are laid out by authenticated sender UUID, not by list position:

- Local messages: blue gradient bubble on the right, local username aligned above the bubble, and a small gray bin icon.
- Remote messages: gray gradient bubble on the left with sender name aligned above.

Sending or receiving preserves scroll position correctly. Sending scrolls to the newest message at the bottom, never the top. Opening a conversation starts at the latest message. New remote messages auto-scroll only when the user is already near the bottom, so reading history is not interrupted.

Deleting is allowed only for messages authored by the authenticated user. Deletion removes the message from the shared conversation for both users and requires a new authenticated Worker endpoint plus D1 migration support. The UI removes it after server confirmation and restores/shows an error if deletion fails.

Outgoing friend requests have a red Cancel Request button. Cancellation deletes only the pending outgoing request. Accepted friendships retain the normal remove-friend action.

Invite to Server reads the active multiplayer connection address directly. Integrated single-player and disconnected states disable the button with an accurate explanation. The recipient receives the exact address through the existing invitation queue and can connect from the notification/action.

## Visual System

Module cards, setting rows, friends, dialogs, and message bubbles share a reusable iOS-inspired glass border modifier. It uses a subtle lighter top/side highlight, darker lower edge, low-opacity inner stroke, and restrained shadow instead of a flat bright outline. Enabled module cards retain an outline and receive only a low-opacity blue surface tint. Message borders follow their bubble color: blue for local and neutral gray for remote. Friend rows use the same neutral glass treatment. Destructive actions are red.

Animations use one shared motion scale and eased transitions. Popup/dialog content animates opacity, scale, and vertical offset. Toggles, hover states, settings expansion, friend state changes, message insertion/deletion, and bubble placement animate without changing list identity or causing scroll jumps.

## Persistence and Compatibility

Settings retain stable IDs and use the existing preferences node. New HUD positions and waypoint collections use versioned JSON under `config/reconfig`. Writes use temporary-file replacement so interruption cannot leave truncated data. Invalid values fall back to defaults without deleting the source file.

The social backend remains compatible with existing users, friendships, and messages. New migrations are additive. The client continues using the hardcoded Worker URL. Cross-server and offline messaging remain supported.

## Error Handling

No asynchronous screen can remain in a permanent loading state. Network operations expose loading, success, empty, and error states and have Retry actions. Runtime feature controllers tolerate missing world/player/renderer state and restore vanilla state on disable, disconnect, or exception. Unsupported shader/render combinations disable only the affected visual effect and show a concise notification rather than crashing Minecraft.

## Verification

Implementation follows test-first development. Source regression tests cover catalog membership, exact setting IDs/types, persistence calls, runtime registration, mixin declarations, HUD registration, chat sender alignment, deletion authorization, scroll direction, invite address resolution, changelog timeout/finally/retry behavior, and visual modifier reuse. Backend tests cover delete authorization, cancel-request behavior, invitation delivery, external-player requests, and cross-server/offline messages.

The final verification gate is:

- All Python source regressions pass.
- All Node Worker behavior tests pass.
- `git diff --check` is clean.
- `gradlew.bat clean buildAndCollect` succeeds under Java 21 on Windows and produces `ReConfig-fabric12111-1.0.0.jar`.
- Manual in-game smoke checks cover every module's toggle, setting persistence, disconnect restoration, HUD drag/scale, message layout/deletion, invitations, and changelog offline behavior.

