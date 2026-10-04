package com.cesarcosmico.fishdex.menu;

import java.util.Locale;

/** What a menu button does, parsed from its {@code action} at load so typos fail there and not on click. */
public sealed interface MenuAction {

    record None() implements MenuAction {
    }

    record OpenRoot() implements MenuAction {
    }

    record Open(String menuId) implements MenuAction {
    }

    record PreviousPage() implements MenuAction {
    }

    record NextPage() implements MenuAction {
    }

    record Close() implements MenuAction {
    }

    record Sort() implements MenuAction {
    }

    /** Controls default to every page; everything else to the first one. */
    default boolean onEveryPageByDefault() {
        return !(this instanceof None) && !(this instanceof Open);
    }

    static MenuAction parse(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return new None();
        }
        if (value.equalsIgnoreCase("open:root")) {
            return new OpenRoot();
        }
        if (value.regionMatches(true, 0, "open:", 0, "open:".length())) {
            String menuId = value.substring("open:".length()).trim();
            if (menuId.isEmpty()) {
                throw new IllegalArgumentException("'open:' needs a menu id");
            }
            return new Open(menuId);
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "previous_page" -> new PreviousPage();
            case "next_page" -> new NextPage();
            case "close" -> new Close();
            case "sort" -> new Sort();
            default -> throw new IllegalArgumentException("unknown action '" + raw + "'");
        };
    }
}
