# Building ReConfig 3.3.0

Use a 64-bit JDK 21. Extract the source archive to a writable folder and open a terminal in the extracted project root. The Gradle wrapper downloads its dependencies on the first run.

```bat
gradlew.bat clean buildAndCollect check
```

Wait for `BUILD SUCCESSFUL`. The standalone mod is collected at `build\libs\ReConfig-3.3.0.jar`. This exact command passed during local verification on 22 October 2026, including the corrected Minecraft mixin audit.

## Install

Use Minecraft 1.21.11, Fabric Loader and Fabric API compatible with 1.21.11 (metadata requires Fabric API >=0.141.5+1.21.11). Put the ReConfig JAR in the instance's `mods` folder. A separate OneConfig installation is not required. The Compose/Skiko native runtime libraries are retained because this UI renderer requires them.

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
