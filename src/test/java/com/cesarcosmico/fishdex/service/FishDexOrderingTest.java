package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.model.DiscoveryGrouping;
import com.cesarcosmico.fishdex.model.FishDexEntry;
import com.cesarcosmico.fishdex.model.FishSize;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.model.ServerRecord;
import com.cesarcosmico.fishdex.model.SortMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link FishDexOrdering} on its own: grouping applies only to the name sorts, the data sorts always sink
 * the undiscovered to the bottom, {@code ENGINE} keeps the input order, and the name key ignores leading
 * icons. The name normaliser is {@link Function#identity()} so the assertions read off the display names.
 */
class FishDexOrderingTest {

    private final FishDexOrdering ordering = new FishDexOrdering(Function.identity());

    @Test
    void nameSortHonoursTheGrouping() {
        List<FishDexEntry> entries = List.of(fish("Cod", true), fish("Bass", false), fish("Anchovy", true));

        assertEquals(List.of("Anchovy", "Bass", "Cod"), names(SortMode.NAME, DiscoveryGrouping.MIXED, entries));
        assertEquals(List.of("Anchovy", "Cod", "Bass"),
                names(SortMode.NAME, DiscoveryGrouping.DISCOVERED_FIRST, entries));
        assertEquals(List.of("Bass", "Anchovy", "Cod"),
                names(SortMode.NAME, DiscoveryGrouping.UNDISCOVERED_FIRST, entries));
    }

    @Test
    void nameDescReversesWithinTheGroup() {
        List<FishDexEntry> entries = List.of(fish("Anchovy", true), fish("Cod", true), fish("Bass", false));
        assertEquals(List.of("Cod", "Anchovy", "Bass"),
                names(SortMode.NAME_DESC, DiscoveryGrouping.DISCOVERED_FIRST, entries));
    }

    @Test
    void dataSortAlwaysSinksUndiscovered() {
        List<FishDexEntry> entries = List.of(
                counted("seen-low", true, 5), counted("unseen-high", false, 100), counted("seen-mid", true, 40));
        // Highest count first, but the undiscovered goes last whatever the grouping asked for.
        assertEquals(List.of("seen-mid", "seen-low", "unseen-high"),
                names(SortMode.COUNT, DiscoveryGrouping.MIXED, entries));
    }

    @Test
    void engineKeepsTheInputOrder() {
        List<FishDexEntry> entries = List.of(fish("Zebra", false), fish("Apple", true), fish("Mango", true));
        assertEquals(List.of("Zebra", "Apple", "Mango"),
                names(SortMode.ENGINE, DiscoveryGrouping.DISCOVERED_FIRST, entries));
    }

    @Test
    void nameKeyIgnoresLeadingIcons() {
        List<FishDexEntry> entries = List.of(fish("❤Cod", true), fish("*Anchovy", true), fish("Bass", true));
        assertEquals(List.of("*Anchovy", "Bass", "❤Cod"),
                names(SortMode.NAME, DiscoveryGrouping.MIXED, entries));
    }

    @Test
    void biomeSortPushesEntriesWithoutOneLast() {
        List<FishDexEntry> entries = List.of(withBiome("plains", "plains"), withBiome("none", null), withBiome("forest", "forest"));
        assertEquals(List.of("forest", "plains", "none"),
                names(SortMode.BIOME, DiscoveryGrouping.MIXED, entries));
    }

    @Test
    void sizeSortsPushEntriesWithoutDataLast() {
        List<FishDexEntry> entries = List.of(sized("mid", 50.0), sized("none", null), sized("big", 80.0));
        assertEquals(List.of("big", "mid", "none"), names(SortMode.MAX_SIZE, DiscoveryGrouping.MIXED, entries));
        assertEquals(List.of("mid", "big", "none"), names(SortMode.MIN_SIZE, DiscoveryGrouping.MIXED, entries));
    }

    private List<String> names(SortMode sort, DiscoveryGrouping grouping, List<FishDexEntry> entries) {
        return ordering.sorted(entries, sort, grouping).stream().map(FishDexEntry::displayName).toList();
    }

    private static FishDexEntry fish(String name, boolean discovered) {
        return entry(name, discovered, 0, null, null, null);
    }

    private static FishDexEntry counted(String name, boolean discovered, long count) {
        return entry(name, discovered, count, null, null, null);
    }

    private static FishDexEntry sized(String name, Double maxCm) {
        return entry(name, true, 0, maxCm, maxCm, null);
    }

    private static FishDexEntry withBiome(String name, String biome) {
        return entry(name, true, 0, null, null, biome);
    }

    private static FishDexEntry entry(String name, boolean discovered, long count, Double maxCm, Double minCm, String biome) {
        FishSize max = maxCm == null ? null : FishSize.ofCentimetres(maxCm);
        FishSize min = minCm == null ? null : FishSize.ofCentimetres(minCm);
        ServerRecord record = biome == null ? null : new ServerRecord(LootId.of(name), FishSize.ofCentimetres(1),
                UUID.randomUUID(), Instant.EPOCH, 0, null, null, null, biome);
        return new FishDexEntry(LootId.of(name), name, discovered, count, 0, min, max, null, null, record, null);
    }
}
