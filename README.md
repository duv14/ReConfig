# ReConfig 1.21.11

Standalone Fabric 1.21.11 client mod by **duv14**, built from the OneConfig v1 UI foundation and requiring no separate OneConfig installation.

## 3.2.0 stability release

This version repairs HUD visibility, Fullbright, Custom Crosshair, Freelook,
friends/messages polling, database-limit handling, and the native Compose/Skia
startup crash. It also adds Modrinth release documentation. See `CHANGELOG.md`,
`PRIVACY.md`.

The v29 update added the requested sounds, Zoom, HUDs, brightness/movement/privacy
controls, and integrated Hitbox Categories / Team Highlight editors. It also
changes Freelook mouse capture. Read `STATUS-v29.md` for controls, implementation
details and validation limits. The v28 duplicate-license packaging fix is retained.
The repository CI builds only the supported 1.21.11 release task. Fabric API
must still be installed separately; the dependency declaration does not bundle it.

## What changed

- ReConfig branding and logo
- Bricolage Grotesque typography
- ReConfig open, close, and notification sounds
- PolyGlass Dark forced as the interface theme
- Social → Friends and Messages, with explicit request acceptance, skin heads, status, invitations, and cross-server chat
- Quality of Life → Modules, with the supplied SVG icons and retained fallback artwork
- Miscellaneous → Settings
- Updates → Changelog, loaded from `https://duv14.com/changelogsreconfig` with an offline cache
- Persistent enable state, controls-style keybind capture, sliders, choices, and a full color picker
- Hardcoded backend: `https://reconfig-chat.duv14-reconfig-api.workers.dev`
- Compose/Skia and Fabric Language Kotlin nested into the distributable

## Build

Use Java 21:

```bat
gradlew.bat buildAndCollect
```

The standalone Fabric 1.21.11 jar is written to `build\libs`. Only place that ReConfig jar in the Minecraft `mods` folder; do not install OneConfig separately. See `BUILDING.md` for backend migration and troubleshooting commands.

## License

ReConfig's adapted program is distributed under GPL-3.0-only using the GPL option in OneConfig's Additional Terms v1.1. Original upstream licenses/notices and third-party licenses remain intact. See `LICENSE-RECONFIG.txt`, `LICENSE`, `ATTRIBUTIONS.md`, and `RELEASE-CHECKLIST.md`. Publish the exact corresponding source alongside your release.
