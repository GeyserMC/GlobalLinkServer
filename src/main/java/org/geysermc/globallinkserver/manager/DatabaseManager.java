/*
 * Copyright (c) 2025-2026 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.geysermc.globallinkserver.config.Config;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class DatabaseManager {
    private final DataSource dataSource;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public DatabaseManager(Config config) {
        var hostname = config.database().hostname();
        String serverName;
        Integer port = null;

        var hostnameSplit = hostname.split(":");
        if (hostnameSplit.length > 1) {
            serverName = hostnameSplit[0];
            port = Integer.parseInt(hostnameSplit[1]);
        } else {
            serverName = hostname;
        }

        dataSource = new HikariDataSource(hikariConfigFor(config, serverName, port));
    }

    private static HikariConfig hikariConfigFor(Config config, String serverName, @Nullable Integer port) {
        HikariConfig hikari = new HikariConfig();
        hikari.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikari.addDataSourceProperty("serverName", serverName);
        if (port != null) {
            hikari.addDataSourceProperty("portNumber", port);
        }
        hikari.addDataSourceProperty("user", config.database().username());
        hikari.addDataSourceProperty("password", config.database().password());
        hikari.addDataSourceProperty("databaseName", config.database().database());
        hikari.setMaximumPoolSize(config.database().maxPoolSize());
        return hikari;
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    public ExecutorService executor() {
        return executorService;
    }
}
