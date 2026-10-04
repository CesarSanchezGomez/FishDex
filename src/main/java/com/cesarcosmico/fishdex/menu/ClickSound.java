package com.cesarcosmico.fishdex.menu;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * A sound played when a button is clicked. Read from config as either a key string
 * ({@code "minecraft:ui.button.click"}) or an object ({@code { key, volume, pitch, source }}).
 * Namespaced keys let resource-pack sounds work; an absent or malformed key means no sound.
 */
public record ClickSound(Key key, Sound.Source source, float volume, float pitch) {

    /** The {@code path} entry of {@code parent} as a click sound, or {@code null} if absent/invalid. */
    public static ClickSound read(ConfigurationSection parent, String path) {
        if (parent == null) {
            return null;
        }
        if (parent.isConfigurationSection(path)) {
            ConfigurationSection s = parent.getConfigurationSection(path);
            return of(s.getString("key", s.getString("sound")),
                    parseSource(s.getString("source")),
                    (float) s.getDouble("volume", 1.0),
                    (float) s.getDouble("pitch", 1.0));
        }
        return of(parent.getString(path), Sound.Source.MASTER, 1f, 1f);
    }

    private static ClickSound of(String raw, Sound.Source source, float volume, float pitch) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new ClickSound(Key.key(normalizeKey(raw.trim())), source, volume, pitch);
        } catch (RuntimeException invalidKey) {
            return null;
        }
    }

    /** Also accept the Bukkit Sound enum form (e.g. {@code AMBIENT_UNDERWATER_ENTER}) as a convenience. */
    private static String normalizeKey(String raw) {
        boolean enumForm = raw.indexOf(':') < 0 && raw.chars().anyMatch(Character::isUpperCase);
        return enumForm ? raw.toLowerCase(Locale.ROOT).replace('_', '.') : raw;
    }

    private static Sound.Source parseSource(String raw) {
        if (raw == null || raw.isBlank()) {
            return Sound.Source.MASTER;
        }
        try {
            return Sound.Source.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return Sound.Source.MASTER;
        }
    }

    public Sound toAdventure() {
        return Sound.sound(key, source, volume, pitch);
    }
}
