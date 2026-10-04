package com.cesarcosmico.fishdex.menu;

import com.cesarcosmico.fishdex.model.CategoryProgress;
import com.cesarcosmico.fishdex.model.DiscoveryGrouping;
import com.cesarcosmico.fishdex.model.FishDexEntry;
import com.cesarcosmico.fishdex.model.ServerRecord;
import com.cesarcosmico.fishdex.model.SortMode;
import com.cesarcosmico.fishdex.service.FishDexOrdering;
import com.cesarcosmico.fishdex.service.FishingEngine;
import com.cesarcosmico.fishdex.text.NameFormatting;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Draws a menu into its inventory: filler, frame, fish entries and buttons, with their placeholders. */
public final class MenuRenderer {

    private static final int BAR_SEGMENTS = 20;

    private final IconRenderer icons;
    private final FishingEngine engine;
    private final FishDexOrdering ordering;

    public MenuRenderer(IconRenderer icons, FishingEngine engine, FishDexOrdering ordering) {
        this.icons = icons;
        this.engine = engine;
        this.ordering = ordering;
    }

    public Component title(MenusConfig.Menu menu, MenuHolder holder, Player viewer) {
        return icons.render(menu.title(), viewer, pageResolvers(holder), IconRenderer.Accent.of(menu.colors()));
    }

