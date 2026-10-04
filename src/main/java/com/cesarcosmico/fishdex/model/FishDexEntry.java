package com.cesarcosmico.fishdex.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** One species as one player sees it; null fields mean "no data yet". */
public record FishDexEntry(
        LootId loot,
        String displayName,
        boolean discovered,
        long count,
        long soldCount,
        FishSize minSize,
        FishSize maxSize,
        Instant firstCatchAt,
        Instant lastCatchAt,
        ServerRecord serverRecord,
        Discovery discovery) {

    public FishDexEntry {
        Objects.requireNonNull(loot, "loot");
        Objects.requireNonNull(displayName, "displayName");
    }

    public Optional<FishSize> minSizeOptional() {
        return Optional.ofNullable(minSize);
    }

    public Optional<FishSize> maxSizeOptional() {
        return Optional.ofNullable(maxSize);
    }

    public Optional<ServerRecord> serverRecordOptional() {
        return Optional.ofNullable(serverRecord);
    }

    public Optional<Discovery> discoveryOptional() {
        return Optional.ofNullable(discovery);
    }
}
