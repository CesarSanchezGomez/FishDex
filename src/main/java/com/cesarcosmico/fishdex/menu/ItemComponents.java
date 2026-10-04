package com.cesarcosmico.fishdex.menu;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** The {@code components:} keys a menu icon supports, compiled once at load into item edits. */
public final class ItemComponents {

    public static final String CUSTOM_NAME = "custom-name";
    public static final String LORE = "lore";

    private ItemComponents() {
    }

    /** Unknown keys and invalid values are reported through {@code warn} and skipped. */
    public static List<Consumer<ItemStack>> compile(ConfigurationSection components, Consumer<String> warn) {
        List<Consumer<ItemStack>> edits = new ArrayList<>();
        if (components == null) {
            return edits;
        }
        for (String key : components.getKeys(false)) {
            Object value = components.get(key);
            try {
                switch (key) {
                    case CUSTOM_NAME, LORE -> {
                        // Text is rendered per viewer by IconRenderer.
                    }
                    case "item-model" -> {
                        String raw = String.valueOf(value).trim();
                        if (!raw.isEmpty()) {
                            Key model = Key.key(raw);
                            edits.add(item -> item.setData(DataComponentTypes.ITEM_MODEL, model));
                        }
                    }
                    case "custom-model-data" -> {
                        CustomModelData data = customModelData(value);
                        edits.add(item -> item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, data));
                    }
                    case "profile" -> {
                        ResolvableProfile profile = profile(String.valueOf(value));
                        edits.add(item -> item.setData(DataComponentTypes.PROFILE, profile));
                    }
                    case "hide-tooltip" -> {
                        if (Boolean.parseBoolean(String.valueOf(value))) {
                            TooltipDisplay hidden = TooltipDisplay.tooltipDisplay().hideTooltip(true).build();
                            edits.add(item -> item.setData(DataComponentTypes.TOOLTIP_DISPLAY, hidden));
                        }
                    }
                    default -> warn.accept("unknown component '" + key + "'");
                }
            } catch (RuntimeException invalid) {
                warn.accept("invalid value for component '" + key + "': " + invalid.getMessage());
            }
        }
        return List.copyOf(edits);
    }

    /** A number keeps the old single-model-id meaning: it becomes the first float, as in vanilla. */
    private static CustomModelData customModelData(Object value) {
        CustomModelData.Builder builder = CustomModelData.customModelData();
        if (value instanceof Number number) {
            return builder.addFloat(number.floatValue()).build();
        }
        if (value instanceof List<?> list) {
            for (Object element : list) {
                if (!(element instanceof Number number)) {
                    throw new IllegalArgumentException("expected numbers, got '" + element + "'");
                }
                builder.addFloat(number.floatValue());
            }
            return builder.build();
        }
        throw new IllegalArgumentException("expected a number or a list of numbers");
    }

    private static ResolvableProfile profile(String texture) {
        return ResolvableProfile.resolvableProfile()
                .uuid(profileId(texture))
                .addProperty(new ProfileProperty("textures", texture))
                .build();
    }

    /** Same texture, same UUID: the client caches skins per profile id, so a random one re-downloads it. */
    static UUID profileId(String texture) {
        return UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
    }
}
