package com.cesarcosmico.fishdex.menu;

import com.cesarcosmico.fishdex.text.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IconRendererTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static boolean anyRed(Component component) {
        return hasColor(component, NamedTextColor.RED);
    }

    private static boolean hasColor(Component component, TextColor color) {
        List<Component> all = new ArrayList<>();
        collect(component, all);
        return all.stream().anyMatch(c -> color.equals(c.color()));
    }

    private static Component renderPlaceholder(String value) {
        IconRenderer renderer = new IconRenderer((viewer, placeholder) -> value);
        return renderer.render("%value%", null, TagResolver.empty(), IconRenderer.Accent.NONE);
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

    @Test
    void legacyCodesInPlaceholderOutputBecomeColours() {
        Component rendered = renderPlaceholder("&aGreen");

        assertEquals("Green", plain(rendered));
        assertTrue(hasColor(rendered, NamedTextColor.GREEN));
    }

    @Test
    void bothHexFormatsInPlaceholderOutputAreApplied() {
        TextColor green = TextColor.fromHexString("#00ff00");

        assertTrue(hasColor(renderPlaceholder("&#00ff00X"), green));
        assertTrue(hasColor(renderPlaceholder("§x§0§0§f§f§0§0X"), green));
        assertEquals("X", plain(renderPlaceholder("§x§0§0§f§f§0§0X")));
    }

    @Test
    void legacyColoursWorkButTagsStayLiteral() {
        Component rendered = renderPlaceholder("&a<red>evil");

        assertEquals("<red>evil", plain(rendered));
        assertTrue(hasColor(rendered, NamedTextColor.GREEN));
        assertFalse(anyRed(rendered));
    }

    @Test
    void eachPlaceholderReachesPlaceholderApiWholeAndOnce() {
        List<String> asked = new ArrayList<>();
        IconRenderer renderer = new IconRenderer((viewer, placeholder) -> {
            asked.add(placeholder);
            return "x";
        });

        renderer.render("%math_{player_level}*2% and %formatter_number_1 000%", null, TagResolver.empty(),
                IconRenderer.Accent.NONE);

        assertEquals(List.of("%math_{player_level}*2%", "%formatter_number_1 000%"), asked);
    }

    @Test
    void percentSignsAroundTagsAreNotPlaceholders() {
        List<String> asked = new ArrayList<>();
        IconRenderer renderer = new IconRenderer((viewer, placeholder) -> {
            asked.add(placeholder);
            return "x";
        });

        Component rendered = renderer.render("<gray><progress>%</gray> of 5%", null,
                TagResolver.resolver("progress", (arguments, context) ->
                        Tag.selfClosingInserting(Component.text("40"))),
                IconRenderer.Accent.NONE);

        assertTrue(asked.isEmpty());
        assertEquals("40% of 5%", plain(rendered));
    }
}
