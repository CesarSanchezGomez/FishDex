package com.cesarcosmico.fishdex.model;

import java.util.List;

public record CategoryProgress(int discovered, int total, long playerCaught, long globalCaught) {

    public double percent() {
        return total == 0 ? 0.0 : discovered * 100.0 / total;
    }

    public static CategoryProgress of(List<FishDexEntry> entries) {
        int discovered = 0;
        long player = 0;
        long global = 0;
        for (FishDexEntry entry : entries) {
            if (entry.discovered()) {
                discovered++;
            }
            player += entry.count();
            global += entry.serverRecordOptional().map(ServerRecord::totalGlobalCount).orElse(0L);
        }
        return new CategoryProgress(discovered, entries.size(), player, global);
    }
}
