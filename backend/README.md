# ReConfig cross-server backend

This Worker is intentionally small enough for Cloudflare's free Workers and D1 allowances. It uses HTTPS polling rather than a permanently open socket.

## Deploy

1. Install Node.js 20+ and run `npm install` in this directory.
2. Run `npx wrangler login`.
3. Run `npx wrangler d1 migrations apply reconfig-chat --remote`.
4. Run `npx wrangler deploy`.
5. Test `https://reconfig-chat.duv14-reconfig-api.workers.dev/v1/health`.

If dependencies are already installed, do not reinstall them. This release adds
message reply metadata, so run the migration command once before deploying.

The supplied `wrangler.toml` already contains the D1 database ID and the required `DB` binding. The client URL is hardcoded; no domain or client configuration is needed.

The generated bearer token is stored in the local Minecraft config. This prevents casual impersonation after enrollment but is not Microsoft account verification. Do not use ReConfig chat for private or sensitive information.
