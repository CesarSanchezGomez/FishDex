package com.cesarcosmico.fishdex.config;

import java.util.Locale;

/** MySQL and MariaDB share the schema and SQL dialect; each one still uses its own JDBC driver. */
public enum DatabaseType {
    SQLITE,
    MYSQL,
    MARIADB;

    /** Rejects unknown values instead of falling back, so a typo never silently switches to SQLite. */
    public static DatabaseType parse(String raw) {
        String value = raw == null ? "sqlite" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "sqlite" -> SQLITE;
            case "mysql" -> MYSQL;
            case "mariadb" -> MARIADB;
            default -> throw new IllegalArgumentException(
                    "Unknown database type '" + raw + "' (expected sqlite, mysql or mariadb)");
        };
    }
}
