package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.config.DatabaseSettings;
import com.cesarcosmico.fishdex.model.CatchRecord;
import com.cesarcosmico.fishdex.model.FishDexEntry;
import com.cesarcosmico.fishdex.model.FishSize;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.storage.Database;
import com.cesarcosmico.fishdex.storage.DiscoveryRepository;
import com.cesarcosmico.fishdex.storage.PlayerStatsRepository;
import com.cesarcosmico.fishdex.storage.ServerRecordRepository;
import com.cesarcosmico.fishdex.storage.SpeciesSalesRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishDexServiceTest {

    private static final Executor DIRECT = Runnable::run;

    private Database database;
    private FishDexService service;

    /** Two species in every category, no display names. */
    private static final FishingEngine FAKE_ENGINE = new FishingEngine() {
        @Override public boolean isAvailable() { return true; }
        @Override public String engineVersion() { return "test"; }
        @Override public List<LootId> allLootIds() { return List.of(LootId.of("cod"), LootId.of("salmon")); }
        @Override public List<LootId> categoryMembers(String category) { return allLootIds(); }
        @Override public Optional<String> displayName(LootId loot) { return Optional.empty(); }
        @Override public Optional<ItemStack> icon(LootId loot, Player viewer) { return Optional.empty(); }
    };

    @BeforeEach
    void setUp(@TempDir Path dir) throws Exception {
        DatabaseSettings settings = DatabaseSettings.sqlite("test.db");
        database = Database.openWith(settings, dir.toFile(), DIRECT);
        var stats = new PlayerStatsRepository(database);
        var records = new ServerRecordRepository(database);
        var discoveries = new DiscoveryRepository(database);
        var sales = new SpeciesSalesRepository(database);
        service = new FishDexService(FAKE_ENGINE, stats, records, discoveries, sales);

        UUID player = TestPlayers.PLAYER;
        CatchRecord catchRecord = new CatchRecord(player, LootId.of("cod"), FishSize.ofCentimetres(50), 1, "minecraft:ocean", Instant.now());
        stats.recordCatch(catchRecord).join();
        records.offer(catchRecord).join();
        discoveries.recordFirstCatch(LootId.of("cod"), player, Instant.now()).join();
        sales.addSales(player, LootId.of("cod"), 3).join();
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void buildsEntriesForEveryLootWithDiscoveryState() {
        List<FishDexEntry> entries = service.entriesFor(TestPlayers.PLAYER, "common_fishes").join();
        Map<LootId, FishDexEntry> byLoot = entries.stream()
                .collect(Collectors.toMap(FishDexEntry::loot, e -> e));

        assertEquals(2, entries.size(), "one entry per loot in the category");

        FishDexEntry cod = byLoot.get(LootId.of("cod"));
        assertTrue(cod.discovered());
        assertEquals(1, cod.count());
        assertEquals(50.0, cod.maxSize().centimetres(), 1e-9);
        assertNotNull(cod.firstCatchAt());
        assertNotNull(cod.lastCatchAt());
        assertEquals(3, cod.soldCount(), "sold count comes from species_sales");
        assertTrue(cod.serverRecordOptional().isPresent());
        assertTrue(cod.discoveryOptional().isPresent());

        FishDexEntry salmon = byLoot.get(LootId.of("salmon"));
        assertFalse(salmon.discovered(), "uncaught species shows as undiscovered");
        assertEquals(0, salmon.count());
    }

    private static final class TestPlayers {
        static final UUID PLAYER = UUID.randomUUID();
    }
}
