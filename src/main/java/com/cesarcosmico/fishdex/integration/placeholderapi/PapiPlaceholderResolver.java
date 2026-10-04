package com.cesarcosmico.fishdex.integration.placeholderapi;

import com.cesarcosmico.fishdex.text.PlaceholderResolver;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** Only created when PlaceholderAPI is enabled, so its classes are never linked otherwise. */
public final class PapiPlaceholderResolver implements PlaceholderResolver {

    @Override
    public String resolve(Player viewer, String placeholder) {
        return viewer == null ? placeholder : PlaceholderAPI.setPlaceholders(viewer, placeholder);
    }
}
