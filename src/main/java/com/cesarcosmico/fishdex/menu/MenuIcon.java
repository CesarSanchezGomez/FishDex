package com.cesarcosmico.fishdex.menu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** A {@code material} plus a {@code components:} block; name and lore stay as MiniMessage templates. */
public record MenuIcon(Material material, String name, List<String> lore, List<Consumer<ItemStack>> components) {

    public MenuIcon {
        lore = List.copyOf(lore);
        components = List.copyOf(components);
    }

    public static MenuIcon from(ConfigurationSection section, Material fallback, Consumer<String> warn) {
        if (section == null) {
            return new MenuIcon(fallback, "", List.of(), List.of());
        }
        Material material = material(section.getString("material"), fallback, warn);
        ConfigurationSection components = section.getConfigurationSection("components");
        String name = components == null ? "" : components.getString(ItemComponents.CUSTOM_NAME, "");
        List<String> lore = components == null ? List.of() : components.getStringList(ItemComponents.LORE);
        return new MenuIcon(material, name, lore, ItemComponents.compile(components, warn));
    }

    private static Material material(String raw, Material fallback, Consumer<String> warn) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            warn.accept("unknown material '" + raw + "', using " + fallback);
            return fallback;
        }
        return material;
    }
}
