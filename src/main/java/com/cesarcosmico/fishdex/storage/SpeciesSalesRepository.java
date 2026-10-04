package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseType;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.model.SpeciesSales;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SpeciesSalesRepository {

    private final Database database;
    private final String upsert;

    public SpeciesSalesRepository(Database database) {
        this.database = database;
        DatabaseType dialect = database.type();
        this.upsert = "INSERT INTO species_sales (uuid, loot_id, sold_count) VALUES (?, ?, ?) "
                + (dialect == DatabaseType.SQLITE
                    ? "ON CONFLICT(uuid, loot_id) DO UPDATE SET sold_count = sold_count + excluded.sold_count"
                    : "ON DUPLICATE KEY UPDATE sold_count = sold_count + VALUES(sold_count)");
    }

    public CompletableFuture<Void> addSales(UUID player, LootId loot, int amount) {
        return database.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setString(1, player.toString());
                ps.setString(2, loot.value());
                ps.setInt(3, amount);
                ps.executeUpdate();
            }
        });
    }

    public CompletableFuture<List<SpeciesSales>> findAll(UUID player) {
        return database.query(conn -> {
            List<SpeciesSales> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT loot_id, sold_count FROM species_sales WHERE uuid = ?")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(new SpeciesSales(LootId.of(rs.getString("loot_id")), rs.getLong("sold_count")));
                    }
                }
            }
            return result;
        });
    }
}
