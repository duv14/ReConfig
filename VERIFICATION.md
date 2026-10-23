# Verification — 22 October 2026

- `gradlew.bat buildAndCollect check`: passed, including `MixinAuditTest` and the Minecraft keybind transition tests. A clean rebuild is performed before packaging.
- Python/Java-source regression suite: 87 tests and 51 subtests passed.
- Minecraft nametag tests: native name preservation, five-character suffix text, selected emoji font, and all seven bundled glyph textures passed.
- Public tag cache: nonblocking lookups, per-frame request de-duplication, positive/negative expiry, bounded queues and identity isolation passed.
- Kotlin settings persistence: an already registered canonical tree now receives delegate writes and preserves its property schema through profile changes. The regression reproduces the old failure and passes with the fix. JVM test state is isolated under each module build directory.
- Backend Node suite: 38 tests passed. The profile fixture accepts a five-character tag and rejects a six-character tag while retaining publication-consent and privacy checks.
- Actual Compose profile editor rendered offscreen at 1040×840 and inspected for layout, spacing, readability and preview placement.
- Official `12duv101` slim skin rendered in idle, walking and waving poses; frame content and articulation were inspected. Skin raster work is capped at 20fps and pauses while a static preview is paused or leaves composition.

Earlier visual checks were offscreen. On 23 October the running Minecraft session was used to reproduce and repair enrollment, and the Worker was redeployed. Real account-to-account friend acceptance/message delivery was not exercised; those flows pass the backend SQL integration tests. No additional database migration is required.

## Account verification repair

A trace inside the running Minecraft client reproduced `AuthenticationException: RateLimiter disallowed request` with HTTP 429 from Mojang's `/session/minecraft/join`. The old enrollment path retried on background polls without a verification cooldown and hid the exception behind a generic notification.

All enrollment consumers now share a synchronized retry gate: rate-limit failures wait five minutes, increasing to fifteen minutes after repeated failures; other verification failures back off from one minute. Successful enrollment and account changes reset the gate. No ownership checks were removed. The UI reports automatic recovery and does not ask the user to sign out. Deterministic tests exercise repeated poll/click suppression, increasing backoff, success/reset, wrapped rate-limit exceptions, and safe error messages.

The Worker now distinguishes upstream verification errors from rejected ownership and retries blocked HTTP transport through a TLS socket to the same official Mojang host. TLS and UUID/name checks remain mandatory; client assertions and third-party proxies are not trusted. The socket is bounded by a six-second timeout and 128 KiB response limit. Transport tests cover host restrictions, fragmented and chunked replies, truncation, success and failure preservation.

Live evidence: Mojang join succeeded, Cloudflare normal fetch received upstream HTTP 403, the TLS fallback completed enrollment successfully. Authenticated live reads returned HTTP 200 for own profile, friends, public profile search, public servers, sync and message history. Saving the existing profile unchanged returned HTTP 200. The directory now contains five explicitly curated public Java server recommendations (migration 0008). The running client retry pause was removed after successful enrollment. No sign-out was needed.

Worker deployment: 8c43186c-2c6b-496a-8365-dd9384883002. The repair is already deployed; no user deployment command is needed. Replace the client JAR and restart Minecraft to retain the client cooldown fix.

## Public server discovery and flat HUD cards (23 October 2026)

- `gradlew.bat clean buildAndCollect check`: passed after the directory UI and HUD changes.
- 87 source tests / 51 subtests and 38 backend tests pass. Directory coverage loads the actual recommendation migration, checks default order, spaced names, addresses and game-mode searches, absent results and hidden listings. Migration reapplication preserves existing accounts/messages.
- Migration 0008 and Worker version `5f096f65-df58-4ece-9aa2-2ff74d0a3edf` are deployed. Live authenticated queries returned five recommendations; Donut SMP, CubeCraft's address, Bed Wars and RPG each matched the expected entry. A nonexistent query returned no records.
- The running game's actual native status probe returned positive ping, live online counts and icons for all five recommendations. No player counts were seeded. Counts and availability change over time.
- The client opens with Recommended ordering, searches names/addresses/descriptions, displays addresses, probes the full result list with at most four concurrent connections, sorts player counts from those shared live status responses, and Refresh bypasses the status cache.
- Removed the rounded 13-pixel top highlight band and extra top-edge highlight from text HUDs, and the music HUD's top-edge highlight. Flat card fill, uniform outline, content and positions remain. The updated HUD appearance has not yet been inspected in a restarted game; the cylinder diagnosis is based on the user's description and the matching top-band drawing code.

These backend changes are already live for this installation. Replace the JAR and restart Minecraft for the directory UI and HUD rendering changes. No user redeployment is required. A separate installation must apply migration 0008 before deploying this Worker.

## Nametag sprites and menu HUD visibility (23 October 2026)

- Reproduced both regressions with failing native nametag/component tests and a HUD visibility test before applying the repair.
- Bitmap emoji textures are 500 pixels wide while Minecraft's font texture atlas is 256 pixels. Nametag emoji components now use Minecraft's `FontDescription.AtlasSprite` and seven explicit sources in the GUI atlas, preserving the original artwork. Native bytecode confirms sprite glyphs bypass bitmap glyph baking and use the selected sprite independently of the placeholder character. The installed ModernUI version includes AtlasSpriteFont/TextLayoutProcessor support for this path.
- The live saved profile includes an emoji and a nonempty tag; this was not a missing save or backend field.
- The normal ReConfig menu suppresses every HUD handled by the shared HUD renderer, including merged backgrounds, cached HUD texture blits, legacy HUDs, compatibility hooks and embedded HUD content. The dedicated HUD editor is the explicit exception. Menu suppression overrides individual Show in Screens settings and invalidates the cached frame on transitions.
- Regression coverage checks gameplay -> menu -> editor -> menu -> gameplay, plus native sprite font selection and all seven atlas registrations/resources. Code review found no remaining material issues. Updated rendering must still be visually checked after installing the new JAR and restarting Minecraft.
- No backend change, redeploy or migration is needed for these two client fixes.

Final verification for the nametag/menu repair: `gradlew.bat clean buildAndCollect check` passed in 1m 46s (135 tasks). All four native nametag tests, the new HUD menu visibility regression, the mixin audit and remaining checks pass. The source regression suite also passes (87 tests / 51 subtests). No runtime screenshot of the updated nametag or menu was captured.
