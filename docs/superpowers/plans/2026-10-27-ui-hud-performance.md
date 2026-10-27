# ReConfig 3.3.1 implementation plan

User requests a finished update and has opted out of routine approval gates.
Implement inline in the supplied source archive; it has no Git checkout.
Baseline edited files are backed up outside the source tree.

## Design

UI: prevent cross-mod Compose ABI collisions by privately relocating AndroidX
and org.jetbrains.compose classes and every ReConfig reference in the final
nested distribution. Preserve Kotlin metadata, service resources and Fabric
dependency metadata. Leave Skia/Skiko namespaces and native DLLs intact because
their native entry points depend on those names. Exclude stale nested artifacts.
Keep compilation/dev namespaces unchanged; buildAndCollect must output the
isolated release. Validate with a conflicting public Compose fixture and a real
private Compose scene creation/close smoke test.

Projectile HUD: independent arrows, pearls, wind charges, snowballs and eggs
toggles, optional hiding zero counts, no displayed rows when all are disabled.
Potion HUD: reuse effect_status to preserve saved positions; rename the module
Potion Effects, native effect icons, remaining timers/infinity, optional labels
and levels, hide-empty setting, editor previews and preserved HUD transforms.
Scoreboard: explicit Hide Scoreboard module, off by default; toggling it hides
only the client sidebar and never changes the server scoreboard.
Performance: nanosecond HUD update intervals, avoid disabled-module sampling,
constant-time module/settings lookup, reversible FPS render caps that never
increase existing lower limits. Do not promise universal FPS improvement.

## Work

- [x] Add behavioral regressions for projectile filtering/timers/render caps and
      package isolation; verify failures against the old code/artifact.
- [x] Implement private release packaging and stale nested-JAR prevention.
- [x] Add projectile settings, improve the existing potion HUD, add sidebar toggle.
- [x] Fix sampling intervals and disabled-module work; preserve layout/config.
- [x] Build 1.21.11 Fabric release, run required checks and private scene smoke.
- [x] Produce JAR and source/release ZIPs, checksums, changelog and test scope.

Review focus: foreign Compose precedence, metadata/classloader identity, JNI
namespace preservation, config compatibility, empty HUD state, duration expiry,
lower existing render distances and restoration on disable, no unsupported FPS
claims, no live user profile/server modification.
