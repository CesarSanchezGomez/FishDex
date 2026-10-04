package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.model.DiscoveryGrouping;
import com.cesarcosmico.fishdex.model.FishDexEntry;
import com.cesarcosmico.fishdex.model.ServerRecord;
import com.cesarcosmico.fishdex.model.SortMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Orders entries by discovery grouping first, then by the sort criterion. Grouping is only a player choice
 * on the name sorts: data sorts always push undiscovered entries last and {@code ENGINE} keeps
 * CustomFishing's order.
 */
public final class FishDexOrdering {

    private final Function<String, String> toPlainName;

    public FishDexOrdering(Function<String, String> toPlainName) {
        this.toPlainName = toPlainName;
    }

    /** A copy of {@code entries} ordered by grouping then criterion; stable, so ties keep the input order. */
    public List<FishDexEntry> sorted(List<FishDexEntry> entries, SortMode sort, DiscoveryGrouping grouping) {
        List<FishDexEntry> copy = new ArrayList<>(entries);
        Comparator<FishDexEntry> criterion = criterionComparator(sort);
        Comparator<FishDexEntry> ordered = switch (effectiveGrouping(sort, grouping)) {
            case DISCOVERED_FIRST -> compose(Comparator.comparing(FishDexEntry::discovered).reversed(), criterion);
            case UNDISCOVERED_FIRST -> compose(Comparator.comparing(FishDexEntry::discovered), criterion);
            case MIXED -> criterion;
        };
        if (ordered != null) {
            copy.sort(ordered);
        }
        return copy;
    }

    public boolean groupable(SortMode sort) {
        return sort == SortMode.NAME || sort == SortMode.NAME_DESC;
    }

    public DiscoveryGrouping effectiveGrouping(SortMode sort, DiscoveryGrouping chosen) {
        if (groupable(sort)) {
            return chosen;
        }
        return sort == SortMode.ENGINE ? DiscoveryGrouping.MIXED : DiscoveryGrouping.DISCOVERED_FIRST;
    }

    private static Comparator<FishDexEntry> compose(Comparator<FishDexEntry> primary, Comparator<FishDexEntry> secondary) {
        return secondary == null ? primary : primary.thenComparing(secondary);
    }

    /** The within-group order for a criterion; {@code null} for {@code ENGINE} (keep the native order). */
    private Comparator<FishDexEntry> criterionComparator(SortMode sort) {
        return switch (sort) {
            case NAME -> Comparator.comparing(this::sortName, String.CASE_INSENSITIVE_ORDER);
            case NAME_DESC -> Comparator.comparing(this::sortName, String.CASE_INSENSITIVE_ORDER).reversed();
            case COUNT -> Comparator.comparingLong(FishDexEntry::count).reversed();
            case COUNT_ASC -> Comparator.comparingLong(FishDexEntry::count);
            case SOLD -> Comparator.comparingLong(FishDexEntry::soldCount).reversed();
            case SOLD_ASC -> Comparator.comparingLong(FishDexEntry::soldCount);
            case MAX_SIZE -> Comparator.comparingDouble(FishDexOrdering::maxCm).reversed();
            case MIN_SIZE -> Comparator.comparingDouble(FishDexOrdering::minCm);
            case BIOME -> Comparator.comparing(FishDexOrdering::biomeName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case ENGINE -> null;
        };
    }

    /** Name key for sorting: plain text with leading icons/symbols/spaces skipped, so it starts at the first letter. */
    private String sortName(FishDexEntry e) {
        String plain = toPlainName.apply(e.displayName());
        int i = 0;
        while (i < plain.length() && !Character.isLetterOrDigit(plain.codePointAt(i))) {
            i += Character.charCount(plain.codePointAt(i));
        }
        return i < plain.length() ? plain.substring(i) : plain;
    }

    private static double maxCm(FishDexEntry e) {
        return e.maxSizeOptional().map(s -> s.centimetres()).orElse(-1.0);
    }

    private static double minCm(FishDexEntry e) {
        return e.minSizeOptional().map(s -> s.centimetres()).orElse(Double.MAX_VALUE);
    }

    private static String biomeName(FishDexEntry e) {
        return e.serverRecordOptional().map(ServerRecord::lastBiome).filter(b -> !b.isBlank()).orElse(null);
    }
}
