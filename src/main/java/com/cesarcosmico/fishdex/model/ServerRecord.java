package com.cesarcosmico.fishdex.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-wide data for a species: the record and its holder, the global catch count and the latest catch. */
public record ServerRecord(
        LootId loot,
        FishSize bestSize,
        UUID holder,
        Instant achievedAt,
        long totalGlobalCount,
        UUID lastCatcher,
        FishSize lastSize,
        Instant lastCaughtAt,
        String lastBiome) {

    public ServerRecord {
        Objects.requireNonNull(loot, "loot");
        Objects.requireNonNull(bestSize, "bestSize");
        Objects.requireNonNull(holder, "holder");
        Objects.requireNonNull(achievedAt, "achievedAt");
    }

    public Optional<UUID> lastCatcherOptional() {
        return Optional.ofNullable(lastCatcher);
    }

    public Optional<FishSize> lastSizeOptional() {
        return Optional.ofNullable(lastSize);
    }
}
