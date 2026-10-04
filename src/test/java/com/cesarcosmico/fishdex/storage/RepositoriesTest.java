package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseSettings;
import com.cesarcosmico.fishdex.model.CatchRecord;
import com.cesarcosmico.fishdex.model.FishSize;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.model.ServerRecord;
import com.cesarcosmico.fishdex.model.SpeciesStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoriesTest {

    private static final Executor DIRECT = Runnable::run;

    private Database database;
    private PlayerStatsRepository playerStats;
    private ServerRecordRepository serverRecords;
    private DiscoveryRepository discoveries;
    private SpeciesSalesRepository speciesSales;

    @BeforeEach
    void setUp(@TempDir Path dir) throws Exception {
        DatabaseSettings settings = DatabaseSettings.sqlite("test.db");
        database = Database.openWith(settings, dir.toFile(), DIRECT);
        playerStats = new PlayerStatsRepository(database);
        serverRecords = new ServerRecordRepository(database);
        discoveries = new DiscoveryRepository(database);
        speciesSales = new SpeciesSalesRepository(database);
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void speciesStatsAccumulateCountAndTrackMinMax() {
        UUID player = UUID.randomUUID();
        LootId cod = LootId.of("cod");

        playerStats.recordCatch(new CatchRecord(player, cod, FishSize.ofCentimetres(50), 1, "minecraft:ocean", Instant.now())).join();
        playerStats.recordCatch(new CatchRecord(player, cod, FishSize.ofCentimetres(30), 2, "minecraft:ocean", Instant.now())).join();

        SpeciesStats stats = statsOf(player, cod);
        assertEquals(3, stats.count(), "count should accumulate the amount of each catch");
        assertEquals(30.0, stats.minSize().centimetres(), 1e-9, "min tracks the smallest");
        assertEquals(50.0, stats.maxSize().centimetres(), 1e-9, "max tracks the largest");
        assertNotNull(stats.firstCatchAt());
        assertNotNull(stats.lastCatchAt());
    }

    @Test
    void sizelessLootIsCountedButHasNoSize() {
        UUID player = UUID.randomUUID();
        LootId trash = LootId.of("seaweed");

        playerStats.recordCatch(new CatchRecord(player, trash, null, 1, null, Instant.now())).join();
        playerStats.recordCatch(new CatchRecord(player, trash, null, 1, null, Instant.now())).join();

        SpeciesStats stats = statsOf(player, trash);
        assertEquals(2, stats.count());
        assertNull(stats.minSize());
        assertNull(stats.maxSize());
    }

    @Test
    void sizeIsTrackedEvenWhenTheFirstCatchHadNone() {
        UUID player = UUID.randomUUID();
        LootId cod = LootId.of("cod");

        playerStats.recordCatch(new CatchRecord(player, cod, null, 1, null, Instant.now())).join();
        playerStats.recordCatch(new CatchRecord(player, cod, FishSize.ofCentimetres(40), 1, "minecraft:ocean", Instant.now())).join();

        SpeciesStats stats = statsOf(player, cod);
        assertEquals(2, stats.count());
        assertNotNull(stats.minSize(), "a sizeless first catch must not pin min to NULL");
        assertEquals(40.0, stats.minSize().centimetres(), 1e-9);
        assertEquals(40.0, stats.maxSize().centimetres(), 1e-9);
    }

    @Test
    void serverRecordKeepsTheLargestAndCountsAllCatches() {
        LootId cod = LootId.of("cod");
        UUID big = UUID.randomUUID();
        UUID small = UUID.randomUUID();

        serverRecords.offer(new CatchRecord(big, cod, FishSize.ofCentimetres(50), 1, "minecraft:ocean", Instant.now())).join();
        serverRecords.offer(new CatchRecord(small, cod, FishSize.ofCentimetres(30), 1, "minecraft:river", Instant.now())).join();

        ServerRecord record = recordOf(cod);
        assertEquals(50.0, record.bestSize().centimetres(), 1e-9, "the largest catch is kept");
        assertEquals(big, record.holder());
        assertEquals(2, record.totalGlobalCount(), "every catch increments the global counter");
        assertEquals(small, record.lastCatcher(), "last catch is always the most recent");
        assertEquals(30.0, record.lastSize().centimetres(), 1e-9);
        assertEquals("minecraft:river", record.lastBiome());
    }

    @Test
    void serverRecordHolderMovesToANewLargestCatch() {
        LootId cod = LootId.of("cod");
        UUID first = UUID.randomUUID();
        UUID beater = UUID.randomUUID();

        serverRecords.offer(new CatchRecord(first, cod, FishSize.ofCentimetres(30), 1, "minecraft:river", Instant.now())).join();
        serverRecords.offer(new CatchRecord(beater, cod, FishSize.ofCentimetres(50), 1, "minecraft:ocean", Instant.now())).join();

        ServerRecord record = recordOf(cod);
        assertEquals(50.0, record.bestSize().centimetres(), 1e-9);
        assertEquals(beater, record.holder(), "the holder follows the new record, not just the size");
    }

    @Test
    void speciesSalesAccumulatePerPlayerAndSpecies() {
        UUID player = UUID.randomUUID();
        LootId cod = LootId.of("cod");

        speciesSales.addSales(player, cod, 3).join();
        speciesSales.addSales(player, cod, 2).join();
        speciesSales.addSales(player, LootId.of("salmon"), 1).join();

        var byLoot = speciesSales.findAll(player).join().stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.loot().value(), s -> s.soldCount()));
        assertEquals(5L, byLoot.get("cod"), "sold counts accumulate per species");
        assertEquals(1L, byLoot.get("salmon"));
    }

    @Test
    void discoveryIsRecordedOnlyOnce() {
        LootId cod = LootId.of("cod");
        assertTrue(discoveries.recordFirstCatch(cod, UUID.randomUUID(), Instant.now()).join(), "first is a discovery");
        assertFalse(discoveries.recordFirstCatch(cod, UUID.randomUUID(), Instant.now()).join(), "later catches are not");

        boolean recorded = discoveries.findAll().join().stream().anyMatch(d -> d.loot().equals(cod));
        assertTrue(recorded);
    }

    private SpeciesStats statsOf(UUID player, LootId loot) {
        return playerStats.findAll(player).join().stream()
                .filter(s -> s.loot().equals(loot)).findFirst().orElseThrow();
    }

    private ServerRecord recordOf(LootId loot) {
        return serverRecords.findAll().join().stream()
                .filter(r -> r.loot().equals(loot)).findFirst().orElseThrow();
    }
}
