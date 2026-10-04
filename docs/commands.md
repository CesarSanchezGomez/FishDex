# Commands and permissions

| Command | Permission | Default |
|---|---|---|
| `/fishdex` | `fishdex.command.open` | everyone |
| `/fishdex <menu>` | `fishdex.command.open` | everyone |
| `/fishdex reload` | `fishdex.command.reload` | op |
| `/fishdex version` | `fishdex.command.version` | op |

Aggregates: `fishdex.use` (open), `fishdex.admin` (reload and version) and `fishdex.*`.

Opening a menu is for players only; `reload` and `version` also work from the console. Brigadier matches
the literals first, so `reload` and `version` cannot be used as menu ids.

A command or subcommand the sender has no permission for is hidden: it does not appear in tab completion
and behaves as an unknown command.

## commands.yml

Renames, aliases, permissions and disabling are done here. Commands are registered once at startup, so
**changes need a restart**; `/fishdex reload` does not apply them.

```yaml
fishdex:                     # internal id, do not change
  enabled: true              # false: the command is not registered
  name: fishdex              # what players type
  aliases: []
  permission: fishdex.command.open
  subcommands:
    reload:  { permission: fishdex.command.reload }
    version: { permission: fishdex.command.version }
```

A value you leave out keeps the built-in default. Examples:

```yaml
fishdex:
  name: dex
  aliases: [ fd ]
  subcommands:
    reload: { permission: myserver.staff.reload }
```
