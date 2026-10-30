# ReConfig changelog

## 3.3.1 — UI compatibility and HUD controls

### 30 October 2026 hotfix — same version

- Particle Changer now caches settings outside the per-particle render path, reuses unchanged snapshots and skips drawing particles at zero opacity. Size and opacity still respect the selected settings.
- Added mouse-button bindings for gameplay modules, including Zoom on Mouse 4/5. Mouse bindings persist and work with hold/toggle activation.
- Recording captures keyboard and mouse input before page navigation, gameplay actions and vanilla fullscreen/debug/screenshot shortcuts. Captured repeats/releases remain consumed after the binding is saved; Escape cancels module capture and Backspace/Delete clears it.
- Added mouse recording to regular config keybind controls and prevented scroll actions while recording.
- Rejected non-finite numeric settings before they reach rendering and blocked unsafe OpenGL fallback/profiling calls when VulkanMod cannot provide a compatible UI renderer.
- Cleared pending Compose work before offscreen-target cleanup and corrected startup version lookup to identify ReConfig correctly.
- The supplied fatal log ends in NVIDIA's native OpenGL driver. Its cause cannot be established from that log alone; this hotfix does not claim to repair all GPU-driver or third-party-mod crashes.

### Original 3.3.1 update

- Isolated ReConfig's Compose/AndroidX runtime and its nested library IDs to fix UI startup when a different Compose Multiplatform version is installed. Kept JNI packages intact and required the matching native renderer ABI.
- Stopped bundling stale dependency JARs from previous builds.
- Added individual projectile-counter switches for arrows, ender pearls, wind charges, snowballs and eggs, with optional zero-count hiding.
- Improved the movable Potion Effects HUD with native icons, translated names, levels, remaining time, infinite-duration support and expiry highlighting. Added display and empty-state controls; retained the existing HUD position ID.
- Added Hide Scoreboard with an optional keybind. It affects the local vanilla sidebar only.
- Corrected custom HUD refresh intervals, avoided unnecessary disabled-HUD sampling and repeated linear module/setting searches, and removed unused effect-text generation.
- FPS Boost no longer raises an already-lower render or entity distance. No measured FPS increase is claimed for this release.
- Released the active profile's directory watcher before renaming it to avoid Windows access-denied failures.
- Ignored duplicate notifications of ReConfig's own config writes so they cannot reload old saved values over newer edits during profile changes. External file edits still reload.
- No backend deployment or migration is required for these changes.

## 3.3.0 — ReConfig feature and polish update

### Profiles and HUD polish

- Renamed Your Profile to Profiles; your own profile remains the default view.
- Added an adaptive two-column profile editor, locked account identity, five-character tag field, editable bio, emoji selection, publication consent, unsaved-change/reset controls and persistent save feedback.
- Added an expanding search control and a separate public-profile results view.
- Enlarged the official-skin preview, added idle/walk/wave animations, pause and rotation controls, and a live Minecraft-font nametag/tag preview.
- Added saved live nametag tags and bundled emoji glyphs. Your private tag stays local; other ReConfig clients see it only when you publish your profile. Public lookups are cached and bounded.
- Removed forced shadows from ReConfig HUD labels and prevented custom text cards from redrawing merged backgrounds.
- Fixed Kotlin config delegates saving to a consumed tree after an existing registration; preserved their schema across profile changes.
- Corrected the Minecraft mixin audit to select the `reconfig_platform` mod metadata.

### Existing update highlights

- Private-by-default profiles and a curated public server directory.
- Startup notification cursors, invitation acknowledgments and older-message paging.
- Windows Spotify session artwork/state, inventory layout controls, crosshair width/height, BetterSounds category volumes and duplicate spear-sound cooldown.
- Privacy policy versioning, offline summary and global HUD background controls.

### Verification

The Gradle build and checks, 86 source tests plus 51 subtests, and all 32 backend tests pass. Offscreen profile layout and skin poses were inspected. Minecraft runtime behavior remains unverified; see `VERIFICATION.md`.

### Runtime dependencies

Compose/Skiko native libraries are retained because the current renderer requires them.

## 3.2.0 — Stability and publishing release

### What’s new

- Added Target HUD, Speedometer, Light Level, Clock HUD, Durability Alerts, Low Health Warning, Projectile Counter, Session Statistics, Auto GG, and Chat Timestamps.
- Added Shulker Box Tooltip, Totem Pop HUD/customizer controls, Browser Music HUD, and a ReConfig entry in the Escape menu.
- Added cursor-based conversation loading so an open chat requests only messages newer than its last received message.
- Added another Minecraft-profile lookup fallback for friend requests.
- Added a configurable Reach Indicator HUD with editable background and decimal precision.
- Changed the default ReConfig accent color to `#00A6FF`.

### Bug fixes

- Gameplay HUDs no longer appear over the main ReConfig interface. Enabled HUDs remain visible inside Edit HUD.
- Fixed visibility for Combo Counter, Item Counter, WAILA, CPS, FPS, Coordinates, Memory, Server Status, Keystrokes, Armor Status, Effects, and Inventory overlays.
- Removed UI Ambience and its volume and music-ducking controls.
- Reworked Fullbright so it changes the lightmap calculation without saving an invalid gamma value.
- Reworked Custom Crosshair so it replaces the vanilla crosshair only while enabled.
- Reworked Freelook camera alignment and input ownership so mouse movement rotates the detached camera without rotating the player, and removed the conflicting legacy mouse-camera mixin.
- Updated the HUD API compatibility snapshot for the new `isAvailable()` override so `apiCheck` no longer fails on that declaration.
- Removed the unsafe off-screen Compose GPU warm-up responsible for native Skia crashes.
- Reduced friends/messages database use by removing whole-history polling for every friend.
- Added 429 backoff and friendly capacity errors instead of displaying raw D1 database errors.
- Message sending now refreshes only the active conversation, and deletion no longer reloads every friend.

### Backend deployment

Deploy the included Worker update to enable sanitized D1-limit responses and the extra Minecraft-profile lookup fallback.

## 3.1.4

- Added privacy consent, message replies, friend removal, request denial, invite cooldowns, HUD visibility controls, and UI state persistence.

### Account verification repair (23 October 2026)

- Stop repeated ownership proofs from hammering Mojang after HTTP 429. Friends, messaging, profiles, and the server directory share the verification cooldown and recover automatically.
- Show an actionable verification cooldown instead of a generic request failure. Signing out is not required by this repair.

### Public server discovery and HUD polish

- Added five curated Java server recommendations; search now matches names, addresses and game modes, including spaced server names.
- Added Recommended ordering and server addresses, live player sorting and forced status refresh.
- Removed the cylindrical rounded top-band decoration and top glints from HUD cards.

### Nametag emoji and HUD visibility repair

- Render saved nametag emoji through native GUI sprite text, avoiding oversized bitmap-font glyphs.
- Hide all ReConfig HUD content, merged backgrounds and cached layers throughout the normal menu; retain HUDs in the dedicated HUD editor.
