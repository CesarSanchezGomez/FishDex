package com.cesarcosmico.fishdex.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortModeTest {

    @Test
    void parsesIdsRegardlessOfSeparatorOrCase() {
        assertEquals(SortMode.NAME_DESC, SortMode.parse("name-desc"));
        assertEquals(SortMode.NAME_DESC, SortMode.parse("NAME_DESC"));
        assertEquals(SortMode.COUNT_ASC, SortMode.parse("count-asc"));
        assertEquals(SortMode.BIOME, SortMode.parse("biome"));
        assertEquals(SortMode.MAX_SIZE, SortMode.parse("max_size"));
    }

    @Test
    void nullAndUnknownFallBackToEngine() {
        assertEquals(SortMode.ENGINE, SortMode.parse(null));
        assertEquals(SortMode.ENGINE, SortMode.parse("does-not-exist"));
    }

    @Test
    void everyIdRoundTrips() {
        for (SortMode mode : SortMode.values()) {
            assertEquals(mode, SortMode.parse(mode.id()));
        }
    }
}
