package com.cesarcosmico.fishdex.text;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalises CustomFishing loot names, which can mix legacy codes ({@code &r}, {@code §a}, hex) with
 * MiniMessage. Converting legacy → MiniMessage <em>before</em> parsing keeps malformed input away from
 * the parser (a crash source) and stops raw {@code &}/{@code §} codes leaking into the GUI.
 */
public final class NameFormatting {

    private static final Pattern SPIGOT_HEX = Pattern.compile("[&§]x((?:[&§][0-9a-fA-F]){6})");
    private static final Pattern SHORT_HEX = Pattern.compile("[&§]#([0-9a-fA-F]{6})");
    private static final Pattern CODE = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");

    private static final Map<Character, String> TAGS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset"));

    private NameFormatting() {
    }

    /**
     * Converts legacy {@code &}/{@code §} codes (incl. hex) to MiniMessage tags; existing tags are kept.
     * Whitespace is preserved, which lore lines rely on for indentation.
     */
    public static String legacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String out = replace(SPIGOT_HEX, input, m -> "<#" + m.group(1).replaceAll("[&§]", "") + ">");
        out = replace(SHORT_HEX, out, m -> "<#" + m.group(1) + ">");
        return replace(CODE, out, m -> "<" + TAGS.get(Character.toLowerCase(m.group(1).charAt(0))) + ">");
    }

    /** Like {@link #legacyToMiniMessage} but trimmed — for names, where surrounding spaces are noise. */
    public static String toMiniMessage(String input) {
        return legacyToMiniMessage(input).trim();
    }

    /** Plain visible text with all formatting (legacy or MiniMessage) removed. */
    public static String toPlain(String input) {
        String mini = toMiniMessage(input);
        try {
            return PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(mini)).trim();
        } catch (RuntimeException e) {
            return MiniMessage.miniMessage().stripTags(mini).trim();
        }
    }

    private static String replace(Pattern pattern, String input, java.util.function.Function<Matcher, String> replacer) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacer.apply(matcher)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
