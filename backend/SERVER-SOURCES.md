# Public server recommendation sources

Starter recommendations are editorial, not personalized rankings. Names and Java addresses were checked on 23 October 2026. Counts, icons and ping come from the actual server status handshake; no player numbers are seeded.

- DonutSMP — `donutsmp.net`: https://store.donutsmp.net/
- Hypixel — `mc.hypixel.net`: https://support.hypixel.net/hc/en-us/articles/360019495360-How-to-Join-the-Hypixel-Server
- CubeCraft — `play.cubecraft.net`: https://help.cubecraft.net/en/article/how-do-i-join-cubecraft-java-edition-15kob0/
- Wynncraft — `play.wynncraft.com`: https://wynncraft.wiki.gg/wiki/Newcomer%27s_Guide
- MCC Island — `play.mccisland.net`: https://mcchampionship.com/island/

Migration 0008 inserts only these public listings and their presentation order. Existing records are preserved with INSERT OR IGNORE. Operators can hide an entry by setting public_servers.listed to 0; recommendations never override this choice. Applying the migration again preserves existing users, messages and directory records.
