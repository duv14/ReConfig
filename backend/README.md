# ReConfig social Worker

The existing `reconfig-chat` Worker and D1 binding are preserved. **Redeployment is required** for the profile, directory, notification cursor and enrollment changes.

```bat
cd backend
npm install
npm test
npx wrangler d1 migrations apply reconfig-chat --remote
npx wrangler deploy
```

Apply migrations before deploying the new Worker. New migration: `0007_profiles_directory_notifications.sql`. It adds tables, indexes and triggers without discarding existing accounts, messages or friendships, and can be reapplied safely. Earlier migrations remain unchanged and Wrangler tracks their application. Back up the production D1 database using the Cloudflare dashboard/export before deployment. Node 22.13+ is required for the tests' built-in SQLite API. Migration 0008 and the Worker update were deployed and checked against the live service on 23 October 2026.

## Five-character tag update

The Profiles redesign accepts tags of up to five ASCII letters/numbers. Saved public tags can appear beside live Minecraft nametags on ReConfig clients; private own tags are kept local. Public lookups are cached for five minutes and missing/private results for ten minutes, with a bounded background queue. Redeploy the included Worker to enable this limit. Tags require migration 0007; the public recommendation catalog additionally requires migration 0008.

## Authentication

Clients use a randomly generated bearer token; only its SHA-256 hash is stored. Existing same-token/same-name enrollment remains compatible. A new identity, replacement token, or username change requires Minecraft session ownership proof:

1. Generate a fresh random 20-byte lowercase hex `serverId` (40 characters).
2. Invoke Minecraft's authenticated session service `joinServer` with that ID and the current account session.
3. POST `/v2/enroll` with `{uuid,name,token,serverId}`.
4. The Worker verifies UUID and name via Mojang `sessionserver.mojang.com/session/minecraft/hasJoined`.

The Microsoft/Minecraft access token is **never sent to this Worker**. Successful verified enrollment adds a session for the new computer without removing other computers’ sessions. Known session tokens can renew without repeating proof; username changes still require proof. Token reuse across different UUIDs is rejected. Existing historical registrations are not retroactively proven; this migration cannot guarantee that accounts enrolled by the old trust-on-first-use protocol were genuine. Session-service failure fails enrollment closed. Live Minecraft session verification succeeded on 23 October 2026; unit tests additionally use mocked session responses.

## API

All routes below require the bearer token. Public means discoverable to other authenticated ReConfig users.

- `GET /v2/profile/me`: `{profile:{uuid,name,bio,tag,emoji,public}}`. Missing profiles are private.
- `POST /v2/profile/me`: `{bio,tag,emoji,public,publicationConsent}`. Bio maximum 280 characters; tag 0–5 ASCII letters/numbers; emoji one of empty string, `cute`, `kiss`, `kiss-closed`, `pray`, `rose`, `sob`, `sob-blue`. Publishing a private profile requires `publicationConsent:true`; general policy acceptance is not publication consent. Unpublishing immediately removes discoverability. Control and bidirectional override characters are rejected. Render content as plain text.
- `GET /v2/profiles?q=`: `{profiles:[{uuid,name,bio,tag,emoji}]}`; case-insensitive name/tag substring search, maximum 50. No bearer hash, server presence or private profile fields are exposed.
- `GET /v2/profiles/<uuid>`: `{profile:...}`, or 404 when private/missing.
- `GET /v2/servers?q=&sort=name|players`: `{servers:[{id,name,address,description,icon,players}]}`, maximum 100 explicitly listed records. `players:null` means unknown. Client Minecraft ping supplies live latency/player data; the Worker does not invent it.
- `GET /v2/sync?after=<message-id>&afterRequest=<request-event-id>&afterDeletion=<deletion-event-id>`: existing `cursor`, `messages`, `invitations` plus `requests:[{id,sender,name,created_at}]`, `requestCursor`, `deleted:[{id,message_id,sender,recipient,deleted_at}]`, `deletionCursor`. Persist each cursor separately. Apply deletions to local conversations and quoted replies. Suppress historical popups on initial synchronization; incrementally page until caught up. Pending requests only are returned. Message bodies are removed on deletion; tombstones remain for cross-device synchronization. Invitations remain pending until explicitly acknowledged with `POST /v2/invitations/ack` and `{ids:[...]}` (maximum 50 IDs, recipient-scoped). Lost fetch responses do not acknowledge invitations. Persist/deduplicate displayed IDs; exactly-once display across a crash is not guaranteed.

