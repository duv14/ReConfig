# ReConfig changelog

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
