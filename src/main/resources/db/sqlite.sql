-- FishDex baseline schema (SQLite). Idempotent DDL.

CREATE TABLE IF NOT EXISTS schema_version (
  version INTEGER PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS species_stats (
  uuid TEXT NOT NULL,
  loot_id TEXT NOT NULL,
  count INTEGER NOT NULL DEFAULT 0,
  min_size REAL,
  max_size REAL,
  first_catch_at INTEGER,
  last_catch_at INTEGER,
  PRIMARY KEY (uuid, loot_id)
);

CREATE TABLE IF NOT EXISTS species_sales (
  uuid TEXT NOT NULL,
  loot_id TEXT NOT NULL,
  sold_count INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (uuid, loot_id)
);

CREATE TABLE IF NOT EXISTS server_records (
  loot_id TEXT PRIMARY KEY,
  best_size REAL NOT NULL,
  holder_uuid TEXT NOT NULL,
  achieved_at INTEGER NOT NULL,
  total_global_count INTEGER NOT NULL DEFAULT 0,
  last_catcher_uuid TEXT,
  last_size REAL,
  last_caught_at INTEGER,
  last_biome TEXT
);

CREATE TABLE IF NOT EXISTS discoveries (
  loot_id TEXT PRIMARY KEY,
  first_player_uuid TEXT NOT NULL,
  discovered_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS player_preferences (
  player_uuid TEXT PRIMARY KEY,
  sort_mode TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
