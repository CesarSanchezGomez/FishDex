package com.cesarcosmico.fishdex.config;

import java.util.List;
import java.util.Map;

public record CommandSpec(boolean enabled, String name, List<String> aliases,
                          String permission, Map<String, String> subPermissions) {

    public String subPermission(String subcommand) {
        return subPermissions.get(subcommand);
    }
}
