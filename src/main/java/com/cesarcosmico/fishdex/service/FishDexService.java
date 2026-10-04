package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.model.CategoryProgress;
import com.cesarcosmico.fishdex.model.Discovery;
import com.cesarcosmico.fishdex.model.FishDexEntry;
import com.cesarcosmico.fishdex.model.FishDexSummary;
import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.model.ServerRecord;
import com.cesarcosmico.fishdex.model.SpeciesSales;
import com.cesarcosmico.fishdex.model.SpeciesStats;
import com.cesarcosmico.fishdex.storage.DiscoveryRepository;
import com.cesarcosmico.fishdex.storage.PlayerStatsRepository;
import com.cesarcosmico.fishdex.storage.ServerRecordRepository;
import com.cesarcosmico.fishdex.storage.SpeciesSalesRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Joins CustomFishing's loot list with the stored stats, records, discoveries and sales. The engine is read
 * on the calling (main) thread; the returned future completes on the database thread, so callers hop back
 * before touching Bukkit.
 */
public final class FishDexService {

    private final FishingEngine engine;
    private final PlayerStatsRepository playerStats;
    private final ServerRecordRepository serverRecords;
    private final DiscoveryRepository discoveries;
    private final SpeciesSalesRepository speciesSales;

    public FishDexService(FishingEngine engine,
                          PlayerStatsRepository playerStats,
                          ServerRecordRepository serverRecords,
                          DiscoveryRepository discoveries,
                          SpeciesSalesRepository speciesSales) {
        this.engine = Objects.requireNonNull(engine);
        this.playerStats = Objects.requireNonNull(playerStats);
        this.serverRecords = Objects.requireNonNull(serverRecords);
        this.discoveries = Objects.requireNonNull(discoveries);
        this.speciesSales = Objects.requireNonNull(speciesSales);
    }

    /** A blank {@code category} means all loot. */
    public CompletableFuture<List<FishDexEntry>> entriesFor(UUID player, String category) {
        List<LootId> ids = (category == null || category.isBlank())
                ? engine.allLootIds()
                : engine.categoryMembers(category);
        Map<LootId, String> names = new LinkedHashMap<>();
        for (LootId id : ids) {
            names.put(id, engine.displayName(id).orElse(id.value()));
        }

        CompletableFuture<List<SpeciesStats>> statsFuture = playerStats.findAll(player);
        CompletableFuture<List<ServerRecord>> recordsFuture = serverRecords.findAll();
        CompletableFuture<List<Discovery>> discoveriesFuture = discoveries.findAll();
        CompletableFuture<List<SpeciesSales>> salesFuture = speciesSales.findAll(player);

        return CompletableFuture.allOf(statsFuture, recordsFuture, discoveriesFuture, salesFuture)
                .thenApply(ignored -> {
                    Map<LootId, SpeciesStats> stats = index(statsFuture.join(), SpeciesStats::loot);
                    Map<LootId, ServerRecord> records = index(recordsFuture.join(), ServerRecord::loot);
                    Map<LootId, Discovery> discovered = index(discoveriesFuture.join(), Discovery::loot);
                    Map<LootId, SpeciesSales> sales = index(salesFuture.join(), SpeciesSales::loot);

                    List<FishDexEntry> entries = new ArrayList<>(ids.size());
                    for (LootId id : ids) {
                        SpeciesStats s = stats.get(id);
                        SpeciesSales sold = sales.get(id);
                        entries.add(new FishDexEntry(
                                id,
                                names.get(id),
                                s != null && s.count() > 0,
                                s == null ? 0L : s.count(),
                                sold == null ? 0L : sold.soldCount(),
                                s == null ? null : s.minSize(),
                                s == null ? null : s.maxSize(),
                                s == null ? null : s.firstCatchAt(),
                                s == null ? null : s.lastCatchAt(),
                                records.get(id),
                                discovered.get(id)));
                    }
                    return entries;
                });
    }

    public CompletableFuture<FishDexSummary> summary(UUID player, Map<String, String> categoryIdToSource) {
        Map<String, List<LootId>> members = new LinkedHashMap<>();
        Set<LootId> union = new HashSet<>();
        for (Map.Entry<String, String> entry : categoryIdToSource.entrySet()) {
            List<LootId> ids = engine.categoryMembers(entry.getValue());
            members.put(entry.getKey(), ids);
            union.addAll(ids);
        }

        CompletableFuture<List<SpeciesStats>> statsFuture = playerStats.findAll(player);
        CompletableFuture<List<ServerRecord>> recordsFuture = serverRecords.findAll();

        return CompletableFuture.allOf(statsFuture, recordsFuture).thenApply(ignored -> {
            Map<LootId, SpeciesStats> stats = index(statsFuture.join(), SpeciesStats::loot);
            Map<LootId, ServerRecord> records = index(recordsFuture.join(), ServerRecord::loot);
            Map<String, CategoryProgress> perCategory = new LinkedHashMap<>();
            members.forEach((id, ids) -> perCategory.put(id, progress(ids, stats, records)));
            CategoryProgress overall = progress(new ArrayList<>(union), stats, records);
            return new FishDexSummary(perCategory, overall);
        });
    }

    private static CategoryProgress progress(List<LootId> members,
                                             Map<LootId, SpeciesStats> stats,
                                             Map<LootId, ServerRecord> records) {
        int discovered = 0;
        long playerCaught = 0;
        long globalCaught = 0;
        for (LootId id : members) {
            SpeciesStats s = stats.get(id);
            if (s != null && s.count() > 0) {
                discovered++;
                playerCaught += s.count();
            }
            ServerRecord r = records.get(id);
            if (r != null) {
                globalCaught += r.totalGlobalCount();
            }
        }
        return new CategoryProgress(discovered, members.size(), playerCaught, globalCaught);
    }

    private static <T> Map<LootId, T> index(List<T> values, Function<T, LootId> key) {
        return values.stream().collect(Collectors.toMap(key, Function.identity(), (a, b) -> a));
    }
}
