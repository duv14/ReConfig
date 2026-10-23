-- Curated public Java recommendations. Addresses and sources: backend/SERVER-SOURCES.md.
CREATE TABLE IF NOT EXISTS public_server_recommendations(server_id TEXT PRIMARY KEY, rank INTEGER NOT NULL);
INSERT OR IGNORE INTO public_servers(id,name,address,description,listed) VALUES
('donutsmp','DonutSMP','donutsmp.net','Competitive survival SMP with PvP, trading and an economy.',1),
('hypixel','Hypixel','mc.hypixel.net','Minigames including Bed Wars, SkyWars, SkyBlock and Duels.',1),
('cubecraft','CubeCraft','play.cubecraft.net','Java minigames with EggWars, SkyWars and parkour.',1),
('wynncraft','Wynncraft','play.wynncraft.com','An RPG adventure with quests, classes and a large world to explore.',1),
('mcc-island','MCC Island','play.mccisland.net','Team minigames, Sky Battle, Battle Box and Parkour Warrior.',1);

INSERT OR IGNORE INTO public_server_recommendations(server_id,rank) VALUES
('donutsmp',10),('hypixel',20),('cubecraft',30),('wynncraft',40),('mcc-island',50);
