# ReConfig Client Expansion Design

## Objective

Turn ReConfig into a standalone Fabric 1.21.11 client with a ReConfig-branded OneConfig-style interface, reliable friends and same-server messaging, a functional Quality of Life module suite, and a complete settings area. No module card may be a placeholder.

## Product Boundaries

- Target Minecraft Java Edition 1.21.11 on Fabric.
- ReConfig remains standalone at runtime; users do not install OneConfig separately.
- The existing OneConfig-derived rendering and configuration code may remain internally for compatibility and licensing, but visible UI, metadata, logs, configuration names, and assets must use ReConfig naming wherever safely possible.
- Upstream copyright and license notices remain intact.
- Cloudflare Worker and D1 remain the free hosted social backend at `https://reconfig-chat.duv14-reconfig-api.workers.dev`.
- Chat messages are accepted only while both users are online on the same normalized Minecraft server. Friend requests, presence, and server invitations may cross servers.

## Navigation and Visual Structure

The sidebar contains three animated categories:

1. **Social**
   - Friends
   - Messaging
2. **Quality of Life**
   - Modules
3. **Miscellaneous**
   - Settings

Navigation entries retain the OneConfig interaction language: animated accent selection, hover interpolation, press feedback, consistent spacing, beveled/smooth shapes, blurred game backdrop, and translucent panel treatment. The ReConfig logo is enlarged without changing the sidebar's alignment. Modal scrims are attached at the shell root so they cover and blur the entire UI rather than only the content pane.

Continuous menu ambience is removed. Open, close, click, and notification events use the supplied ReConfig sounds and respect per-event volume settings.

## Social Identity and Authentication

The client derives the active account identity from the authenticated Minecraft session UUID and username. A random credential is stored per UUID, not once per operating-system profile. The client enrolls whenever the active account changes and never marks itself enrolled until the Worker returns a successful response.

Every HTTP response is checked for a 2xx status and parsed into a typed result. Worker errors appear as ReConfig notifications and relevant inline form errors. Network failures use bounded retry/backoff and never create fake local success state.

## Friends

Friendship uses explicit states and actions:

- none
- outgoing request
- incoming request
- friends
- blocked/removed only as an internal terminal state for cleanup

Sending a request creates an outgoing record. The recipient must explicitly accept or decline it. Reciprocal legacy requests are migrated to an accepted friendship. Friends can be removed. Duplicate operations are idempotent.

Each row displays:

- the player's real Minecraft head, resolved from their UUID and cached locally;
- username;
- green online or grey offline status dot;
- contextual Accept/Decline controls for incoming requests;
- Message and Invite to Server buttons for accepted friends.

Invite to Server sends a backend event containing the sender's current server address only when the sender is connected to a multiplayer server. The recipient receives a notification with Join and Dismiss actions. The client must not send or display credentials or single-player/LAN internals.

## Messaging

Messaging uses a WhatsApp-style two-pane interface:

- left conversation list with head, name, latest message, timestamp, presence, and unread badge;
- right header with head, name, status, and server eligibility;
- scrolling bubbles with sender alignment, timestamps, delivery state, and date grouping;
- composer with input, send button, character limit, disabled-state explanation, and smooth focus/press animations.

The Worker records a presence heartbeat containing a one-way hash of the normalized server address plus an expiry time. It permits message creation only when sender and recipient are accepted friends, both presence records are fresh, and their server hashes match. The backend retains conversation history, but clients cannot send while on different servers or while either player is offline. Polling retrieves messages and receipts incrementally without marking messages lost before the client acknowledges them.

## Module Framework

Each module implements a shared contract containing stable id, display name, description, category, enabled state, keybind, configuration schema, and lifecycle hooks. Settings persist to a ReConfig-owned JSON file. Keybinds support unbound, hold, toggle, and conflict reporting as appropriate. The Modules screen displays animated cards; selecting a card opens its real configuration page.

All requested modules are functional:

