package com.cesarcosmico.fishdex.menu;

import com.cesarcosmico.fishdex.text.NameFormatting;
import com.cesarcosmico.fishdex.text.PlaceholderResolver;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns menu templates into components and items. A template goes through the menu accent
 * ({@code {colors}…{/colors}}), legacy-code normalisation and MiniMessage, with italics off. A lore line
 * that renders to no text is dropped and {@code <newline>} splits a line, so placeholders can hide or add
 * lines.
 */
public final class IconRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern EXTERNAL_PLACEHOLDER = Pattern.compile("%([A-Za-z0-9_.:-]+)%");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final PlaceholderResolver placeholders;

    public IconRenderer(PlaceholderResolver placeholders) {
        this.placeholders = placeholders;
    }

    /** One colour is solid, two or more a gradient; templates wrap text in {@code {colors}…{/colors}}. */
    public record Accent(String open, String close) {

        public static final Accent NONE = new Accent("", "");

        public static Accent of(List<String> colors) {
            if (colors == null || colors.isEmpty()) {
                return NONE;
            }
            if (colors.size() == 1) {
                return new Accent("<color:" + colors.get(0) + ">", "</color>");
            }
            return new Accent("<gradient:" + String.join(":", colors) + ">", "</gradient>");
        }

        public String apply(String template) {
            return template.replace("{colors}", open).replace("{/colors}", close);
        }
    }

    public ItemStack create(MenuIcon icon, Player viewer, TagResolver resolver, Accent accent) {
        ItemStack item = ItemStack.of(icon.material());
        for (Consumer<ItemStack> component : icon.components()) {
            component.accept(item);
        }
        if (!icon.name().isBlank()) {
            item.setData(DataComponentTypes.CUSTOM_NAME, render(icon.name(), viewer, resolver, accent));
        }
        if (!icon.lore().isEmpty()) {
            item.setData(DataComponentTypes.LORE, ItemLore.lore(renderLore(icon.lore(), viewer, resolver, accent)));
        }
        return item;
    }

    /** Re-labels a CustomFishing-built item, so its name can follow the menu accent instead of the fish's. */
    public void relabel(ItemStack item, String nameTemplate, List<String> lore, Player viewer,
                        TagResolver resolver, Accent accent) {
        if (nameTemplate != null && !nameTemplate.isBlank()) {
            item.setData(DataComponentTypes.CUSTOM_NAME, render(nameTemplate, viewer, resolver, accent));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(renderLore(lore, viewer, resolver, accent)));
    }

    public Component render(String template, Player viewer, TagResolver resolver, Accent accent) {
        List<String> external = new ArrayList<>();
        String prepared = withExternalTags(NameFormatting.legacyToMiniMessage(accent.apply(template)), external);
        TagResolver all = TagResolver.resolver(resolver, externalResolver(external, viewer));
        try {
            return MINI_MESSAGE.deserialize("<!italic>" + prepared, all);
        } catch (RuntimeException malformed) {
            return Component.text(MINI_MESSAGE.stripTags(prepared)).decoration(TextDecoration.ITALIC, false);
        }
    }

    /**
     * Swaps each {@code %placeholder%} for an indexed {@code <ext:n>} tag. The value is inserted as a finished
     * component, never parsed as MiniMessage, so a player-controlled placeholder cannot inject tags.
     */
    private static String withExternalTags(String text, List<String> external) {
        Matcher matcher = EXTERNAL_PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            external.add(matcher.group());
            matcher.appendReplacement(out, "<ext:" + (external.size() - 1) + ">");
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private TagResolver externalResolver(List<String> external, Player viewer) {
        if (external.isEmpty()) {
            return TagResolver.empty();
        }
        return TagResolver.resolver("ext", (arguments, context) -> {
            int index = arguments.popOr("missing placeholder index").asInt().orElse(-1);
            if (index < 0 || index >= external.size()) {
                return Tag.selfClosingInserting(Component.empty());
            }
            String placeholder = external.get(index);
            return Tag.selfClosingInserting(LEGACY.deserialize(placeholders.resolve(viewer, placeholder)));
        });
    }

    private List<Component> renderLore(List<String> lines, Player viewer, TagResolver resolver, Accent accent) {
        List<Component> lore = new ArrayList<>(lines.size());
        for (String line : lines) {
            for (Component part : splitLines(render(line, viewer, resolver, accent))) {
                if (!PlainTextComponentSerializer.plainText().serialize(part).isEmpty()) {
                    lore.add(part);
                }
            }
        }
        return lore;
    }

    private static List<Component> splitLines(Component component) {
        if (!PlainTextComponentSerializer.plainText().serialize(component).contains("\n")) {
            return List.of(component);
        }
        List<Component> lines = new ArrayList<>();
        lines.add(flatten(component, Style.empty(), lines, Component.text()).build());
        return lines;
    }

    /** Copies {@code node} into {@code line} with its inherited style, starting a new line at each {@code \n}. */
    private static TextComponent.Builder flatten(Component node, Style inherited,
                                                 List<Component> lines, TextComponent.Builder line) {
        Style style = inherited.merge(node.style());
        if (node instanceof TextComponent text) {
            String[] parts = text.content().split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    lines.add(line.build());
                    line = Component.text();
                }
                if (!parts[i].isEmpty()) {
                    line.append(Component.text(parts[i]).style(style));
                }
            }
        } else {
            line.append(node.children(List.of()).style(style));
        }
        for (Component child : node.children()) {
            line = flatten(child, style, lines, line);
        }
        return line;
    }
}
