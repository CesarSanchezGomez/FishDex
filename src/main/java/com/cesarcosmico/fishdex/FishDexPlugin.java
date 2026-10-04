package com.cesarcosmico.fishdex;

import com.cesarcosmico.fishdex.command.CommandRegistrar;
import com.cesarcosmico.fishdex.command.FishDexCommand;
import com.cesarcosmico.fishdex.config.CommandsConfig;
import com.cesarcosmico.fishdex.config.ConfigHolder;
import com.cesarcosmico.fishdex.config.ConfigValidator;
import com.cesarcosmico.fishdex.config.DatabaseSettings;
import com.cesarcosmico.fishdex.integration.customfishing.CatchListener;
import com.cesarcosmico.fishdex.integration.customfishing.CustomFishingEngine;
import com.cesarcosmico.fishdex.integration.customfishing.CustomFishingReloadListener;
import com.cesarcosmico.fishdex.integration.customfishing.MarketSellListener;
import com.cesarcosmico.fishdex.integration.placeholderapi.PapiPlaceholderResolver;
import com.cesarcosmico.fishdex.listener.MenuListener;
import com.cesarcosmico.fishdex.listener.PlayerSessionListener;
import com.cesarcosmico.fishdex.menu.IconRenderer;
import com.cesarcosmico.fishdex.menu.MenuNavigator;
import com.cesarcosmico.fishdex.menu.MenuRenderer;
import com.cesarcosmico.fishdex.menu.MenusConfig;
import com.cesarcosmico.fishdex.service.CatchService;
import com.cesarcosmico.fishdex.service.FishDexOrdering;
import com.cesarcosmico.fishdex.service.FishDexService;
import com.cesarcosmico.fishdex.service.PreferenceService;
import com.cesarcosmico.fishdex.service.SaleService;
import com.cesarcosmico.fishdex.storage.Database;
import com.cesarcosmico.fishdex.storage.DiscoveryRepository;
import com.cesarcosmico.fishdex.storage.PlayerStatsRepository;
import com.cesarcosmico.fishdex.storage.PreferenceRepository;
import com.cesarcosmico.fishdex.storage.ServerRecordRepository;
import com.cesarcosmico.fishdex.storage.SpeciesSalesRepository;
import com.cesarcosmico.fishdex.text.Messages;
import com.cesarcosmico.fishdex.text.NameFormatting;
import com.cesarcosmico.fishdex.text.PlaceholderResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public final class FishDexPlugin extends JavaPlugin {

    private Database database;
    private CustomFishingEngine engine;
    private ConfigHolder<Messages> messages;
    private ConfigHolder<MenusConfig> menus;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            ConfigValidator.check(this, getConfig(), "config.yml");
            messages = new ConfigHolder<>(Messages.load(this, language()));
            menus = new ConfigHolder<>(MenusConfig.load(this));
            database = Database.open(databaseSettings(), getDataFolder());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "FishDex could not start: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlayerStatsRepository playerStats = new PlayerStatsRepository(database);
        ServerRecordRepository serverRecords = new ServerRecordRepository(database);
        DiscoveryRepository discoveries = new DiscoveryRepository(database);
        SpeciesSalesRepository sales = new SpeciesSalesRepository(database);

        engine = new CustomFishingEngine(getLogger());
        if (engine.isAvailable()) {
            register(new CatchListener(new CatchService(playerStats, serverRecords, discoveries, getLogger())),
                    new MarketSellListener(new SaleService(sales, getLogger())),
                    new CustomFishingReloadListener(engine));
        } else {
            getLogger().warning("CustomFishing is not ready; catches and sales are not being recorded.");
        }

        PreferenceService preferences = new PreferenceService(new PreferenceRepository(database), getLogger());
        for (Player online : getServer().getOnlinePlayers()) {
            preferences.load(online.getUniqueId());
        }

        FishDexOrdering ordering = new FishDexOrdering(NameFormatting::toPlain);
        MenuRenderer renderer = new MenuRenderer(new IconRenderer(placeholderResolver()), engine, ordering);
        MenuNavigator navigator = new MenuNavigator(this, menus, messages,
                new FishDexService(engine, playerStats, serverRecords, discoveries, sales),
                preferences, ordering, renderer);
        register(new MenuListener(navigator), new PlayerSessionListener(preferences));

        CommandsConfig commands = CommandsConfig.load(this);
        new CommandRegistrar(this, commands, List.of(new FishDexCommand(navigator, messages, engine,
                getPluginMeta().getVersion(), this::reload, getLogger()))).register();
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    /** Loads everything first and swaps only when all of it parsed, so a broken file keeps the old state. */
    private void reload() throws Exception {
        reloadConfig();
        ConfigValidator.check(this, getConfig(), "config.yml");
        Messages newMessages = Messages.load(this, language());
        MenusConfig newMenus = MenusConfig.load(this);
        messages.set(newMessages);
        menus.set(newMenus);
        engine.invalidateCaches();
    }

    private String language() {
        return getConfig().getString("language", "en_US");
    }

    private DatabaseSettings databaseSettings() {
        File file = new File(getDataFolder(), "database.yml");
        if (!file.exists()) {
            saveResource("database.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigValidator.check(this, yaml, "database.yml");
        return DatabaseSettings.from(yaml);
    }

    private PlaceholderResolver placeholderResolver() {
        return getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")
                ? new PapiPlaceholderResolver()
                : PlaceholderResolver.NONE;
    }

    private void register(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