1. **Auto Text Hot Key** — configurable key-to-chat-text actions with optional command mode and send/open-chat behavior.
2. **Better Sounds** — independent UI/world/player sound category multipliers and optional replacement toggles supported by client sound events.
3. **Fog Customizer** — fog start/end distance, density-style distance control, color override, dimension scope, and disable option.
4. **FOV Changer** — base FOV plus sprint, speed, bow-use, and flying modifier controls with smooth interpolation.
5. **Hitbox** — entity hitbox rendering with type/color/line-width and through-walls controls.
6. **Hit Color** — configurable hurt flash overlay color and opacity.
7. **Hurt Cam** — intensity multiplier from zero to vanilla strength.
8. **Item Counter** — selected item/ammunition counts rendered as a movable HUD element.
9. **Motion Blur** — configurable post-process accumulation strength, disabled in menus and guarded for incompatible renderer state.
10. **Nick Hider** — local replacement of the player's name in HUD, chat display components, nametags, and tab list without modifying sent chat.
11. **Particle Changer** — per-particle visibility and multiplier controls with global limit.
12. **PvP Info** — configurable HUD for FPS, ping, CPS, combo, reach estimate, potion effects, armor durability, and coordinates.
13. **WAILA** — targeted block/entity name and core state information near the crosshair.
14. **Weather Changer** — client-side clear/rain/thunder visual override and intensity without changing server weather.
15. **Waypoints** — add/edit/delete dimension-aware waypoints with world beacon, distance, color, visibility, and configurable keybind.
16. **Freelook** — independent camera yaw/pitch while preserving player direction, first/third-person support, hold/toggle modes, sensitivity, camera collision option, perspective restoration, and a configurable keybind.
17. **Zoom** — smooth zoom, hold/toggle, scroll-adjustable magnification, cinematic camera option, and configurable keybind.
18. **Custom Crosshair** — live editor and in-game renderer for shape, gap, size, thickness, outline, opacity, static/dynamic behavior, and normal/target/hit colors.

Module implementations use narrowly scoped Fabric callbacks or mixins and do not alter server-authoritative state. Render-state changes are restored after each hook to prevent leaks into vanilla or other mods.

## Settings

Miscellaneous → Settings provides:

- UI scale and panel scale;
- animation enable/speed;
- game blur and panel blur strength;
- theme/accent controls;
- open/close/click/notification sound toggles and volume;
- notification duration and placement;
- social presence visibility, invitations, and notification privacy;
- global keybind management and conflict display;
- reset individual section and reset all, each with confirmation.

There is no ambience/looping-menu-sound setting because the loop is removed entirely.

## Backend and Migration

A new D1 migration introduces normalized friend requests/friendships, per-user sessions, presence, message conversations, delivery acknowledgements, and server invitations. It preserves users and safely converts mutual rows in the legacy `requests` table into accepted friendships. Existing legacy messages may remain archived; new message history uses the new schema.

Worker endpoints are versioned and tested for:

- account enrollment and token/account separation;
- request, accept, decline, duplicate, reciprocal migration, and remove flows;
- presence expiry and online status;
- same-server message acceptance;
- different-server/offline message rejection;
- message pagination and acknowledgement;
- invitation creation and retrieval;
- authorization and malformed input.

## Failure Handling

- Social actions show progress and remain actionable after failure.
- A failed request never appears as pending unless the server persisted it.
- Cached heads fall back to a neutral Steve/Alex head, never a letter avatar.
- If the Worker is unavailable, modules and settings continue working; Social shows an offline banner.
- Renderer hooks fail closed and restore vanilla behavior when prerequisites are unavailable.
- Invalid or conflicting module configuration is clamped during load and surfaced in logs without crashing Minecraft.

## Verification

Completion requires:

- backend unit tests and migration tests passing;
- module configuration serialization tests passing;
- module lifecycle and keybind tests passing where logic can be isolated;
- Stonecutter preprocessing for the inactive 1.21.11 target verified;
- `gradlew.bat buildAndCollect` succeeding on Windows with Java 21;
- two-account manual social test on separate computers;
- same-server message success and different-server rejection verified;
- every module enabled and exercised in a Fabric 1.21.11 client;
- extracted ZIP directory validation before delivery.

