package com.cesarcosmico.fishdex.menu;

import com.cesarcosmico.fishdex.model.DiscoveryGrouping;
import com.cesarcosmico.fishdex.model.SortMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Loads {@code menus/fishdex/}: {@code settings.yml} holds the global settings, {@code fishdex.yml} is the
 * root index, and each file under {@code collections/} is a collection whose id is the file name
 * ({@code _}-prefixed files are ignored). Problems inside a menu only warn; a file that is not valid YAML
 * fails the whole load so a reload can keep the previous menus.
 */
public final class MenusConfig {

    private static final String BASE = "menus/fishdex";
    private static final String SETTINGS_FILE = "settings.yml";
    private static final String COLLECTIONS_DIR = "collections";
    private static final String ROOT_MENU_ID = "fishdex";
    private static final List<String> DEFAULT_RESOURCES = List.of(
            BASE + "/settings.yml",
            BASE + "/fishdex.yml",
            BASE + "/collections/_example.yml",
            BASE + "/collections/common.yml",
            BASE + "/collections/singular.yml",
            BASE + "/collections/rare.yml",
            BASE + "/collections/epic.yml",
            BASE + "/collections/legendary.yml",
            BASE + "/collections/mythic.yml",
            BASE + "/collections/divine.yml",
            BASE + "/collections/complete.yml");
    private static final Set<String> GLOBAL_ONLY_KEYS =
            Set.of("date-format", "sort", "sort-cycle", "sort-labels", "grouping", "group-labels", "grouping-hint");
    private static final String DEFAULT_GROUPING_HINT =
            " <newline> <gray>Grouping: <white><group-mode></white>. <newline> <gray>Shift-click on A-Z / Z-A. ";
    private static final Set<String> COLLECTION_ONLY_KEYS =
            Set.of("category", "content-slots", "content-layout", "discovered", "undiscovered");
    private static final Set<String> COLLECTION_REQUIRED_KEYS =
            Set.of("category", "discovered", "undiscovered");

    public record Decoration(int[] slots, MenuIcon icon) {
    }

    /** {@code page == 0} means every page. */
    public record Button(int slot, int page, MenuAction action, MenuIcon icon, ClickSound sound) {
    }

    public record Settings(String dateFormat, SortMode sort, List<SortMode> sortCycle,
                           Map<SortMode, String> sortLabels, DiscoveryGrouping grouping,
                           Map<DiscoveryGrouping, String> groupLabels, String groupingHint,
                           ContentLayout.Mode contentLayout) {
    }

    public record Menu(String id, boolean collection, String category, String title, int rows,
                       int[] contentSlots, ContentLayout.Mode contentLayout,
                       MenuIcon filler, List<String> colors,
                       List<Decoration> decorations, List<Button> buttons, ClickSound openSound, int indexPageCount,
                       List<String> discoveredLore, String discoveredName, MenuIcon undiscovered) {
    }

    private final Settings settings;
    private final Map<String, Menu> menus;

    private MenusConfig(Settings settings, Map<String, Menu> menus) {
        this.settings = settings;
        this.menus = menus;
    }

    public Settings settings() {
        return settings;
    }

    public String rootMenuId() {
        return ROOT_MENU_ID;
    }

    public Map<String, Menu> menus() {
        return menus;
    }

    public Menu menu(String id) {
        return menus.get(id);
    }

