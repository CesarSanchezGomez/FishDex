package com.cesarcosmico.fishdex.menu;

import com.cesarcosmico.fishdex.model.DiscoveryGrouping;
import com.cesarcosmico.fishdex.model.SortPreference;
import com.cesarcosmico.fishdex.service.FishDexOrdering;
import com.cesarcosmico.fishdex.service.FishDexService;
import com.cesarcosmico.fishdex.service.PreferenceService;
import com.cesarcosmico.fishdex.text.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Opens menus, turns pages, re-sorts and runs button actions. */
public final class MenuNavigator {

    private static final List<DiscoveryGrouping> GROUPING_CYCLE = List.of(DiscoveryGrouping.values());

    private final Plugin plugin;
    private final Supplier<MenusConfig> menus;
    private final Supplier<Messages> messages;
    private final FishDexService fishDex;
    private final PreferenceService preferences;
    private final FishDexOrdering ordering;
    private final MenuRenderer renderer;

    public MenuNavigator(Plugin plugin, Supplier<MenusConfig> menus, Supplier<Messages> messages,
                         FishDexService fishDex, PreferenceService preferences, FishDexOrdering ordering,
                         MenuRenderer renderer) {
        this.plugin = plugin;
        this.menus = menus;
        this.messages = messages;
        this.fishDex = fishDex;
        this.preferences = preferences;
        this.ordering = ordering;
        this.renderer = renderer;
    }

    public String rootMenuId() {
        return menus.get().rootMenuId();
    }

    public Set<String> menuIds() {
        return menus.get().menus().keySet();
    }

    public void open(Player player, String menuId) {
        MenusConfig config = menus.get();
        MenusConfig.Menu menu = config.menu(menuId);
        if (menu == null) {
            player.sendMessage(messages.get().get("fishdex.unknown-category"));
            return;
        }
        if (menu.collection()) {
            fishDex.entriesFor(player.getUniqueId(), menu.category()).whenComplete((entries, error) ->
                    player.getScheduler().run(plugin, task -> {
                        if (!failed(player, error)) {
                            SortPreference preference = preferences.get(player.getUniqueId()).orElse(
                                    new SortPreference(config.settings().sort(), config.settings().grouping()));
                            show(player, config, menu, MenuHolder.collection(menu.id(), entries,
                                    menu.contentSlots().length, preference.sort(), preference.grouping()));
                        }
                    }, null));
        } else {
            fishDex.summary(player.getUniqueId(), collectionCategories(config)).whenComplete((summary, error) ->
                    player.getScheduler().run(plugin, task -> {
                        if (!failed(player, error)) {
                            show(player, config, menu, MenuHolder.index(menu.id(), summary, menu.indexPageCount()));
                        }
                    }, null));
        }
    }

    public void click(Player player, MenuHolder holder, int rawSlot, ClickType click) {
        MenusConfig config = menus.get();
        MenusConfig.Menu menu = config.menu(holder.menuId());
        if (menu == null) {
            return;
        }
        for (MenusConfig.Button button : menu.buttons()) {
            if (button.slot() == rawSlot && MenuRenderer.shows(button, holder)) {
                if (button.sound() != null) {
                    player.playSound(button.sound().toAdventure());
                }
                run(player, config, menu, holder, button.action(), click);
                return;
            }
        }
    }

    private void run(Player player, MenusConfig config, MenusConfig.Menu menu, MenuHolder holder,
                     MenuAction action, ClickType click) {
        switch (action) {
            case MenuAction.OpenRoot _ -> open(player, config.rootMenuId());
            case MenuAction.Open open -> open(player, open.menuId());
            case MenuAction.Close _ -> player.closeInventory();
            case MenuAction.PreviousPage _ -> {
                if (holder.setPage(holder.page() - 1)) {
                    refresh(player, config, menu, holder);
                }
            }
            case MenuAction.NextPage _ -> {
                if (holder.setPage(holder.page() + 1)) {
                    refresh(player, config, menu, holder);
                }
            }
            case MenuAction.Sort _ when menu.collection() -> {
                int direction = click.isRightClick() ? -1 : 1;
                if (!click.isShiftClick()) {
                    holder.setSort(step(config.settings().sortCycle(), holder.sort(), direction));
                } else if (ordering.groupable(holder.sort())) {
                    holder.setGrouping(step(GROUPING_CYCLE, holder.grouping(), direction));
                } else {
                    return;
                }
                holder.setPage(0);
                preferences.set(player.getUniqueId(), new SortPreference(holder.sort(), holder.grouping()));
                refresh(player, config, menu, holder);
            }
            case MenuAction.Sort _, MenuAction.None _ -> {
            }
        }
    }

    private void show(Player player, MenusConfig config, MenusConfig.Menu menu, MenuHolder holder) {
        Inventory inventory = Bukkit.createInventory(holder, menu.rows() * 9, renderer.title(menu, holder, player));
        holder.attach(inventory);
        renderer.render(config, menu, holder, player);
        player.openInventory(inventory);
        if (menu.openSound() != null) {
            player.playSound(menu.openSound().toAdventure());
        }
    }

    /**
     * Paper cannot retitle an open inventory, so when the title changes (e.g. it shows {@code <page>}) the
     * menu is reopened on the next tick; otherwise it is redrawn in place.
     */
    private void refresh(Player player, MenusConfig config, MenusConfig.Menu menu, MenuHolder holder) {
        Component title = renderer.title(menu, holder, player);
        if (title.equals(player.getOpenInventory().title())) {
            renderer.render(config, menu, holder, player);
            return;
        }
        Inventory inventory = Bukkit.createInventory(holder, menu.rows() * 9, title);
        holder.attach(inventory);
        renderer.render(config, menu, holder, player);
        player.getScheduler().run(plugin, task -> player.openInventory(inventory), null);
    }

    private boolean failed(Player player, Throwable error) {
        if (error == null) {
            return false;
        }
        player.sendMessage(messages.get().get("fishdex.error"));
        plugin.getLogger().log(Level.WARNING, "Failed to load the FishDex for " + player.getName(), error);
        return true;
    }

    private static Map<String, String> collectionCategories(MenusConfig config) {
        Map<String, String> categories = new LinkedHashMap<>();
        config.menus().forEach((id, menu) -> {
            if (menu.collection() && menu.category() != null && !menu.category().isBlank()) {
                categories.put(id, menu.category());
            }
        });
        return categories;
    }

    /** The element {@code delta} steps away from {@code current}, wrapping around the cycle. */
    static <E> E step(List<E> cycle, E current, int delta) {
        if (cycle.isEmpty()) {
            return current;
        }
        int index = cycle.indexOf(current);
        if (index < 0) {
            return delta < 0 ? cycle.getLast() : cycle.getFirst();
        }
        return cycle.get(Math.floorMod(index + delta, cycle.size()));
    }
}
