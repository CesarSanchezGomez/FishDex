package com.cesarcosmico.fishdex.integration.customfishing;

import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.service.FishingEngine;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.context.Context;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The only class that reads CustomFishing's API. Categories and names are memoised because every menu open
 * resolves one name per loot; {@link #invalidateCaches()} runs on {@code /fishdex reload} and on
 * CustomFishing's own reload.
 */
public final class CustomFishingEngine implements FishingEngine {

    private static final String PLUGIN_NAME = "CustomFishing";

    private final Logger logger;
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();
    private final Map<String, List<LootId>> categoryCache = new ConcurrentHashMap<>();
    private final Map<LootId, Optional<String>> nameCache = new ConcurrentHashMap<>();
    private volatile List<LootId> allLootIdsCache;

    public CustomFishingEngine(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            return BukkitCustomFishingPlugin.getInstance() != null;
        } catch (RuntimeException notInitialised) {
            return false;
        }
    }

    @Override
    public String engineVersion() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        return plugin == null ? "unknown" : plugin.getPluginMeta().getVersion();
    }

    @Override
    public List<LootId> allLootIds() {
        List<LootId> cached = allLootIdsCache;
        if (cached != null) {
            return cached;
        }
        // CustomFishing's master lists: contents/category/fish.yml and item.yml.
        Set<String> ids = new LinkedHashSet<>(category("fishes"));
        ids.addAll(category("items"));
        List<LootId> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            result.add(LootId.of(id));
        }
        allLootIdsCache = List.copyOf(result);
        return allLootIdsCache;
    }

    @Override
    public List<LootId> categoryMembers(String category) {
        return categoryCache.computeIfAbsent(category, key -> {
            List<LootId> result = new ArrayList<>();
            for (String id : category(key)) {
                result.add(LootId.of(id));
            }
            return List.copyOf(result);
        });
    }

    @Override
    public Optional<String> displayName(LootId loot) {
        return nameCache.computeIfAbsent(loot, key -> {
            try {
                return BukkitCustomFishingPlugin.getInstance().getLootManager().getLoot(key.value())
                        .map(entry -> entry.nick());
            } catch (RuntimeException e) {
                reportOnce("display names", e);
                return Optional.empty();
            }
        });
    }

    @Override
    public Optional<ItemStack> icon(LootId loot, Player viewer) {
        try {
            ItemStack item = BukkitCustomFishingPlugin.getInstance().getItemManager()
                    .buildInternal(Context.player(viewer), loot.value());
            return item == null || item.getType().isAir() ? Optional.empty() : Optional.of(item);
        } catch (RuntimeException e) {
            reportOnce("loot icons", e);
            return Optional.empty();
        }
    }

    public void invalidateCaches() {
        categoryCache.clear();
        nameCache.clear();
        allLootIdsCache = null;
        reportedFailures.clear();
    }

    private List<String> category(String category) {
        try {
            List<String> members = BukkitCustomFishingPlugin.getInstance()
                    .getStatisticsManager().getCategoryMembers(category);
            return members == null ? List.of() : members;
        } catch (RuntimeException e) {
            reportOnce("categories", e);
            return List.of();
        }
    }

    /** A broken CustomFishing API would otherwise fail on every menu open; one warning per kind is enough. */
    private void reportOnce(String what, RuntimeException error) {
        if (reportedFailures.add(what)) {
            logger.log(Level.WARNING, "CustomFishing failed while reading " + what
                    + "; the FishDex shows fallbacks until the next reload.", error);
        }
    }
}
