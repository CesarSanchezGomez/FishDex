package com.cesarcosmico.fishdex.model;

import java.util.Locale;

/** {@code ENGINE} keeps CustomFishing's order; {@link #id()} is the token used in config and stored preferences. */
public enum SortMode {
    ENGINE("engine"),
    NAME("name"),
    NAME_DESC("name-desc"),
    COUNT("count"),
    COUNT_ASC("count-asc"),
    SOLD("sold"),
    SOLD_ASC("sold-asc"),
    MAX_SIZE("max-size"),
    MIN_SIZE("min-size"),
    BIOME("biome");

    private final String id;

    SortMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Parses an id ({@code -} or {@code _} separated), falling back to {@link #ENGINE}. */
    public static SortMode parse(String raw) {
        if (raw == null) {
            return ENGINE;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (SortMode mode : values()) {
            if (mode.id.equals(key)) {
                return mode;
            }
        }
        return ENGINE;
    }
}
