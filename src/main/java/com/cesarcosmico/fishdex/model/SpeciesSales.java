package com.cesarcosmico.fishdex.model;

import java.util.Objects;

/** How many units of a species a player has sold on CustomFishing's market. */
public record SpeciesSales(LootId loot, long soldCount) {

    public SpeciesSales {
        Objects.requireNonNull(loot, "loot");
        if (soldCount < 0) {
            throw new IllegalArgumentException("soldCount must be >= 0: " + soldCount);
        }
    }
}
