package com.cesarcosmico.fishdex.command;

import com.cesarcosmico.fishdex.config.CommandSpec;
import com.cesarcosmico.fishdex.menu.MenuNavigator;
import com.cesarcosmico.fishdex.service.FishingEngine;
import com.cesarcosmico.fishdex.text.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code /fishdex [menu]} plus the admin subcommands {@code reload} and {@code version}. Brigadier tries
 * literals before arguments, so {@code reload} and {@code version} cannot be used as menu ids.
 */
public final class FishDexCommand implements PluginCommand {

    private static final String RELOAD = "reload";
    private static final String VERSION = "version";

    @FunctionalInterface
    public interface ReloadAction {
        void reload() throws Exception;
    }

    private final MenuNavigator navigator;
    private final Supplier<Messages> messages;
    private final FishingEngine engine;
    private final String pluginVersion;
    private final ReloadAction reloadAction;
    private final Logger logger;

    public FishDexCommand(MenuNavigator navigator, Supplier<Messages> messages, FishingEngine engine,
                          String pluginVersion, ReloadAction reloadAction, Logger logger) {
        this.navigator = navigator;
        this.messages = messages;
        this.engine = engine;
        this.pluginVersion = pluginVersion;
        this.reloadAction = reloadAction;
        this.logger = logger;
    }

    @Override
    public String id() {
        return "fishdex";
    }

    @Override
    public String description() {
        return "Open the FishDex";
    }

    @Override
    public CommandSpec defaults() {
        return new CommandSpec(true, "fishdex", List.of(), "fishdex.command.open", Map.of(
                VERSION, "fishdex.command.version",
                RELOAD, "fishdex.command.reload"));
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> build(CommandSpec spec) {
        return Commands.literal(spec.name())
                .requires(source -> canOpen(source, spec)
                        || allowed(source.getSender(), spec.subPermission(RELOAD))
                        || allowed(source.getSender(), spec.subPermission(VERSION)))
                .executes(context -> openRoot(context, spec))
                .then(Commands.literal(RELOAD)
                        .requires(source -> allowed(source.getSender(), spec.subPermission(RELOAD)))
                        .executes(this::reload))
                .then(Commands.literal(VERSION)
                        .requires(source -> allowed(source.getSender(), spec.subPermission(VERSION)))
                        .executes(this::version))
                .then(Commands.argument("menu", StringArgumentType.word())
                        .requires(source -> canOpen(source, spec))
                        .suggests(this::suggestMenus)
                        .executes(this::openMenu))
                .build();
    }

    private static boolean canOpen(CommandSourceStack source, CommandSpec spec) {
        return source.getSender() instanceof Player && allowed(source.getSender(), spec.permission());
    }

    private static boolean allowed(CommandSender sender, String permission) {
        return permission != null && sender.hasPermission(permission);
    }

    private int openRoot(CommandContext<CommandSourceStack> context, CommandSpec spec) {
        if (canOpen(context.getSource(), spec)) {
            navigator.open((Player) context.getSource().getSender(), navigator.rootMenuId());
        } else {
            context.getSource().getSender().sendMessage(messages.get().get("command.usage"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int openMenu(CommandContext<CommandSourceStack> context) {
        navigator.open((Player) context.getSource().getSender(), StringArgumentType.getString(context, "menu"));
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            reloadAction.reload();
            sender.sendMessage(messages.get().get("command.reloaded"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "FishDex reload failed; the previous configuration is still active.", e);
            sender.sendMessage(messages.get().get("command.reload-failed",
                    Placeholder.unparsed("error", String.valueOf(e.getMessage()))));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int version(CommandContext<CommandSourceStack> context) {
        context.getSource().getSender().sendMessage(messages.get().get("command.version",
                Placeholder.unparsed("version", pluginVersion),
                Placeholder.unparsed("engine", engine.engineVersion())));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestMenus(CommandContext<CommandSourceStack> context,
                                                        SuggestionsBuilder builder) {
        String prefix = builder.getRemainingLowerCase();
        for (String id : navigator.menuIds()) {
            if (id.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    }
}
