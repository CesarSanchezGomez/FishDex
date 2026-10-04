package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.model.LootId;
import com.cesarcosmico.fishdex.storage.SpeciesSalesRepository;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SaleService {

    private final SpeciesSalesRepository repository;
    private final Logger logger;

    public SaleService(SpeciesSalesRepository repository, Logger logger) {
        this.repository = Objects.requireNonNull(repository);
        this.logger = Objects.requireNonNull(logger);
    }

    public void record(UUID player, Map<LootId, Integer> soldByLoot) {
        soldByLoot.forEach((loot, amount) -> repository.addSales(player, loot, amount)
                .exceptionally(error -> {
                    logger.log(Level.WARNING, "Failed to record sale of " + loot.value(), error);
                    return null;
                }));
    }
}
