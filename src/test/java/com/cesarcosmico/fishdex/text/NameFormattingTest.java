package com.cesarcosmico.fishdex.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameFormattingTest {

    @Test
    void convertsSingleLegacyCodes() {
        assertEquals("<red>Foo", NameFormatting.toMiniMessage("&cFoo"));
        assertEquals("<green>Bar", NameFormatting.toMiniMessage("§aBar"));
    }

    @Test
    void convertsResetSoItDoesNotLeak() {
        // A real CustomFishing name that used to leak raw codes into the GUI.
        assertEquals("<reset> Tiburón confeti<reset>", NameFormatting.toMiniMessage("&r Tiburón confeti&r"));
    }

    @Test
    void convertsHexForms() {
        assertEquals("<#ff0000>Red", NameFormatting.toMiniMessage("&x&f&f&0&0&0&0Red"));
        assertEquals("<#00ff00>Green", NameFormatting.toMiniMessage("&#00ff00Green"));
    }

    @Test
    void keepsExistingMiniMessageTags() {
        assertEquals("<gradient:#fff:#000><bold>Hi</bold></gradient>",
                NameFormatting.toMiniMessage("<gradient:#fff:#000><bold>Hi</bold></gradient>"));
    }

    @Test
    void handlesMixedLegacyAndMiniMessage() {
        assertEquals("<reset><gradient:#fff:#000>Hi</gradient>",
                NameFormatting.toMiniMessage("&r<gradient:#fff:#000>Hi</gradient>"));
    }

    @Test
    void emptyOrNullBecomeEmpty() {
        assertEquals("", NameFormatting.toMiniMessage(""));
        assertEquals("", NameFormatting.toMiniMessage(null));
    }

    @Test
    void legacyToMiniMessagePreservesSurroundingSpaces() {
        // Lore/PlaceholderAPI output keeps its indentation; only toMiniMessage (names) trims.
        assertEquals("  <green>Hi  ", NameFormatting.legacyToMiniMessage("  &aHi  "));
        assertEquals("<green>Hi", NameFormatting.toMiniMessage("  &aHi  "));
    }

    @Test
    void plainTextDropsAllFormatting() {
        assertEquals("Shark", NameFormatting.toPlain("&r<gradient:#fff:#000>&lShark</gradient>"));
    }
}
