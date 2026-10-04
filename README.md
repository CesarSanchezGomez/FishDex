# FishDex

A configurable FishDex for [CustomFishing](https://github.com/Xiao-MoMi/Custom-Fishing). Players browse
every fish by collection, see which ones they have discovered, their own sizes and catch counts, how many
they sold, and the server records and first discoverer of each species. CustomFishing stays the fishing
engine; FishDex records what it does not store.

- **Server:** Paper 26.2 or newer, Java 25.
- **Required:** CustomFishing 2.3.24 or newer.
- **Storage:** SQLite (default), MySQL or MariaDB.
- **Optional:** PlaceholderAPI, for `%placeholders%` in menu text.

## Commands

| Command | Permission | Default |
|---|---|---|
| `/fishdex [menu]` | `fishdex.command.open` | everyone |
| `/fishdex reload` | `fishdex.command.reload` | op |
| `/fishdex version` | `fishdex.command.version` | op |

Names, aliases and permissions can be changed in `commands.yml`; see [docs/commands.md](docs/commands.md).

## Configuration

Menus are YAML files under `menus/fishdex/`: one root menu and one file per collection, each mapped to a
CustomFishing category. See [docs/configuration.md](docs/configuration.md).

## Build

```bash
./gradlew build
```

The jar is written to `build/libs/`. The database drivers are not bundled: Paper downloads them on the
first start. See [docs/development.md](docs/development.md).
