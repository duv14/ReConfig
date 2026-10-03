CREATE TABLE users (uuid TEXT PRIMARY KEY, name TEXT NOT NULL COLLATE NOCASE UNIQUE, token_hash TEXT NOT NULL, created_at INTEGER NOT NULL);
CREATE TABLE requests (sender TEXT NOT NULL, recipient TEXT NOT NULL, created_at INTEGER NOT NULL, PRIMARY KEY(sender, recipient));
CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT NOT NULL, recipient TEXT NOT NULL, body TEXT NOT NULL, created_at INTEGER NOT NULL, delivered INTEGER NOT NULL DEFAULT 0);
CREATE INDEX messages_inbox ON messages(recipient, delivered, id);
