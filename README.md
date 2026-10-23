# ReConfig 3.3.0

Standalone Fabric client mod for Minecraft 1.21.11 by **duv14**, adapted from OneConfig by Polyfrost and contributors. No separate OneConfig mod is required. Fabric API must be installed.

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

Use JDK 21. The built mod is `build\libs\ReConfig-3.3.0.jar`. See [BUILD-INSTRUCTIONS.md](BUILD-INSTRUCTIONS.md), [CHANGELOG.md](CHANGELOG.md), and [VERIFICATION.md](VERIFICATION.md).

The hardcoded social backend is `https://reconfig-chat.duv14-reconfig-api.workers.dev`. Five-character tags require deploying the included Worker update; migration 0007 does not need to be repeated if already applied. See [backend/README.md](backend/README.md). Privacy hosting source is under `docs/site/policy/`.

## License

ReConfig is distributed under GPL-3.0-only using the GPL option in OneConfig's Additional Terms v1.1. Preserve all upstream and third-party notices. See `LICENSE-RECONFIG.txt`, `LICENSE`, `ATTRIBUTIONS.md`, and `THIRD_PARTY_NOTICES.md`. Publish the corresponding source alongside the JAR.
