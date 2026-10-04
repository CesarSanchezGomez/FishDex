package com.cesarcosmico.fishdex.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** The first player to ever catch a species; CustomFishing does not track it. */
public record Discovery(LootId loot, UUID firstPlayer, Instant discoveredAt) {

    public Discovery {
        Objects.requireNonNull(loot, "loot");
        Objects.requireNonNull(firstPlayer, "firstPlayer");
        Objects.requireNonNull(discoveredAt, "discoveredAt");
    }
}
