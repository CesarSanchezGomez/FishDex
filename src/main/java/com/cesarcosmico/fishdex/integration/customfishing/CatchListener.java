package com.cesarcosmico.fishdex.integration.customfishing;

import com.cesarcosmico.fishdex.model.CatchRecord;
import com.cesarcosmico.fishdex.model.FishSize;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.service.CatchService;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.event.FishingLootSpawnEvent;
import net.momirealms.customfishing.api.mechanic.context.Context;
import net.momirealms.customfishing.api.mechanic.context.ContextKeys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Instant;

/**
 * Listens to loot spawn rather than {@code FishingResultEvent}: CustomFishing only writes
 * {@code ContextKeys.SIZE} while building the loot item, in a task that runs after the result event, so the
 * size is missing there. The event fires once per spawned item, so each one is a single catch.
 */
public final class CatchListener implements Listener {

    private final CatchService catches;

    public CatchListener(CatchService catches) {
        this.catches = catches;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLootSpawn(FishingLootSpawnEvent event) {
        Context<Player> context = event.getContext();
        if (context == null) {
            return;
        }
        String id = context.arg(ContextKeys.ID);
        if (id == null || id.isBlank()) {
            return;
        }
        Player player = event.getPlayer();
        catches.handle(new CatchRecord(player.getUniqueId(), LootId.of(id), size(context, event.getEntity()),
                1, biomeKey(player), Instant.now()));
    }

    /** The context size, or the spawned item's stored size when the context has none. */
    private static FishSize size(Context<Player> context, Entity entity) {
        Float size = context.arg(ContextKeys.SIZE);
        if (size == null && entity instanceof Item item) {
            size = BukkitCustomFishingPlugin.getInstance().getItemManager().getFishSize(item.getItemStack());
        }
        return size == null ? null : FishSize.ofCentimetres(size);
    }

    /** Namespaced (e.g. {@code minecraft:plains}) so the menu can build the client translation key. */
    private static String biomeKey(Player player) {
        try {
            return player.getLocation().getBlock().getBiome().getKey().toString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
