package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.model.CatchRecord;
import com.cesarcosmico.fishdex.storage.DiscoveryRepository;
import com.cesarcosmico.fishdex.storage.PlayerStatsRepository;
import com.cesarcosmico.fishdex.storage.ServerRecordRepository;

import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CatchService {

    private final PlayerStatsRepository playerStats;
    private final ServerRecordRepository serverRecords;
    private final DiscoveryRepository discoveries;
    private final Logger logger;

    public CatchService(PlayerStatsRepository playerStats,
                        ServerRecordRepository serverRecords,
                        DiscoveryRepository discoveries,
                        Logger logger) {
        this.playerStats = Objects.requireNonNull(playerStats);
        this.serverRecords = Objects.requireNonNull(serverRecords);
        this.discoveries = Objects.requireNonNull(discoveries);
        this.logger = Objects.requireNonNull(logger);
    }

    public void handle(CatchRecord ctx) {
        playerStats.recordCatch(ctx).exceptionally(logFailure("record player stats"));

        discoveries.recordFirstCatch(ctx.loot(), ctx.player(), ctx.caughtAt())
                .exceptionally(logFailure("record discovery"));

        // Server records are size-based; size-less loot (trash/blocks) is counted but not ranked.
        if (ctx.hasSize()) {
            serverRecords.offer(ctx).exceptionally(logFailure("offer server record"));
        }
    }

    private <T> Function<Throwable, T> logFailure(String what) {
        return throwable -> {
            logger.log(Level.WARNING, "Failed to " + what, throwable);
            return null;
        };
    }
}
