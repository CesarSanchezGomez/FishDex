package com.cesarcosmico.fishdex.model;

public record FishSize(double centimetres) implements Comparable<FishSize> {

    public FishSize {
        if (centimetres < 0) {
            throw new IllegalArgumentException("size must not be negative: " + centimetres);
        }
    }

    public static FishSize ofCentimetres(double cm) {
        return new FishSize(cm);
    }

    /** Two-decimal representation, matching the in-game display convention (e.g. {@code 82.40}). */
    public String formatted() {
        return String.format(java.util.Locale.ROOT, "%.2f", centimetres);
    }

    @Override
    public int compareTo(FishSize other) {
        return Double.compare(this.centimetres, other.centimetres);
    }
}
