# ReConfig Functional Modules Rebuild Design

## Goal

Replace the placeholder ReConfig module layer with a standalone, persistent Fabric 1.21.11 module engine whose visible UI preserves the OneConfig/iOS 26 visual language and whose requested modules have real runtime behavior.

## Branding

- The in-game sidebar uses the full supplied ReConfig text wordmark.
- The supplied square blue `R` is used for icon-sized locations that previously displayed a OneConfig logo graphic, including mod metadata, notifications, and internal config icons.
- OneConfig component assets, shapes, shadows, blur, icons, typography infrastructure, and animations remain available because they form the requested iOS 26 visual foundation.
- Legal attribution and internal namespaces remain unchanged where required by the upstream license and architecture.

## Module UI

The Quality of Life → Modules page uses the existing OneConfig Mods-grid proportions: four columns, large dark image area, blue title footer, hover animation, enabled indication, and category filter chips. Each module has a dedicated icon and opens a settings page.

Settings pages use typed native-looking controls rather than generic text fields:

- Boolean values use animated switches.
- Bounded numeric values use sliders plus a visible numeric value.
- Enumerations use dropdown controls.
- Colors use a swatch and color picker.
- Text uses a styled input only where text is the correct data type.
- Keybinds use a capture button. Clicking begins listening; the next keyboard key is stored, Escape cancels, and Backspace/Delete unbinds.
- Every page includes enable, reset, and back controls.

## Persistent Module Engine

A typed module registry is shared by UI and Minecraft runtime. It owns stable IDs, enabled state, keybind, typed setting descriptors, validation, and immutable snapshots. Configuration is stored under the Minecraft config directory as ReConfig JSON and is written atomically after every mutation. The registry loads before module hooks begin, so enabled states, settings, colors, keybinds, and UI preferences survive a full game restart.

Key dispatch distinguishes press, repeat, release, hold, and toggle modes. UI key capture consumes the next event before module activation to prevent binding a key from also triggering a module.

## Runtime Hooks

Each requested module receives a real scoped hook:

- Auto Text: sends configured chat text/commands on activation.
- Better Sounds: adjusts or cancels matching sound instances by category.
- Fog Customizer: changes fog start/end and optional color.
- FOV Changer: supplies configured base/sprint FOV with interpolation.
- Hitbox: toggles vanilla hitbox rendering and applies supported visual options.
- Hit Color: changes hurt overlay tint/opacity.
- Hurt Cam: scales or disables damage camera tilt.
- Item Counter: renders count/durability HUD.
- Motion Blur: applies configurable frame/post-process accumulation while allowed.
- Nick Hider: replaces the local name in chat, tab, and nametag scopes.
- Particle Changer: filters particle families and enforces multiplier/limit.
- PvP Info: renders enabled combat/performance metrics.
- WAILA: renders targeted block/entity details within configured range.
- Weather Changer: overrides client rain/thunder visuals without changing the server.
- Waypoints: persists dimension/server-scoped points and renders labels/distance.
- Freelook: rotates only the camera, restores perspective, and obeys hold/toggle mode.
- Zoom: interpolates magnification and supports hold/toggle activation.
- Custom Crosshair: suppresses vanilla crosshair and renders configured shape, size, gap, thickness, outline, opacity, and contextual colors.

Unsupported runtime conditions fail closed: the module disables only its affected rendering/action for that frame and records a diagnostic rather than crashing Minecraft.

## General Settings

Miscellaneous → Settings uses the same styled controls for UI scale, animation speed, game blur, panel blur, accent, event sound volumes, notification duration, social presence, and reset. Changes are saved immediately and applied live where supported.

## Testing and Verification

- Pure tests cover setting validation, serialization, atomic reload, key capture, activation modes, and module math/filter helpers.
- Descriptor tests prove all 18 requested modules have icons, keybinds, settings pages, and runtime implementations.
- UI source tests prevent invisible focusable overlays and generic numeric/key textboxes.
- Mixin JSON and metadata are parsed and scanned.
- Backend tests remain green.
- The Windows delivery build command remains `gradlew.bat clean buildAndCollect`; its 1.21.11 JAR is collected in `build\\libs`.

