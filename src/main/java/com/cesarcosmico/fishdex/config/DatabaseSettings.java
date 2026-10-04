package com.cesarcosmico.fishdex.config;

import org.bukkit.configuration.ConfigurationSection;

public record DatabaseSettings(DatabaseType type, String sqliteFile, Connection mysql, Connection mariadb) {

    public record Connection(String host, int port, String database, String username, String password,
                             int poolSize, String properties) {

        static Connection defaults() {
            return new Connection("localhost", 3306, "fishdex", "root", "", 10, "");
        }

        static Connection from(ConfigurationSection s) {
            if (s == null) {
                return defaults();
            }
            return new Connection(
                    s.getString("host", "localhost"),
                    s.getInt("port", 3306),
                    s.getString("database", "fishdex"),
                    s.getString("username", "root"),
                    s.getString("password", ""),
                    s.getInt("pool-size", 10),
                    s.getString("properties", ""));
        }
    }

    public static DatabaseSettings sqlite(String file) {
        return new DatabaseSettings(DatabaseType.SQLITE, file, Connection.defaults(), Connection.defaults());
    }

    public static DatabaseSettings from(ConfigurationSection root) {
        DatabaseType type = DatabaseType.parse(root == null ? "sqlite" : root.getString("type", "sqlite"));
        ConfigurationSection sqlite = root == null ? null : root.getConfigurationSection("sqlite");
        String sqliteFile = sqlite == null ? "data.db" : sqlite.getString("file", "data.db");
        Connection mysql = Connection.from(root == null ? null : root.getConfigurationSection("mysql"));
        Connection mariadb = Connection.from(root == null ? null : root.getConfigurationSection("mariadb"));
        return new DatabaseSettings(type, sqliteFile, mysql, mariadb);
    }
}
