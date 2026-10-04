package com.cesarcosmico.fishdex.menu;

import com.cesarcosmico.fishdex.text.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IconRendererTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static boolean anyRed(Component component) {
        List<Component> all = new ArrayList<>();
        collect(component, all);
        return all.stream().anyMatch(c -> NamedTextColor.RED.equals(c.color()));
    }

    private static void collect(Component component, List<Component> out) {
        out.add(component);
        component.children().forEach(child -> collect(child, out));
    }

    @Test
    void placeholderOutputIsNeverParsedAsMiniMessage() {
        PlaceholderResolver hostile = (viewer, placeholder) -> "<red><click:run_command:'/op me'>Steve";
        IconRenderer renderer = new IconRenderer(hostile);

        Component rendered = renderer.render("Hi %player_name%!", null, TagResolver.empty(), IconRenderer.Accent.NONE);

        assertEquals("Hi <red><click:run_command:'/op me'>Steve!", plain(rendered));
        assertFalse(anyRed(rendered));
    }

    @Test
    void withoutPlaceholderApiThePlaceholderStaysLiteral() {
        IconRenderer renderer = new IconRenderer(PlaceholderResolver.NONE);

        Component rendered = renderer.render("<gray>%player_name%</gray>", null, TagResolver.empty(), IconRenderer.Accent.NONE);

        assertEquals("%player_name%", plain(rendered));
    }

    @Test
    void accentWrapsTheMarkedText() {
        IconRenderer renderer = new IconRenderer(PlaceholderResolver.NONE);

        Component rendered = renderer.render("{colors}FishDex{/colors}", null, TagResolver.empty(),
                IconRenderer.Accent.of(List.of("#FF0000")));

        assertEquals("FishDex", plain(rendered));
    }
}
