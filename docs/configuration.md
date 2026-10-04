# Configuration

Everything lives in the plugin's data folder:

```
plugins/FishDex/
  config.yml          # language
  database.yml        # storage backend (restart to apply)
  commands.yml        # command names, aliases and permissions (restart to apply)
  lang/en_US.yml      # player messages (one file per language)
  menus/fishdex/
    settings.yml      # global menu settings (not a menu)
    fishdex.yml       # root menu opened by /fishdex
    collections/      # one collection per file; "_"-prefixed files are ignored
```

`/fishdex reload` reloads `config.yml`, the language file and every menu. It loads all of them first and
only applies the result if everything parsed; a file that is not valid YAML aborts the reload, the
previous configuration stays active and the error is shown to the sender and in the console.

`config.yml`, `database.yml` and `commands.yml` are compared with their bundled defaults when they load: an
outdated `config-version`, missing keys and unknown keys (typos, removed settings) are reported as warnings.
Menu files are checked as described in [Menus](#menus).

## config.yml

| Key | Default | Description |
|---|---|---|
| `language` | `en_US` | Loads `lang/<language>.yml`. Shipped: `en_US`, `es_ES`. Missing keys fall back to the bundled file. |

## database.yml

| Key | Default | Description |
|---|---|---|
| `type` | `sqlite` | `sqlite`, `mysql` or `mariadb`. Any other value stops the plugin with a clear error instead of silently using SQLite. |
| `sqlite.file` | `data.db` | Created inside the plugin folder. |
| `mysql.*` / `mariadb.*` | | `host`, `port`, `database`, `username`, `password`, `pool-size`, `properties` (JDBC query string). |

The MySQL and MariaDB drivers are downloaded by Paper on the first start (cached in `libraries/`); SQLite
uses the driver that ships with the server. Changes to this file need a restart.

## Language files

`lang/<language>.yml` holds the chat messages. `<prefix>` inserts the `prefix` value; other placeholders
(`<version>`, `<engine>`, `<error>`) are filled in by the plugin. Menu text is not here: it belongs to each
menu file, because it is part of the menu design.

---

## Menus

- **The folder decides the type.** A file under `collections/` (at any depth) is a **collection**: it
  lists the fish of one CustomFishing category. Any other menu file is an **index** of buttons;
  `fishdex.yml` is the root index and should be the only one.
- The menu id is the file name: `/fishdex <id>` opens it (with tab completion).
- Menus are **self-contained**: nothing is inherited between files. Copy `collections/_example.yml` to
  start a new one. Every block is optional; code fallbacks (`rows: 6`, `content-slots: 18-35`) only keep an
  incomplete file working.
- Adding a fish needs no edit (the list comes from CustomFishing). Adding a collection is one new file in
  `collections/` plus an `open:<id>` button in `fishdex.yml`. To hide a collection, prefix its file name
  with `_` and remove its button.

### settings.yml

Global values shared by every menu; setting them inside a menu file only warns.

| Key | Description |
|---|---|
| `date-format` | Java date pattern used by the date placeholders (default `dd/MM/yyyy`). |
| `sort` | Initial order for players who never chose one. |
| `sort-cycle` | The orders the sort button cycles through. |
| `grouping` | Default discovery grouping: `discovered-first`, `undiscovered-first`, `mixed`. |
| `content-layout` | Default layout for collections: `centered` or `fill`. |
| `sort-labels`, `group-labels` | Text shown for each order and grouping. |
| `grouping-hint` | The `<grouping-hint>` lore line (shown only on the name orders). |

### Menu keys

| Scope | Keys |
|---|---|
| Any menu | `title`, `rows`, `colors`, `filler`, `open-sound`, `decorations`, `buttons` |
| Collections only | `category` (CustomFishing category), `content-slots`, `content-layout`, `discovered`, `undiscovered` |
| Required in a collection | `category`, `discovered`, `undiscovered` |

`rows` is 1 to 6. `content-slots` accepts ranges and lists (`"18-35"`, `"10,11,12,20"`). With
`content-layout: centered`, a collection that fits on one page fills full rows of nine and centres the last
partial row; once it paginates, entries are packed left to right so pages stay aligned.

### Icons

An icon is a `material` plus a `components:` block named after the vanilla item components:

```yaml
material: PLAYER_HEAD
components:
  custom-name: "{colors}<b>Next »</b>{/colors}"   # MiniMessage
  lore: [ " <gray>...</gray> " ]                  # MiniMessage, one entry per line
  item-model: mypack:gui/coral/thorn_coral        # resource-pack model
  custom-model-data: 51001                        # a number or a list of numbers
  profile: "<base64 texture>"                     # player-head texture
  hide-tooltip: true
```

Unknown materials, unknown component keys and invalid values are reported when the menu loads (with the
menu, button or decoration they belong to) and skipped.

In lore, `<newline>` splits a line and a line that renders to no text is dropped, so a placeholder can add
or hide lines.

### Colours

`colors` takes one colour (solid) or several (gradient). Wrap text in `{colors}…{/colors}` and it takes the
menu's colours, so frames, controls and the undiscovered marker match the page without duplicating
templates.

```yaml
colors: "#FF0D0D"
colors: ["#FF7373", "#FF0D0D"]
```

### Decorations

Everything that is not a button: a map of entries, each with `slots` and an icon. Decorations are drawn in
order, buttons are drawn on top, and decorations resolve the progress placeholders, so a one-slot
decoration can be a live progress panel.

```yaml
decorations:
  corners: { slots: "0,8,45,53", material: BLUE_STAINED_GLASS_PANE, components: { hide-tooltip: true } }
  progress:
    slots: "4"
    material: SPYGLASS
    components:
      custom-name: "{colors}<b>Progress</b>{/colors}"
      lore: [ " <discovered>/<total> <progress-bar> " ]
```

### Buttons

A button is a `slot`, an icon and an `action`:

| `action` | Effect |
|---|---|
| `open:root` | Opens `fishdex.yml`. |
| `open:<id>` | Opens that menu. |
| `previous_page`, `next_page` | Changes page; only shown when there is a page to go to. |
| `close` | Closes the menu. |
| `sort` | Collections only: left/right click cycles the order, shift-click cycles the grouping. |

An unknown action is reported at load and the button does nothing. `page: N` pins a button to page `N`;
without it, controls (`open:root`, `previous_page`, `next_page`, `close`, `sort`) show on every page and
other buttons on the first one. `page: all` shows any button on every page. The number of pages of an index
is its highest `page`.

Sounds are a key (`"minecraft:ui.button.click"`, resource-pack keys work) or an object
`{ key, volume, pitch, source }`. `open-sound` plays when the menu opens; a button's `sound` plays when it
is clicked.

### Fish entries

A discovered fish shows the real CustomFishing item; `discovered.components.custom-name` (optional) and
`discovered.components.lore` replace its name and lore. An undiscovered fish uses the `undiscovered` icon.

| Placeholder | Value |
|---|---|
| `<name>`, `<name-plain>` | Fish name, formatted or plain (to tint it with `{colors}`). |
| `<count>`, `<sold>` | Times you caught it, units you sold on the CustomFishing market. |
| `<min>`, `<max>` | Your smallest and largest catch. |
| `<first-catch>`, `<last-catch>` | Your first and last catch dates. |
| `<discoverer>`, `<discovered-date>` | First player on the server to catch it, and when. |
| `<record>`, `<record-holder>`, `<record-date>` | Server record size, holder and date. |
| `<last-size>`, `<last-catcher>`, `<last-catch-date>`, `<last-biome>` | Latest catch on the server; the biome is translated by each client. |
| `<total-global>` | Catches of this species on the whole server. |

### Progress, pages and sorting

- Progress (decorations, and `open:<id>` buttons in an index): `<discovered>`, `<total>`,
  `<discovered-padded>`, `<total-padded>`, `<progress>`, `<progress-bar>`, `<player-caught>`,
  `<global-caught>`, `<player>`. In an index, decorations show the overall progress and each `open:<id>`
  button shows its collection's progress.
- Pages: `<page>` and `<page-count>`, zero-padded (`01/02`). They can be used in the title: the menu is
  reopened when the page changes, because Paper cannot rename an open inventory.
- Sort button: `<sort-mode>`, `<next-sort-mode>`, `<prev-sort-mode>`, `<group-mode>`, `<grouping-hint>`.

Orders: `engine` (CustomFishing's order), `name`, `name-desc`, `count`, `count-asc`, `sold`, `sold-asc`,
`max-size`, `min-size`, `biome`. The grouping only applies to `name` and `name-desc`; on the data orders the
undiscovered entries always go last. Both choices are saved per player.

### PlaceholderAPI

With PlaceholderAPI installed, any menu text also accepts `%placeholder%`, resolved for the viewer. The value
is inserted as text and never interpreted as MiniMessage, so a player-controlled value (a nickname, a
prefix) cannot inject formatting or click actions. Legacy colour codes in the value are kept.
