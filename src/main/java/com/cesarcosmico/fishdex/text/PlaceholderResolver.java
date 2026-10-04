package com.cesarcosmico.fishdex.text;

import org.bukkit.entity.Player;

/** Resolves one external placeholder such as {@code %player_name%}; {@link #NONE} keeps it as literal text. */
@FunctionalInterface
public interface PlaceholderResolver {

    PlaceholderResolver NONE = (viewer, placeholder) -> placeholder;

    String resolve(Player viewer, String placeholder);
}
