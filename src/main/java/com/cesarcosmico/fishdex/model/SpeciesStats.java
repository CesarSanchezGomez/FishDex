package com.cesarcosmico.fishdex.model;

import java.time.Instant;
import java.util.Objects;

public record SpeciesStats(
        LootId loot,
        long count,
        FishSize minSize,
        FishSize maxSize,
        Instant firstCatchAt,
        Instant lastCatchAt) {

    public SpeciesStats {
        Objects.requireNonNull(loot, "loot");
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0: " + count);
        }
    }
}
