# Runtime repair implementation plan

**Goal:** Connect the existing ReConfig module settings to Minecraft 1.21.11 runtime behavior.

**Architecture:** Keep the module catalog as the settings source. Use narrowly targeted
client mixins and controllers; register real HUD providers with the existing editor.
Client projectile prediction must not delay, forge, or suppress normal use packets;
authoritative spawns replace predictions and rejected predictions expire.

**Constraints:** Standalone Fabric 1.21.11, preserve existing UI/assets, Java 21 build.
No claim of compile/runtime verification without a successful build and client run.

## Tasks

- [x] Test and implement reusable effect math and freelook state without Minecraft dependencies.
- [x] Connect camera rotation and hold/toggle lifecycle; restore perspective on exit.
- [x] Register Item Counter and WAILA TextHud providers and module visibility gating.
- [x] Connect fog buffer ranges, particle draw data, hit tint and nameplate extraction.
- [x] Connect precipitation selection and render a configurable crosshair.
- [x] Replace selected sound events, preserving location, category and attenuation.
- [x] Implement visual projectile prediction/reconciliation and world-only frame accumulation.
- [x] Restrict buildAndCollect to 1.21.11, run available tests and attempt Gradle.
- [x] Audit registrations and document limits accurately in STATUS-v26.md.
- [ ] Full Java 21 Gradle compilation, mixin audit and Minecraft run (blocked by network).

## Verification

Run dependency-free Java assertions against production effect math before and after
implementation; run existing Python regressions and backend tests. Validate mixin
JSON and resource paths. The required release gate remains Java 21 Gradle compilation,
mixin audit and in-game testing on a remote server with latency/rejection cases.