    public void render(MenusConfig config, MenusConfig.Menu menu, MenuHolder holder, Player viewer) {
        IconRenderer.Accent accent = IconRenderer.Accent.of(menu.colors());
        Inventory inventory = holder.getInventory();
        inventory.clear();

        if (menu.filler() != null) {
            ItemStack filler = icons.create(menu.filler(), viewer, TagResolver.empty(), accent);
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                inventory.setItem(slot, filler);
            }
        }
        TagResolver page = pageResolvers(holder);
        CategoryProgress progress = menu.collection()
                ? CategoryProgress.of(holder.entries())
                : holder.summary().overall();
        TagResolver frameResolver = TagResolver.resolver(progressResolvers(progress, viewer, accent), page);
        for (MenusConfig.Decoration decoration : menu.decorations()) {
            place(inventory, decoration.slots(), icons.create(decoration.icon(), viewer, frameResolver, accent));
        }
        if (menu.collection()) {
            renderEntries(config, menu, holder, viewer, accent);
        }
        for (MenusConfig.Button button : menu.buttons()) {
            if (button.slot() >= 0 && button.slot() < inventory.getSize() && shows(button, holder)) {
                TagResolver resolver = TagResolver.resolver(buttonResolvers(config, menu, button, holder, viewer), page);
                inventory.setItem(button.slot(), icons.create(button.icon(), viewer, resolver, accent));
            }
        }
    }

    /** Whether {@code button} is drawn (and clickable) on the holder's current page. */
    public static boolean shows(MenusConfig.Button button, MenuHolder holder) {
        boolean onPage = button.page() == 0 || button.page() - 1 == holder.page();
        return onPage && switch (button.action()) {
            case MenuAction.PreviousPage _ -> holder.page() > 0;
            case MenuAction.NextPage _ -> holder.page() < holder.pageCount() - 1;
            default -> true;
        };
    }

    private void renderEntries(MenusConfig config, MenusConfig.Menu menu, MenuHolder holder, Player viewer,
                               IconRenderer.Accent accent) {
        DateTimeFormatter dates = formatter(config.settings().dateFormat());
        int[] slots = menu.contentSlots();
        int start = holder.page() * slots.length;
        List<FishDexEntry> entries = ordering.sorted(holder.entries(), holder.sort(), holder.grouping());
        int onPage = Math.max(0, Math.min(slots.length, entries.size() - start));
        // Centering only fits a single page; once it paginates, full rows keep the pages aligned.
        ContentLayout.Mode mode = holder.pageCount() > 1 ? ContentLayout.Mode.FILL : menu.contentLayout();
        int[] targets = ContentLayout.place(mode, slots, onPage);
        Inventory inventory = holder.getInventory();
        for (int slot : slots) {
            inventory.setItem(slot, null);
        }
        for (int i = 0; i < onPage; i++) {
            inventory.setItem(targets[i], entryIcon(entries.get(start + i), menu, viewer, dates, accent));
        }
    }

    private ItemStack entryIcon(FishDexEntry entry, MenusConfig.Menu menu, Player viewer,
                                DateTimeFormatter dates, IconRenderer.Accent accent) {
        TagResolver resolver = entryResolvers(entry, dates);
        if (!entry.discovered()) {
            return icons.create(menu.undiscovered(), viewer, resolver, accent);
        }
        ItemStack icon = engine.icon(entry.loot(), viewer).orElseGet(() -> fallbackIcon(entry, viewer));
        if (menu.discoveredName() != null || !menu.discoveredLore().isEmpty()) {
            icons.relabel(icon, menu.discoveredName(), menu.discoveredLore(), viewer, resolver, accent);
        }
        return icon;
    }

    private ItemStack fallbackIcon(FishDexEntry entry, Player viewer) {
        ItemStack item = ItemStack.of(Material.COD);
        item.setData(DataComponentTypes.CUSTOM_NAME,
                icons.render(entry.displayName(), viewer, TagResolver.empty(), IconRenderer.Accent.NONE));
        return item;
    }

    private TagResolver buttonResolvers(MenusConfig config, MenusConfig.Menu menu, MenusConfig.Button button,
                                        MenuHolder holder, Player viewer) {
        return switch (button.action()) {
            case MenuAction.Sort _ when menu.collection() -> TagResolver.resolver(
                    sortResolvers(config, holder.sort()), groupResolvers(config, holder.sort(), holder.grouping()));
            case MenuAction.Open open when holder.summary() != null -> progressResolvers(
                    holder.summary().category(open.menuId()), viewer, IconRenderer.Accent.of(menu.colors()));
            default -> TagResolver.empty();
        };
    }

    private static TagResolver entryResolvers(FishDexEntry entry, DateTimeFormatter dates) {
        ServerRecord record = entry.serverRecord();
        return TagResolver.resolver(
                Placeholder.parsed("name", NameFormatting.toMiniMessage(entry.displayName())),
                Placeholder.unparsed("name-plain", NameFormatting.toPlain(entry.displayName())),
                Placeholder.unparsed("count", Long.toString(entry.count())),
                Placeholder.unparsed("sold", Long.toString(entry.soldCount())),
                Placeholder.unparsed("min", entry.minSizeOptional().map(s -> s.formatted()).orElse("?")),
                Placeholder.unparsed("max", entry.maxSizeOptional().map(s -> s.formatted()).orElse("?")),
                Placeholder.unparsed("record", record == null ? "-" : record.bestSize().formatted()),
                Placeholder.unparsed("record-holder", record == null ? "-" : playerName(record.holder())),
                Placeholder.unparsed("record-date", record == null ? "-" : dates.format(record.achievedAt())),
                Placeholder.unparsed("total-global", record == null ? "0" : Long.toString(record.totalGlobalCount())),
                Placeholder.unparsed("first-catch", entry.firstCatchAt() == null ? "-" : dates.format(entry.firstCatchAt())),
                Placeholder.unparsed("last-catch", entry.lastCatchAt() == null ? "-" : dates.format(entry.lastCatchAt())),
                Placeholder.unparsed("discoverer",
                        entry.discoveryOptional().map(d -> playerName(d.firstPlayer())).orElse("-")),
                Placeholder.unparsed("discovered-date",
                        entry.discoveryOptional().map(d -> dates.format(d.discoveredAt())).orElse("-")),
                Placeholder.unparsed("last-catcher", record == null
                        ? "-" : record.lastCatcherOptional().map(MenuRenderer::playerName).orElse("-")),
                Placeholder.unparsed("last-size", record == null
                        ? "-" : record.lastSizeOptional().map(s -> s.formatted()).orElse("-")),
                Placeholder.unparsed("last-catch-date", record == null || record.lastCaughtAt() == null
                        ? "-" : dates.format(record.lastCaughtAt())),
                Placeholder.component("last-biome", biome(record)));
    }

    /** Translatable, so each client shows the biome in its own language. */
    private static Component biome(ServerRecord record) {
        String key = record == null ? null : record.lastBiome();
        if (key == null || key.isBlank()) {
            return Component.text("-");
        }
        String namespaced = key.contains(":") ? key : "minecraft:" + key;
        return Component.translatable("biome." + namespaced.replace(':', '.'));
    }

    private static TagResolver sortResolvers(MenusConfig config, SortMode current) {
        List<SortMode> cycle = config.settings().sortCycle();
        return TagResolver.resolver(
                Placeholder.parsed("sort-mode", sortLabel(config, current)),
                Placeholder.parsed("next-sort-mode", sortLabel(config, MenuNavigator.step(cycle, current, 1))),
                Placeholder.parsed("prev-sort-mode", sortLabel(config, MenuNavigator.step(cycle, current, -1))));
    }

    private TagResolver groupResolvers(MenusConfig config, SortMode sort, DiscoveryGrouping chosen) {
        DiscoveryGrouping effective = ordering.effectiveGrouping(sort, chosen);
        return TagResolver.resolver(
                Placeholder.parsed("group-mode", config.settings().groupLabels().getOrDefault(effective, effective.id())),
                Placeholder.parsed("grouping-hint", ordering.groupable(sort) ? config.settings().groupingHint() : ""));
    }

    private static String sortLabel(MenusConfig config, SortMode mode) {
        return config.settings().sortLabels().getOrDefault(mode, mode.id());
    }

    private static TagResolver progressResolvers(CategoryProgress progress, Player viewer, IconRenderer.Accent accent) {
        int width = Math.max(2, Integer.toString(progress.total()).length());
        return TagResolver.resolver(
                Placeholder.unparsed("discovered", Integer.toString(progress.discovered())),
                Placeholder.unparsed("total", Integer.toString(progress.total())),
                Placeholder.unparsed("discovered-padded", pad(progress.discovered(), width)),
                Placeholder.unparsed("total-padded", pad(progress.total(), width)),
                Placeholder.unparsed("progress", percent(progress.percent())),
                Placeholder.parsed("progress-bar", progressBar(progress.percent(), accent)),
                Placeholder.unparsed("player-caught", Long.toString(progress.playerCaught())),
                Placeholder.unparsed("global-caught", Long.toString(progress.globalCaught())),
                Placeholder.unparsed("player", viewer.getName()));
    }

    /** The accent spans the whole bar so the gradient does not shift with progress; the rest is dark gray. */
    private static String progressBar(double percent, IconRenderer.Accent accent) {
        int filled = (int) Math.round(Math.clamp(percent, 0, 100) / 100.0 * BAR_SEGMENTS);
        String open = accent.open().isEmpty() ? "<white>" : accent.open();
        String close = accent.close().isEmpty() ? "</white>" : accent.close();
        return open + "<st>" + " ".repeat(filled)
                + "<dark_gray>" + " ".repeat(BAR_SEGMENTS - filled) + "</dark_gray>"
                + "</st>" + close;
    }

    private static TagResolver pageResolvers(MenuHolder holder) {
        int total = holder.pageCount();
        int width = Math.max(2, Integer.toString(total).length());
        return TagResolver.resolver(
                Placeholder.unparsed("page", pad(holder.page() + 1, width)),
                Placeholder.unparsed("page-count", pad(total, width)));
    }

    private static String pad(int value, int width) {
        return String.format(Locale.ROOT, "%0" + width + "d", value);
    }

    private static String percent(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String playerName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "?" : name;
    }

    private static void place(Inventory inventory, int[] slots, ItemStack item) {
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            }
        }
    }

    private static DateTimeFormatter formatter(String pattern) {
        try {
            return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
        } catch (IllegalArgumentException | DateTimeException e) {
            return DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
        }
    }
}