    public static MenusConfig load(Plugin plugin) throws IOException, InvalidConfigurationException {
        Logger log = plugin.getLogger();
        File base = new File(plugin.getDataFolder(), BASE);
        saveDefaults(plugin, base);

        Settings settings = readSettings(readYaml(new File(base, SETTINGS_FILE)));

        List<File> files = new ArrayList<>();
        collectMenuFiles(base, files);
        List<Menu> parsed = new ArrayList<>();
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - ".yml".length());
            Consumer<String> warn = message -> log.warning("FishDex menu '" + id + "': " + message);
            parsed.add(readMenu(id, readYaml(file), settings, isCollection(base, file), warn));
        }
        parsed.sort(Comparator.comparing(Menu::id));

        Map<String, Menu> menus = new LinkedHashMap<>();
        for (Menu menu : parsed) {
            if (menus.putIfAbsent(menu.id(), menu) != null) {
                log.warning("FishDex: duplicate menu id '" + menu.id() + "'; keeping the first one found.");
            }
        }
        validate(menus, log);
        return new MenusConfig(settings, Map.copyOf(menus));
    }

    private static YamlConfiguration readYaml(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        if (file.exists()) {
            try {
                yaml.load(file);
            } catch (InvalidConfigurationException e) {
                throw new InvalidConfigurationException(file.getName() + ": " + e.getMessage(), e);
            }
        }
        return yaml;
    }

    private static Settings readSettings(ConfigurationSection s) {
        return new Settings(
                s.getString("date-format", "dd/MM/yyyy"),
                SortMode.parse(s.getString("sort", "engine")),
                parseSortCycle(s.getStringList("sort-cycle")),
                readSortLabels(s.getConfigurationSection("sort-labels")),
                DiscoveryGrouping.parse(s.getString("grouping", "mixed")),
                readGroupLabels(s.getConfigurationSection("group-labels")),
                s.getString("grouping-hint", DEFAULT_GROUPING_HINT),
                ContentLayout.Mode.parse(s.getString("content-layout", "centered")));
    }

    private static Menu readMenu(String id, ConfigurationSection s, Settings settings, boolean collection,
                                 Consumer<String> warn) {
        warnMisplacedKeys(s, collection, warn);

        int rows = Math.clamp(s.getInt("rows", 6), 1, 6);
        int[] contentSlots = parseSlots(s.getString("content-slots", "18-35"), rows);
        ContentLayout.Mode contentLayout = s.isString("content-layout")
                ? ContentLayout.Mode.parse(s.getString("content-layout")) : settings.contentLayout();

        ConfigurationSection fillerSection = s.getConfigurationSection("filler");
        MenuIcon filler = fillerSection != null && fillerSection.getBoolean("enabled", true)
                ? MenuIcon.from(fillerSection, Material.LIGHT_GRAY_STAINED_GLASS_PANE, warn) : null;

        List<Button> buttons = readButtons(s.getConfigurationSection("buttons"), collection, warn);
        int indexPageCount = 1;
        for (Button button : buttons) {
            indexPageCount = Math.max(indexPageCount, button.page());
        }

        ConfigurationSection discovered = collection ? s.getConfigurationSection("discovered") : null;
        ConfigurationSection discoveredText = discovered == null ? null : discovered.getConfigurationSection("components");
        List<String> discoveredLore = discoveredText == null ? List.of() : discoveredText.getStringList(ItemComponents.LORE);
        String discoveredName = discoveredText == null ? null : discoveredText.getString(ItemComponents.CUSTOM_NAME);
        MenuIcon undiscovered = collection
                ? MenuIcon.from(s.getConfigurationSection("undiscovered"), Material.GRAY_DYE, warn) : null;

        return new Menu(id, collection, collection ? s.getString("category") : null,
                s.getString("title", "<white>" + id + "</white>"),
                rows, contentSlots, contentLayout, filler, readColors(s),
                readDecorations(s.getConfigurationSection("decorations"), warn),
                buttons, ClickSound.read(s, "open-sound"), indexPageCount,
                discoveredLore, discoveredName, undiscovered);
    }

    private static List<Button> readButtons(ConfigurationSection section, boolean collection, Consumer<String> warn) {
        List<Button> buttons = new ArrayList<>();
        if (section == null) {
            return buttons;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection b = section.getConfigurationSection(key);
            if (b == null) {
                continue;
            }
            Consumer<String> buttonWarn = message -> warn.accept("button '" + key + "': " + message);
            MenuAction action;
            try {
                action = MenuAction.parse(b.getString("action"));
            } catch (IllegalArgumentException invalid) {
                buttonWarn.accept(invalid.getMessage() + "; the button does nothing");
                action = new MenuAction.None();
            }
            if (action instanceof MenuAction.Sort && !collection) {
                buttonWarn.accept("'sort' only works in collections");
            }
            buttons.add(new Button(b.getInt("slot", -1), resolvePage(b, action), action,
                    MenuIcon.from(b, Material.PLAYER_HEAD, buttonWarn), ClickSound.read(b, "sound")));
        }
        return buttons;
    }

    /** {@code page} unset, {@code all}, {@code *} or below 1 falls back to the action's default. */
    private static int resolvePage(ConfigurationSection b, MenuAction action) {
        int fallback = action.onEveryPageByDefault() ? 0 : 1;
        if (!b.isSet("page")) {
            return fallback;
        }
        if (b.isInt("page")) {
            return Math.max(0, b.getInt("page"));
        }
        String raw = b.getString("page", "").trim();
        if (raw.equalsIgnoreCase("all") || raw.equals("*")) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static List<Decoration> readDecorations(ConfigurationSection section, Consumer<String> warn) {
        List<Decoration> decorations = new ArrayList<>();
        if (section == null) {
            return decorations;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection d = section.getConfigurationSection(key);
            if (d != null) {
                decorations.add(new Decoration(parseSlotList(d.getString("slots", "")),
                        MenuIcon.from(d, Material.GRAY_STAINED_GLASS_PANE,
                                message -> warn.accept("decoration '" + key + "': " + message))));
            }
        }
        return decorations;
    }

    /** One colour is solid, two or more form a gradient. */
    private static List<String> readColors(ConfigurationSection s) {
        if (s.isList("colors")) {
            return List.copyOf(s.getStringList("colors"));
        }
        if (s.isString("colors")) {
            return List.of(s.getString("colors"));
        }
        return List.of();
    }

    private static List<SortMode> parseSortCycle(List<String> raw) {
        Set<SortMode> ordered = new LinkedHashSet<>();
        for (String id : raw) {
            ordered.add(SortMode.parse(id));
        }
        return ordered.isEmpty() ? List.of(SortMode.values()) : List.copyOf(ordered);
    }

    private static Map<SortMode, String> readSortLabels(ConfigurationSection labels) {
        Map<SortMode, String> map = new LinkedHashMap<>();
        if (labels != null) {
            for (String key : labels.getKeys(false)) {
                map.put(SortMode.parse(key), labels.getString(key, key));
            }
        }
        return map;
    }

    private static Map<DiscoveryGrouping, String> readGroupLabels(ConfigurationSection labels) {
        Map<DiscoveryGrouping, String> map = new LinkedHashMap<>();
        if (labels != null) {
            for (String key : labels.getKeys(false)) {
                map.put(DiscoveryGrouping.parse(key), labels.getString(key, key));
            }
        }
        return map;
    }

    private static void warnMisplacedKeys(ConfigurationSection s, boolean collection, Consumer<String> warn) {
        for (String key : GLOBAL_ONLY_KEYS) {
            if (s.contains(key)) {
                warn.accept("'" + key + "' is a global setting; move it to settings.yml");
            }
        }
        Set<String> keys = collection ? COLLECTION_REQUIRED_KEYS : COLLECTION_ONLY_KEYS;
        for (String key : keys) {
            if (collection && !s.contains(key)) {
                warn.accept("missing required '" + key + "'");
            } else if (!collection && s.contains(key)) {
                warn.accept("'" + key + "' only applies to collections");
            }
        }
    }

    private static void validate(Map<String, Menu> menus, Logger log) {
        if (!menus.containsKey(ROOT_MENU_ID)) {
            log.warning("FishDex: the root menu '" + ROOT_MENU_ID + ".yml' is missing.");
        }
        for (Menu menu : menus.values()) {
            if (!menu.collection() && !menu.id().equals(ROOT_MENU_ID)) {
                log.warning("FishDex: '" + menu.id() + "' is an extra top-level menu; only '" + ROOT_MENU_ID
                        + "' is the root, put collections under " + COLLECTIONS_DIR + "/.");
            }
            int size = menu.rows() * 9;
            Map<Integer, Set<Integer>> slotsByPage = new HashMap<>();
            for (Button button : menu.buttons()) {
                if (button.action() instanceof MenuAction.Open open && !menus.containsKey(open.menuId())) {
                    log.warning("FishDex menu '" + menu.id() + "': a button opens unknown menu '" + open.menuId() + "'.");
                }
                if (button.slot() >= 0 && button.slot() < size
                        && !slotsByPage.computeIfAbsent(button.page(), p -> new HashSet<>()).add(button.slot())) {
                    log.warning("FishDex menu '" + menu.id() + "': two buttons on slot " + button.slot() + ".");
                }
            }
        }
    }

    private static void saveDefaults(Plugin plugin, File base) {
        if (base.exists()) {
            return;
        }
        for (String resource : DEFAULT_RESOURCES) {
            plugin.saveResource(resource, false);
        }
    }

    private static void collectMenuFiles(File dir, List<File> out) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                collectMenuFiles(entry, out);
            } else if (entry.getName().endsWith(".yml") && !entry.getName().startsWith("_")
                    && !entry.getName().equals(SETTINGS_FILE)) {
                out.add(entry);
            }
        }
    }

    private static boolean isCollection(File base, File file) {
        for (File dir = file.getParentFile(); dir != null && !dir.equals(base); dir = dir.getParentFile()) {
            if (dir.getName().equals(COLLECTIONS_DIR)) {
                return true;
            }
        }
        return false;
    }

    private static int[] parseSlots(String raw, int rows) {
        int[] parsed = parseSlotList(raw);
        if (parsed.length > 0) {
            return parsed;
        }
        int[] fallback = new int[Math.max(0, rows * 9 - 9)];
        for (int i = 0; i < fallback.length; i++) {
            fallback[i] = i;
        }
        return fallback;
    }

    /** {@code "0,8,10-16"}; malformed tokens are skipped. */
    private static int[] parseSlotList(String raw) {
        List<Integer> slots = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return new int[0];
        }
        for (String token : raw.split(",")) {
            String t = token.trim();
            int dash = t.indexOf('-');
            try {
                if (dash > 0) {
                    int from = Integer.parseInt(t.substring(0, dash).trim());
                    int to = Integer.parseInt(t.substring(dash + 1).trim());
                    for (int i = from; i <= to; i++) {
                        slots.add(i);
                    }
                } else if (!t.isEmpty()) {
                    slots.add(Integer.parseInt(t));
                }
            } catch (NumberFormatException ignored) {
                // malformed token
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}
