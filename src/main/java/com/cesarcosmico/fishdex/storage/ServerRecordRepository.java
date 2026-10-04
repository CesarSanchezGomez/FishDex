package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseType;
import com.cesarcosmico.fishdex.model.CatchRecord;
import com.cesarcosmico.fishdex.model.FishSize;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.model.ServerRecord;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * One upsert per catch: bumps the global counter, refreshes the last-catch fields and replaces the
 * record only when the catch is larger.
 */
public final class ServerRecordRepository {

    private final Database database;

    private static final String COLUMNS =
            "loot_id, best_size, holder_uuid, achieved_at, total_global_count,"
                    + " last_catcher_uuid, last_size, last_caught_at, last_biome";

    private final String upsert;

    public ServerRecordRepository(Database database) {
        this.database = database;
        DatabaseType dialect = database.type();
        boolean sqlite = dialect == DatabaseType.SQLITE;
        this.upsert = "INSERT INTO server_records (" + COLUMNS + ") VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?) " + (sqlite
                ? "ON CONFLICT(loot_id) DO UPDATE SET"
                    + " total_global_count = server_records.total_global_count + 1,"
                    + " best_size = CASE WHEN excluded.best_size > server_records.best_size THEN excluded.best_size ELSE server_records.best_size END,"
                    + " holder_uuid = CASE WHEN excluded.best_size > server_records.best_size THEN excluded.holder_uuid ELSE server_records.holder_uuid END,"
                    + " achieved_at = CASE WHEN excluded.best_size > server_records.best_size THEN excluded.achieved_at ELSE server_records.achieved_at END,"
                    + " last_catcher_uuid = excluded.last_catcher_uuid,"
                    + " last_size = excluded.last_size,"
                    + " last_caught_at = excluded.last_caught_at,"
                    + " last_biome = excluded.last_biome"
                // MySQL evaluates these assignments left to right: best_size must be updated last, or
                // the holder/date conditions would compare against the already-updated size.
                : "ON DUPLICATE KEY UPDATE"
                    + " total_global_count = total_global_count + 1,"
                    + " holder_uuid = IF(VALUES(best_size) > best_size, VALUES(holder_uuid), holder_uuid),"
                    + " achieved_at = IF(VALUES(best_size) > best_size, VALUES(achieved_at), achieved_at),"
                    + " best_size = IF(VALUES(best_size) > best_size, VALUES(best_size), best_size),"
                    + " last_catcher_uuid = VALUES(last_catcher_uuid),"
                    + " last_size = VALUES(last_size),"
                    + " last_caught_at = VALUES(last_caught_at),"
                    + " last_biome = VALUES(last_biome)");
    }

    public CompletableFuture<Void> offer(CatchRecord caught) {
        return database.execute(conn -> {
            double size = caught.size().centimetres();
            long now = caught.caughtAt().toEpochMilli();
            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setString(1, caught.loot().value());
                ps.setDouble(2, size);
                ps.setString(3, caught.player().toString());
                ps.setLong(4, now);
                ps.setString(5, caught.player().toString());
                ps.setDouble(6, size);
                ps.setLong(7, now);
                ps.setString(8, caught.biome());
                ps.executeUpdate();
            }
        });
    }

    public CompletableFuture<List<ServerRecord>> findAll() {
        return database.query(conn -> {
            List<ServerRecord> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + " FROM server_records");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        });
    }

    private static ServerRecord map(ResultSet rs) throws SQLException {
        return new ServerRecord(
                LootId.of(rs.getString("loot_id")),
                FishSize.ofCentimetres(rs.getDouble("best_size")),
                UUID.fromString(rs.getString("holder_uuid")),
                Instant.ofEpochMilli(rs.getLong("achieved_at")),
                rs.getLong("total_global_count"),
                readUuid(rs, "last_catcher_uuid"),
                readSize(rs, "last_size"),
                readInstant(rs, "last_caught_at"),
                rs.getString("last_biome"));
    }

    private static UUID readUuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
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
