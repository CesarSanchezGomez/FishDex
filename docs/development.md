# Development

## Build

```bash
./gradlew build
```

Requires a JDK 25 or newer (the build targets Java 25 bytecode). The jar is
`build/libs/FishDex-<version>.jar`. Dependency versions live in `gradle.properties`; keep the HikariCP
version there in sync with `FishDexLoader`, which downloads it at runtime.

Tests use JUnit 5, a real SQLite database in a temporary folder and a direct executor, so they need no
server. Listeners, commands and menus are checked by hand on a test server.

## Layout

```
com.cesarcosmico.fishdex
├── FishDexPlugin, FishDexLoader   composition root and runtime libraries
├── model/                         records and enums, plain Java
├── service/                       catch, sale, preference and FishDex logic; FishingEngine
├── storage/                       Database (pool, schema, query thread) and the repositories
├── config/                        typed settings, commands.yml, validation, reload holder
├── text/                          messages, name formatting, external placeholders
├── menu/                          menu config, rendering, navigation and actions
├── command/                       Brigadier commands
├── listener/                      Bukkit listeners
└── integration/                   the only packages that import other plugins
    ├── customfishing/
    └── placeholderapi/
```

Dependencies are wired by constructor in `FishDexPlugin`; there are no static instances. `FishingEngine` is
the only interface over CustomFishing, because its API is external and changes, and the services are tested
against a fake of it. Repositories are concrete classes.

## Data

The schema is `src/main/resources/db/{sqlite,mysql}.sql` (MariaDB uses the MySQL file), applied on start
with `CREATE TABLE IF NOT EXISTS`. `schema_version` records the version for future migrations.

| Table | Content |
|---|---|
| `species_stats` | Per player and species: count, min and max size, first and last catch. |
| `species_sales` | Per player and species: units sold on the CustomFishing market. |
| `server_records` | Per species: record size, holder and date, global count, latest catch and biome. |
| `discoveries` | Per species: first player to catch it and when. |
| `player_preferences` | Per player: FishDex order and grouping. |

The key everywhere is the CustomFishing loot id. Timestamps are epoch milliseconds.

## CustomFishing

- Catches are read from `FishingLootSpawnEvent`, not `FishingResultEvent`: CustomFishing only writes
  `ContextKeys.SIZE` while it builds the loot item, in a task that runs after the result event. If the
  context has no size, the size stored on the spawned item is used.
- Sales come from `MarketSellEvent`, mapping each sold item back to its loot id.
- Loot lists, names and icons are read through `CustomFishingEngine`, which caches categories and names and
  clears them on `/fishdex reload` and on `CustomFishingReloadEvent`.

## Threading

- Every query runs on one database thread (`fishdex-db`), which keeps writes in order.
- Repository futures complete on that thread. Code that then touches players or inventories hops back with
  `player.getScheduler().run(...)`, which also skips the work if the player has left.
- CustomFishing is read on the main thread.
- PlaceholderAPI is called on the main thread, while rendering menus; expansions may read Bukkit state.
- On disable, queued writes are drained (up to 10 seconds) before the pool closes.

Architecture conventions shared with the other CesarCosmico plugins are described in the workspace's
`ARCHITECTURE-STANDARD.md`.
