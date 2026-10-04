package com.cesarcosmico.fishdex.integration.customfishing;

import net.momirealms.customfishing.api.event.CustomFishingReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Makes a {@code /customfishing reload} show in the FishDex without a separate {@code /fishdex reload}. */
public final class CustomFishingReloadListener implements Listener {

    private final CustomFishingEngine engine;

    public CustomFishingReloadListener(CustomFishingEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onReload(CustomFishingReloadEvent event) {
        engine.invalidateCaches();
    }
}
