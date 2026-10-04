package com.cesarcosmico.fishdex.storage;

import com.cesarcosmico.fishdex.config.DatabaseSettings;
import com.cesarcosmico.fishdex.config.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Connection pool, schema and the single thread every query runs on. One worker serialises writes, so
 * read-then-write upserts stay consistent without locking; callers hop back to the main thread themselves.
 */
public final class Database implements AutoCloseable {

    private static final int SCHEMA_VERSION = 1;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final HikariDataSource dataSource;
    private final DatabaseType type;
    private final Executor executor;
    private final ExecutorService ownedExecutor;

    private Database(HikariDataSource dataSource, DatabaseType type, Executor executor, ExecutorService ownedExecutor) {
        this.dataSource = dataSource;
        this.type = type;
        this.executor = executor;
        this.ownedExecutor = ownedExecutor;
    }

    public static Database open(DatabaseSettings settings, File dataFolder) throws SQLException {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "fishdex-db");
            thread.setDaemon(true);
            return thread;
        });
        return create(settings, dataFolder, executor, executor);
    }

    /** Runs every query on {@code executor} and never shuts it down; meant for tests ({@code Runnable::run}). */
    public static Database openWith(DatabaseSettings settings, File dataFolder, Executor executor) throws SQLException {
        return create(settings, dataFolder, executor, null);
    }

    private static Database create(DatabaseSettings settings, File dataFolder, Executor executor,
                                   ExecutorService ownedExecutor) throws SQLException {
        HikariDataSource dataSource = new HikariDataSource(poolConfig(settings, dataFolder));
        Database database = new Database(dataSource, settings.type(), executor, ownedExecutor);
        try {
            database.applySchema();
        } catch (SQLException | RuntimeException e) {
            database.close();
            throw e;
        }
        return database;
    }

    private static HikariConfig poolConfig(DatabaseSettings settings, File dataFolder) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("fishdex-pool");
        config.setConnectionTimeout(10_000);
        if (settings.type() == DatabaseType.SQLITE) {
            File file = new File(dataFolder, settings.sqliteFile());
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            // The driver ships with the server; the plugin loader only downloads the MySQL/MariaDB ones.
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            config.setMaximumPoolSize(1);
            config.setConnectionInitSql("PRAGMA busy_timeout = 5000");
            return config;
        }
        boolean mariadb = settings.type() == DatabaseType.MARIADB;
        DatabaseSettings.Connection c = mariadb ? settings.mariadb() : settings.mysql();
        String properties = c.properties() == null ? "" : c.properties().trim();
        config.setDriverClassName(mariadb ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:" + (mariadb ? "mariadb" : "mysql") + "://" + c.host() + ":" + c.port() + "/"
                + c.database() + (properties.isEmpty() ? "" : "?" + properties));
        config.setUsername(c.username());
        config.setPassword(c.password());
        config.setMaximumPoolSize(Math.max(1, c.poolSize()));
        return config;
    }

    public DatabaseType type() {
        return type;
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    public <T> CompletableFuture<T> query(SqlFunction<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return work.apply(connection);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> execute(SqlConsumer work) {
        return query(connection -> {
            work.accept(connection);
            return null;
        });
    }

    private void applySchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schemaStatements()) {
                statement.execute(sql);
            }
            statement.execute("INSERT INTO schema_version (version) VALUES (" + SCHEMA_VERSION + ")"
                    + (type == DatabaseType.SQLITE
                        ? " ON CONFLICT(version) DO NOTHING"
                        : " ON DUPLICATE KEY UPDATE version = version"));
        }
    }

    private List<String> schemaStatements() {
        String resource = type == DatabaseType.SQLITE ? "db/sqlite.sql" : "db/mysql.sql";
        InputStream in = Database.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Missing schema resource: " + resource);
        }
        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading schema resource: " + resource, e);
        }
        List<String> statements = new ArrayList<>();
        for (String statement : sql.toString().split(";")) {
            if (!statement.isBlank()) {
                statements.add(statement.trim());
            }
        }
        return statements;
    }

    /** Waits for queued writes, then closes the pool. */
    @Override
    public void close() {
        if (ownedExecutor != null) {
            ownedExecutor.shutdown();
            try {
                if (!ownedExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    ownedExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                ownedExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
