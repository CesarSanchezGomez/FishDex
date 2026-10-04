package com.cesarcosmico.fishdex.integration.customfishing;

import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.service.SaleService;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.event.MarketSellEvent;
import net.momirealms.customfishing.api.mechanic.item.ItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** The sell event only carries item stacks, so each sold item is mapped back to its CustomFishing loot id. */
public final class MarketSellListener implements Listener {

    private final SaleService saleService;

    public MarketSellListener(SaleService saleService) {
        this.saleService = saleService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSell(MarketSellEvent event) {
        ItemManager itemManager = BukkitCustomFishingPlugin.getInstance().getItemManager();
        Map<LootId, Integer> soldByLoot = new LinkedHashMap<>();
        for (ItemStack item : event.getItems()) {
            if (item == null) {
                continue;
            }
            String id = itemManager.getCustomFishingItemID(item);
            if (id == null || id.isBlank()) {
                continue;
            }
            soldByLoot.merge(LootId.of(id), item.getAmount(), Integer::sum);
        }
        if (!soldByLoot.isEmpty()) {
            saleService.record(event.getPlayer().getUniqueId(), soldByLoot);
        }
    }
}