Presence remains fresh for 150 seconds to tolerate the client's 60-second heartbeat. Cross-server messaging is independent of presence/server hashes. SQL indexes support incoming messages, requests, deletion events, invitations and both friendship directions. Per-account fixed-minute write limits are 60 messages and 10 profile saves/friend request attempts/server invitation attempts; server invitation pairs additionally have a 30-second cooldown. Responses expose friendly errors and `retryAfterSeconds`; clients should back off, poll fuller state only in the social UI, and avoid parallel duplicate pollers. Read requests additionally have a bounded best-effort 120/minute per-account isolate budget; this resets on isolate restart and is not a global abuse limit. These controls are not a substitute for Cloudflare edge abuse controls on unauthenticated enrollment or distributed requests. Negative/error profile lookups are cached for 30 seconds and successful lookups for 10 minutes in a bounded isolate cache; existing D1 contact rows provide durable lookup reuse.

## Curating the public directory

Migration 0007 creates an empty directory. Migration 0008 adds five verified public Java recommendations with a separate ranking table. Sources are recorded in SERVER-SOURCES.md. Operators may add other addresses explicitly intended for public publication. Never populate this table from `presence`, invitations, private server history or technical logs. There is no client-writable directory endpoint.

For example, prepare reviewed SQL with a real public server's details (the following address is documentation-only):

```sql
INSERT INTO public_servers(id,name,address,description,icon,players,listed)
VALUES('example','Example server','play.example.org','Approved public description','',NULL,1)
ON CONFLICT(id) DO UPDATE SET name=excluded.name,address=excluded.address,
 description=excluded.description,icon=excluded.icon,listed=excluded.listed;
```

Apply reviewed SQL with `npx wrangler d1 execute reconfig-chat --remote --file=directory.sql`. Set `listed=0` to remove a listing. Use only approved HTTPS icon URLs; verify the public address and Minecraft version manually before listing.

## Data and verification limits

Messages are not end-to-end encrypted. There is no automatic retention/deletion scheduler; profile withdrawal hides publication but does not delete account or message records. Account deletion requests require operator handling. Profile lookup tries Minecraft Services, Mojang, PlayerDB and Ashcon, with timeouts and a stored local D1 contact record. Privacy disclosures must name these actual providers.

`npm test` executes the real migration SQL against temporary in-memory SQLite, route authorization, cross-server/offline messaging, replies/deletion, profile consent/validation, private-directory isolation, request/deletion cursors, presence, rate limits and migration preservation/index selection. This verifies local SQL behavior, not Cloudflare production quotas, live network services, or delivery between real Minecraft clients.

## Startup, invitation acknowledgment and conversation history

On each new client session call `GET /v2/sync?bootstrap=1`: it returns recipient-scoped `cursor`, `requestCursor`, `deletionCursor`, `invitationCursor` highwater IDs and empty event arrays. This suppresses historical notification replay without marking invitations delivered. Rebuild displayed conversations from message history, then poll using all four `after`, `afterRequest`, `afterDeletion`, `afterInvitation` parameters. Persist each returned cursor only after processing its page. New events arriving after bootstrap remain available. `/v2/events` can still retrieve all pending invitations for a dedicated inbox; it has no acknowledgment side effect. ACK only invitations actually handled by the client.

`GET /v2/messages?with=<uuid>&after=0` returns the latest 200 messages in ascending display order. `?before=<beforeCursor>` retrieves the previous page, also ascending. Both return `beforeCursor` (oldest returned), `cursor` (newest returned), and `hasMore` in the requested direction. `?after=<cursor>` retrieves the next 200 newer messages. Do not pass both `before` and a nonzero `after`. Empty pages preserve the forward cursor. Deletion synchronization separately removes cached messages and quoted previews.

## Mojang verification transport

`src/index.js` installs the official session-server transport and delegates API routing to `src/worker.js`. When Mojang blocks the standard Cloudflare fetch with HTTP 403/429/503, a bounded TLS socket checks the same official host. Ownership is still established solely by Mojang and must match both UUID and username. No third-party profile lookup can authorize an account. Upstream transport errors return a temporary service error, not a false ownership rejection.
