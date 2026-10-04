package com.cesarcosmico.fishdex.menu;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Fixes the owner's centred patterns for a row of nine (odd counts contiguous, even counts split around a
 * middle gap) and the multi-row case (full rows first, remainder centred below). FILL keeps the
 * left-to-right packing.
 */
class ContentLayoutTest {

    private static final int[] ROW = IntStream.range(0, 9).toArray();
    private static final int[] TWO_ROWS = IntStream.range(0, 18).toArray();

    @Test
    void centersOddCounts() {
        assertArrayEquals(new int[]{4}, ContentLayout.place(ContentLayout.Mode.CENTERED, ROW, 1));
        assertArrayEquals(new int[]{3, 4, 5}, ContentLayout.place(ContentLayout.Mode.CENTERED, ROW, 3));
    }

    @Test
    void centersEvenCountsWithAMiddleGap() {
        assertArrayEquals(new int[]{2, 3, 5, 6}, ContentLayout.place(ContentLayout.Mode.CENTERED, ROW, 4));
        assertArrayEquals(new int[]{1, 2, 3, 5, 6, 7}, ContentLayout.place(ContentLayout.Mode.CENTERED, ROW, 6));
        assertArrayEquals(new int[]{0, 1, 2, 3, 5, 6, 7, 8}, ContentLayout.place(ContentLayout.Mode.CENTERED, ROW, 8));
    }

    @Test
    void fillsAFullRow() {
        assertArrayEquals(ROW, ContentLayout.place(ContentLayout.Mode.CENTERED, ROW, 9));
    }

    @Test
    void fillsFullRowsThenCentersTheRemainder() {
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 13},
                ContentLayout.place(ContentLayout.Mode.CENTERED, TWO_ROWS, 10));
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 14, 15},
                ContentLayout.place(ContentLayout.Mode.CENTERED, TWO_ROWS, 13));
    }

    @Test
    void fillPacksLeftToRight() {
        assertArrayEquals(new int[]{0, 1, 2, 3}, ContentLayout.place(ContentLayout.Mode.FILL, ROW, 4));
    }
}
