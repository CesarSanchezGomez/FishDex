package com.cesarcosmico.fishdex.model;

import java.util.Locale;

/** Whether discovered or undiscovered entries come first, independently of the sort criterion. */
public enum DiscoveryGrouping {
    DISCOVERED_FIRST("discovered-first"),
    UNDISCOVERED_FIRST("undiscovered-first"),
    MIXED("mixed");

    private final String id;

    DiscoveryGrouping(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Parses an id ({@code -} or {@code _} separated), falling back to {@link #DISCOVERED_FIRST}. */
    public static DiscoveryGrouping parse(String raw) {
        if (raw == null) {
            return DISCOVERED_FIRST;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (DiscoveryGrouping grouping : values()) {
            if (grouping.id.equals(key)) {
                return grouping;
            }
        }
        return DISCOVERED_FIRST;
    }
}
