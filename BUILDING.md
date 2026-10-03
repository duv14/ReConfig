# Building and deploying ReConfig

## Requirements

- Windows 10/11 or Linux
- Java 21 (`java -version` must report 21)
- Node.js 20 or newer
- Internet access for the first Gradle and npm runs

## Deploy the social service

Open a terminal in `backend` and run:

```bat
npm install
npx wrangler login
npx wrangler d1 migrations apply reconfig-chat --remote
npx wrangler deploy
```

If you already ran `npm install` and logged in, do not repeat those two steps. For this update you **must** run both the remote migration command and `npx wrangler deploy`.

The client is already hardcoded to:

`https://reconfig-chat.duv14-reconfig-api.workers.dev`

No domain or client-side configuration is needed. The new `0003_social_schema_repair.sql` migration repairs databases where the earlier social migration was recorded before every table was created.

Verify it with:

```bat
curl https://reconfig-chat.duv14-reconfig-api.workers.dev/v1/health
```

## Build the standalone Fabric mod

From the repository root:

```bat
gradlew.bat --stop
gradlew.bat clean buildAndCollect
```

The Fabric 1.21.11 jar appears in `build\libs`. Put that ReConfig jar in the Minecraft `mods` folder. Do not install a separate OneConfig jar.

If Gradle reports the wrong Java version, set `JAVA_HOME` to a Java 21 JDK before running the wrapper. If the wrapper download was interrupted, delete only the incomplete `gradle-9.5.0-bin` folder under `%USERPROFILE%\.gradle\wrapper\dists` and run the command again.

## Test the backend without deploying

```bat
cd backend
npm test
```

The test suite covers explicit acceptance, automatic reciprocal friendship, matching-server authorization for messages, cross-server rejection, and server invitations.
