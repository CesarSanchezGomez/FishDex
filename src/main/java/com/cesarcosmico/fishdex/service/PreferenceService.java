package com.cesarcosmico.fishdex.service;

import com.cesarcosmico.fishdex.model.SortPreference;
import com.cesarcosmico.fishdex.storage.PreferenceRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Per-player FishDex ordering, cached for synchronous reads on the main thread. */
public final class PreferenceService {

    private final PreferenceRepository repository;
    private final Logger logger;
    private final Set<UUID> online = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, SortPreference> cache = new ConcurrentHashMap<>();

    public PreferenceService(PreferenceRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    public void load(UUID player) {
        online.add(player);
        repository.find(player).whenComplete((stored, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Failed to load FishDex preferences of " + player, error);
                return;
            }
            // The load may finish after the player quit, or after they already picked a new order.
            stored.ifPresent(preference -> cache.compute(player,
                    (key, current) -> online.contains(key) && current == null ? preference : current));
        });
    }

    public void evict(UUID player) {
        online.remove(player);
        cache.remove(player);
    }

    public Optional<SortPreference> get(UUID player) {
        return Optional.ofNullable(cache.get(player));
    }

    public void set(UUID player, SortPreference preference) {
        cache.put(player, preference);
        repository.save(player, preference).exceptionally(error -> {
            logger.log(Level.WARNING, "Failed to save FishDex preferences of " + player, error);
            return null;
        });
    }
}
