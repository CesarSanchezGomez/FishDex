package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseType;
import com.cesarcosmico.fishdex.model.Discovery;
import com.cesarcosmico.fishdex.model.LootId;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** First writer wins: insert-or-ignore turns later catches into no-ops. */
public final class DiscoveryRepository {

    private final Database database;
    private final String insertIgnore;

    public DiscoveryRepository(Database database) {
        this.database = database;
        DatabaseType dialect = database.type();
        String columns = " INTO discoveries (loot_id, first_player_uuid, discovered_at) VALUES (?, ?, ?)";
        this.insertIgnore = (dialect == DatabaseType.SQLITE ? "INSERT OR IGNORE" : "INSERT IGNORE") + columns;
    }

    public CompletableFuture<Boolean> recordFirstCatch(LootId loot, UUID player, Instant at) {
        return database.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(insertIgnore)) {
                ps.setString(1, loot.value());
                ps.setString(2, player.toString());
                ps.setLong(3, at.toEpochMilli());
                return ps.executeUpdate() > 0;
            }
        });
    }

    public CompletableFuture<List<Discovery>> findAll() {
        return database.query(conn -> {
            List<Discovery> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT loot_id, first_player_uuid, discovered_at FROM discoveries");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Discovery(
                            LootId.of(rs.getString("loot_id")),
                            UUID.fromString(rs.getString("first_player_uuid")),
                            Instant.ofEpochMilli(rs.getLong("discovered_at"))));
                }
            }
            return result;
        });
    }
}
