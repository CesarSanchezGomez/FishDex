-- FishDex baseline schema (MySQL / MariaDB). Idempotent DDL.

CREATE TABLE IF NOT EXISTS schema_version (
  version INT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS species_stats (
  uuid VARCHAR(36) NOT NULL,
  loot_id VARCHAR(128) NOT NULL,
  count BIGINT NOT NULL DEFAULT 0,
  min_size DOUBLE,
  max_size DOUBLE,
  first_catch_at BIGINT,
  last_catch_at BIGINT,
  PRIMARY KEY (uuid, loot_id)
);

CREATE TABLE IF NOT EXISTS species_sales (
  uuid VARCHAR(36) NOT NULL,
  loot_id VARCHAR(128) NOT NULL,
  sold_count BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (uuid, loot_id)
);

CREATE TABLE IF NOT EXISTS server_records (
  loot_id VARCHAR(128) PRIMARY KEY,
  best_size DOUBLE NOT NULL,
  holder_uuid VARCHAR(36) NOT NULL,
  achieved_at BIGINT NOT NULL,
  total_global_count BIGINT NOT NULL DEFAULT 0,
  last_catcher_uuid VARCHAR(36),
  last_size DOUBLE,
  last_caught_at BIGINT,
  last_biome VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS discoveries (
  loot_id VARCHAR(128) PRIMARY KEY,
  first_player_uuid VARCHAR(36) NOT NULL,
  discovered_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS player_preferences (
  player_uuid VARCHAR(36) PRIMARY KEY,
  sort_mode VARCHAR(32) NOT NULL,
  updated_at BIGINT NOT NULL
);
