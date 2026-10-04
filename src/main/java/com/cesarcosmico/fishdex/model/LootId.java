package com.cesarcosmico.fishdex.model;

import java.util.Objects;

/** A CustomFishing loot id (as in {@code contents/item/*.yml}); the join key of every table. */
public record LootId(String value) {

    public LootId {
        Objects.requireNonNull(value, "loot id value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("loot id must not be blank");
        }
    }

    public static LootId of(String value) {
        return new LootId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
