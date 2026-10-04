package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseType;
import com.cesarcosmico.fishdex.model.CatchRecord;
import com.cesarcosmico.fishdex.model.FishSize;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.model.SpeciesStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerStatsRepository {

    private final Database database;
    private final String upsertWithSize;
    private final String upsertNoSize;

    public PlayerStatsRepository(Database database) {
        this.database = database;
        DatabaseType dialect = database.type();
        boolean sqlite = dialect == DatabaseType.SQLITE;
        String insert = "INSERT INTO species_stats"
                + " (uuid, loot_id, count, min_size, max_size, first_catch_at, last_catch_at) VALUES ";

        // MIN/MAX/LEAST/GREATEST return NULL if any argument is NULL, so a row first stored by a sizeless
        // catch would never get a size: COALESCE falls back to the incoming value.
        this.upsertWithSize = insert + "(?, ?, ?, ?, ?, ?, ?) " + (sqlite
                ? "ON CONFLICT(uuid, loot_id) DO UPDATE SET"
                    + " count = species_stats.count + excluded.count,"
                    + " min_size = MIN(COALESCE(species_stats.min_size, excluded.min_size), excluded.min_size),"
                    + " max_size = MAX(COALESCE(species_stats.max_size, excluded.max_size), excluded.max_size),"
                    + " last_catch_at = excluded.last_catch_at"
                : "ON DUPLICATE KEY UPDATE"
                    + " count = count + VALUES(count),"
                    + " min_size = LEAST(COALESCE(min_size, VALUES(min_size)), VALUES(min_size)),"
                    + " max_size = GREATEST(COALESCE(max_size, VALUES(max_size)), VALUES(max_size)),"
                    + " last_catch_at = VALUES(last_catch_at)");

        this.upsertNoSize = insert + "(?, ?, ?, NULL, NULL, ?, ?) " + (sqlite
                ? "ON CONFLICT(uuid, loot_id) DO UPDATE SET"
                    + " count = species_stats.count + excluded.count,"
                    + " last_catch_at = excluded.last_catch_at"
                : "ON DUPLICATE KEY UPDATE"
                    + " count = count + VALUES(count),"
                    + " last_catch_at = VALUES(last_catch_at)");
    }

    public CompletableFuture<Void> recordCatch(CatchRecord record) {
        long now = record.caughtAt().toEpochMilli();
        return database.execute(conn -> {
            if (record.hasSize()) {
                double size = record.size().centimetres();
                try (PreparedStatement ps = conn.prepareStatement(upsertWithSize)) {
                    ps.setString(1, record.player().toString());
                    ps.setString(2, record.loot().value());
                    ps.setInt(3, record.amount());
                    ps.setDouble(4, size);
                    ps.setDouble(5, size);
                    ps.setLong(6, now);
                    ps.setLong(7, now);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(upsertNoSize)) {
                    ps.setString(1, record.player().toString());
                    ps.setString(2, record.loot().value());
                    ps.setInt(3, record.amount());
                    ps.setLong(4, now);
                    ps.setLong(5, now);
                    ps.executeUpdate();
                }
            }
        });
    }

    public CompletableFuture<List<SpeciesStats>> findAll(UUID player) {
        return database.query(conn -> {
            List<SpeciesStats> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT loot_id, count, min_size, max_size, first_catch_at, last_catch_at"
                            + " FROM species_stats WHERE uuid = ?")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(map(rs));
                    }
                }
            }
            return result;
        });
    }

    private static SpeciesStats map(ResultSet rs) throws SQLException {
        LootId loot = LootId.of(rs.getString("loot_id"));
        long count = rs.getLong("count");
        FishSize min = readSize(rs, "min_size");
        FishSize max = readSize(rs, "max_size");
        Instant first = readInstant(rs, "first_catch_at");
        Instant last = readInstant(rs, "last_catch_at");
        return new SpeciesStats(loot, count, min, max, first, last);
    }

    private static FishSize readSize(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : FishSize.ofCentimetres(value);
    }

    private static Instant readInstant(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Instant.ofEpochMilli(value);
    }
}
