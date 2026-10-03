CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY,user_uuid TEXT NOT NULL,updated_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS friendships(user_low TEXT NOT NULL,user_high TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(user_low,user_high));
CREATE TABLE IF NOT EXISTS presence(user_uuid TEXT PRIMARY KEY,server_hash TEXT,last_seen INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS messages_v2(id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL,recipient TEXT NOT NULL,body TEXT NOT NULL,created_at INTEGER NOT NULL,acknowledged INTEGER NOT NULL DEFAULT 0);
CREATE TABLE IF NOT EXISTS invitations(id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL,recipient TEXT NOT NULL,server_address TEXT NOT NULL,created_at INTEGER NOT NULL,acknowledged INTEGER NOT NULL DEFAULT 0);
CREATE INDEX IF NOT EXISTS messages_v2_pair ON messages_v2(sender,recipient,id);
INSERT OR IGNORE INTO sessions(token_hash,user_uuid,updated_at) SELECT token_hash,uuid,created_at FROM users;
INSERT OR IGNORE INTO friendships(user_low,user_high,created_at)
SELECT MIN(a.sender,a.recipient),MAX(a.sender,a.recipient),MIN(a.created_at,b.created_at)
FROM requests a JOIN requests b ON b.sender=a.recipient AND b.recipient=a.sender;
