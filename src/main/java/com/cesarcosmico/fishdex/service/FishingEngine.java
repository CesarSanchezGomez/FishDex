package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.model.LootId;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Everything FishDex reads from CustomFishing. An interface only because CustomFishing's API is external
 * and volatile, and the services are tested against a fake of it.
 */
public interface FishingEngine {

    boolean isAvailable();

    String engineVersion();

    /** Fish and trash, in CustomFishing's category order. */
    List<LootId> allLootIds();

    List<LootId> categoryMembers(String category);

    Optional<String> displayName(LootId loot);

    /** The loot item exactly as CustomFishing builds it for {@code viewer}; main thread only. */
    Optional<ItemStack> icon(LootId loot, Player viewer);
}
