package com.cesarcosmico.fishdex.menu;

import java.util.Arrays;

/**
 * Places a category's entries inside its content-slots. FILL packs them left-to-right; CENTERED fills
 * full rows of nine and centres the last partial row (even counts leave a middle gap, e.g. 4 -> XXAAXAAXX,
 * 6 -> XAAAXAAAX). Pure integer logic (no Bukkit), so it is unit-tested directly. CENTERED needs
 * row-aligned content-slots (a multiple of nine); otherwise it falls back to FILL.
 */
public final class ContentLayout {

    private static final int WIDTH = 9;

    public enum Mode {
        FILL, CENTERED;

        public static Mode parse(String raw) {
            return "fill".equalsIgnoreCase(raw) ? FILL : CENTERED;
        }
    }

    private ContentLayout() {
    }

    /** Target slots for {@code count} items among {@code slots}, per {@code mode}. */
    public static int[] place(Mode mode, int[] slots, int count) {
        int n = Math.max(0, Math.min(count, slots.length));
        if (mode == Mode.FILL || slots.length % WIDTH != 0) {
            return Arrays.copyOf(slots, n);
        }
        int[] targets = new int[n];
        int placed = 0;
        int remaining = n;
        int row = 0;
        while (remaining > 0) {
            int rowStart = row * WIDTH;
            int inRow = Math.min(WIDTH, remaining);
            for (int col : columns(inRow, WIDTH)) {
                targets[placed++] = slots[rowStart + col];
            }
            remaining -= inRow;
            row++;
        }
        return targets;
    }

    /** Column indices (0..width-1) occupied by {@code k} centred items in a row of {@code width}. */
    static int[] columns(int k, int width) {
        if (k >= width) {
            int[] full = new int[width];
            for (int i = 0; i < width; i++) {
                full[i] = i;
            }
            return full;
        }
        int center = (width - 1) / 2;
        int[] cols = new int[k];
        if (k % 2 == 1) {
            int start = center - (k - 1) / 2;
            for (int i = 0; i < k; i++) {
                cols[i] = start + i;
            }
        } else {
            int half = k / 2;
            int idx = 0;
            for (int i = 0; i < half; i++) {
                cols[idx++] = center - half + i;
            }
            for (int i = 0; i < half; i++) {
                cols[idx++] = center + 1 + i;
            }
        }
        return cols;
    }
}
