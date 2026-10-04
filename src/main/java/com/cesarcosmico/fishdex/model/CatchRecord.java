package com.cesarcosmico.fishdex.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One catch. {@code size} is null for loot without a size (trash, blocks): it is counted but never ranked. */
public record CatchRecord(
        UUID player,
        LootId loot,
        FishSize size,
        int amount,
        String biome,
        Instant caughtAt) {

    public CatchRecord {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(loot, "loot");
        Objects.requireNonNull(caughtAt, "caughtAt");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be >= 1: " + amount);
        }
    }

    public boolean hasSize() {
        return size != null;
    }
}
