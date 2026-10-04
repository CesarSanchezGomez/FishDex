package com.cesarcosmico.fishdex.model;

/** Stored as {@code sort;grouping}; a value without {@code ;} reads back with the {@code MIXED} grouping. */
public record SortPreference(SortMode sort, DiscoveryGrouping grouping) {

    public String serialize() {
        return sort.id() + ";" + grouping.id();
    }

    public static SortPreference parse(String raw) {
        String[] parts = raw.split(";", 2);
        SortMode sort = SortMode.parse(parts[0]);
        DiscoveryGrouping grouping = parts.length > 1 ? DiscoveryGrouping.parse(parts[1]) : DiscoveryGrouping.MIXED;
        return new SortPreference(sort, grouping);
    }
}
