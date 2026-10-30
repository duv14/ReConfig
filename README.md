# ReConfig 3.3.1

Standalone Fabric client mod for Minecraft 1.21.11 by **duv14**, adapted from OneConfig by Polyfrost and contributors. No separate OneConfig mod is required. Install compatible Fabric API and Fabric Language Kotlin.

## 30 October 2026 hotfix

Re-download 3.3.1 to get cached Particle Changer rendering, mouse-button bindings (including Zoom on Mouse 4/5), exclusive keybind recording and renderer safety guards. Replace the old JAR and restart Minecraft. The version number stays 3.3.1. See CHANGELOG.md and VERIFICATION.md for scope.

## UI compatibility and HUD update

- Private Compose packages prevent a separate Compose Multiplatform mod from replacing ReConfig's UI classes. The matching Skiko renderer remains bundled with its required native libraries.
- Projectile Counter has separate arrows, pearls, wind charges, snowballs and eggs switches, plus an option to hide zero counts.
- Potion Effects has native effect icons, remaining timers, level/name controls and an empty-state toggle. Move and resize it in Edit HUD.
- Hide Scoreboard toggles the vanilla sidebar locally without changing server scores or teams.
- Disabled HUDs skip unnecessary sampling, module lookups use indexes, and custom HUD refresh intervals use the correct time units. FPS Boost preserves already-lower distances; performance still depends on the scene and hardware.

## Profiles and HUD update

- Profiles opens on your own editable profile, with a larger articulated skin preview, idle/walk/wave controls and a Minecraft-font nametag.
- The read-only account name sits beside a tag field limited to five letters/numbers; the bio and emoji controls update the preview immediately.
- An animated search control opens a dedicated public-profile search view with profile cards.
- Saved tags and selected emoji render beside live Minecraft nametags. Your own tag can appear in third person; other ReConfig clients see it only when your profile is public.
- Profile publication remains a separate, explicit opt-in. Private profiles are hidden from search.
- HUD labels no longer force text shadows, and custom text cards respect merged-background ownership to avoid drawing dark backgrounds twice.

## Build and install

```bat
gradlew.bat clean buildAndCollect check
```

Use JDK 21. The built mod is `build\libs\ReConfig-3.3.1.jar`. See [BUILD-INSTRUCTIONS.md](BUILD-INSTRUCTIONS.md), [CHANGELOG.md](CHANGELOG.md), and [VERIFICATION.md](VERIFICATION.md).

The hardcoded social backend is `https://reconfig-chat.duv14-reconfig-api.workers.dev`. Five-character tags require deploying the included Worker update; migration 0007 does not need to be repeated if already applied. See [backend/README.md](backend/README.md). Privacy hosting source is under `docs/site/policy/`.

## License

ReConfig is distributed under GPL-3.0-only using the GPL option in OneConfig's Additional Terms v1.1. Preserve all upstream and third-party notices. See `LICENSE-RECONFIG.txt`, `LICENSE`, `ATTRIBUTIONS.md`, and `THIRD_PARTY_NOTICES.md`. Publish the corresponding source alongside the JAR.
