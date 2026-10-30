# Building ReConfig 3.3.1

Use a 64-bit JDK 21. Extract the source archive to a writable folder and open a terminal in the extracted project root. The Gradle wrapper downloads its dependencies on the first run.

```bat
gradlew.bat clean buildAndCollect check
```

Wait for `BUILD SUCCESSFUL`. Install the collected standalone mod at `build\libs\ReConfig-3.3.1.jar`. Use this collected JAR: intermediate JARs under bootstrap or minecraft have not passed through private runtime packaging.

## Install

Use Minecraft 1.21.11, Java 21, Fabric Loader, Fabric API >=0.141.5+1.21.11 and Fabric Language Kotlin >=1.13.8+kotlin.2.3.0. Put the ReConfig JAR in the instance's `mods` folder, replacing the previous ReConfig JAR, then restart Minecraft. A separate OneConfig installation is not required. The Compose/Skiko native runtime libraries are retained because this UI renderer requires them.

## 30 October hotfix

The version stays 3.3.1. While recording a binding, press a keyboard key or mouse button. Mouse 4/5 work for module activation and regular config bindings. Escape cancels module capture; Backspace/Delete clears it. Particle Changer caches its settings outside per-particle rendering. The supplied NVIDIA native crash remains unproven as fixed; see VERIFICATION.md.

## New controls

- Modules -> Projectile Counter: keep Pearls and Wind charges enabled and disable the other types for a two-type counter.
- Modules -> Potion Effects: choose names, levels, timers, empty-state behavior and background. Open Edit HUD to position and scale it.
- Modules -> Hide Scoreboard: enable to hide the sidebar; disable to restore it. A keybind is optional.
- FPS Boost caps render/entity distances and restores the previous values when disabled. It is not a guaranteed FPS increase or a replacement for a performance mod.

This update changes the client only. Its UI/HUD fixes require no Worker redeployment or database migration.

## Updated social backend

For a separate backend deployment, apply the included migrations (including 0008 for public server recommendations) before deploying. Both migration 0008 and the Worker update have already been applied to your current service:

```bat
cd backend
npm install
npm test
npx wrangler deploy
```

For a database that has not yet applied the included migrations, apply them before deploying: `npx wrangler d1 migrations apply reconfig-chat --remote`. See `backend/README.md` for account login, public server directory curation and API details. The mod build does not deploy the Worker or privacy website.

## Live tags

Save your tag to apply it in gameplay. Your own saved tag appears in third person when Nick Hider and vanilla HUD hiding are off. Other ReConfig users see your tag only if you publish your profile. Unmodified clients do not render ReConfig tags. Public tags are cached for five minutes, so other clients may take a few minutes to see edits. Name visibility rules for other players are preserved.

## Verification scope

See `VERIFICATION.md`. A successful build and offscreen UI render do not replace testing inside a running Minecraft client.
