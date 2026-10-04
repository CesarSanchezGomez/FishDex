package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseType;
import com.cesarcosmico.fishdex.model.SortPreference;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** The {@code sort_mode} column holds the whole serialised {@link SortPreference} (criterion + grouping). */
public final class PreferenceRepository {

    private final Database database;
    private final String upsert;

    public PreferenceRepository(Database database) {
        this.database = database;
        DatabaseType dialect = database.type();
        this.upsert = "INSERT INTO player_preferences (player_uuid, sort_mode, updated_at) VALUES (?, ?, ?) "
                + (dialect == DatabaseType.SQLITE
                    ? "ON CONFLICT(player_uuid) DO UPDATE SET sort_mode = excluded.sort_mode, updated_at = excluded.updated_at"
                    : "ON DUPLICATE KEY UPDATE sort_mode = VALUES(sort_mode), updated_at = VALUES(updated_at)");
    }

    public CompletableFuture<Optional<SortPreference>> find(UUID player) {
        return database.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT sort_mode FROM player_preferences WHERE player_uuid = ?")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.<SortPreference>empty();
                    }
                    return Optional.of(SortPreference.parse(rs.getString("sort_mode")));
                }
            }
        });
    }

    public CompletableFuture<Void> save(UUID player, SortPreference preference) {
        return database.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setString(1, player.toString());
                ps.setString(2, preference.serialize());
                ps.setLong(3, Instant.now().toEpochMilli());
                ps.executeUpdate();
            }
        });
    }
}
