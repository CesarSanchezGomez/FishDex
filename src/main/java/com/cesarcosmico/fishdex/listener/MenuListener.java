package com.cesarcosmico.fishdex.listener;

import com.cesarcosmico.fishdex.menu.MenuHolder;
import com.cesarcosmico.fishdex.menu.MenuNavigator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** FishDex menus are read-only: every click and drag is cancelled, and clicks on the menu are routed. */
public final class MenuListener implements Listener {

    private final MenuNavigator navigator;

    public MenuListener(MenuNavigator navigator) {
        this.navigator = navigator;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < top.getSize() && event.getWhoClicked() instanceof Player player) {
            navigator.click(player, holder, rawSlot, event.getClick());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
