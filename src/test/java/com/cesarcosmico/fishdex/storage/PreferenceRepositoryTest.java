package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseSettings;
import com.cesarcosmico.fishdex.model.DiscoveryGrouping;
import com.cesarcosmico.fishdex.model.SortMode;
import com.cesarcosmico.fishdex.model.SortPreference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenceRepositoryTest {

    private static final Executor DIRECT = Runnable::run;

    private Database database;
    private PreferenceRepository repository;

    @BeforeEach
    void setUp(@TempDir Path dir) throws Exception {
        DatabaseSettings settings = DatabaseSettings.sqlite("test.db");
        database = Database.openWith(settings, dir.toFile(), DIRECT);
        repository = new PreferenceRepository(database);
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void noPreferenceByDefault() {
        assertTrue(repository.find(UUID.randomUUID()).join().isEmpty());
    }

    @Test
    void savesAndReplacesTheChoice() {
        UUID player = UUID.randomUUID();

        SortPreference first = new SortPreference(SortMode.NAME_DESC, DiscoveryGrouping.UNDISCOVERED_FIRST);
        repository.save(player, first).join();
        assertEquals(first, repository.find(player).join().orElseThrow(),
                "criterion and grouping round-trip together");

        SortPreference second = new SortPreference(SortMode.COUNT, DiscoveryGrouping.MIXED);
        repository.save(player, second).join();
        assertEquals(second, repository.find(player).join().orElseThrow(),
                "saving again replaces the previous preference");
    }
}
