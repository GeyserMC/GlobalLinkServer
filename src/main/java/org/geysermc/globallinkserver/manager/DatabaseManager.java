/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.geysermc.globallinkserver.config.Config;
import org.jspecify.annotations.NullMarked;
import org.mariadb.jdbc.MariaDbPoolDataSource;

@NullMarked
public final class DatabaseManager {
    private final MariaDbPoolDataSource dataSource;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public DatabaseManager(Config config) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");

            var hostname = config.database().hostname();
            String serverName;
            int port = 3306;

            var hostnameSplit = hostname.split(":");
            if (hostnameSplit.length > 1) {
                serverName = hostnameSplit[0];
                port = Integer.parseInt(hostnameSplit[1]);
            } else {
                serverName = hostname;
            }

            dataSource = new MariaDbPoolDataSource();
            dataSource.setServerName(serverName);
            dataSource.setPort(port);
            dataSource.setDatabaseName(config.database().database());
            dataSource.setUser(config.database().username());
            dataSource.setPassword(config.database().password());
            dataSource.setMinPoolSize(1);
            dataSource.setMaxPoolSize(config.database().maxPoolSize());

        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("Cannot find required class to load the MariaDB database");
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to set the datasource connection fields", exception);
        }

        connectionCheck();
    }

    private void connectionCheck() {
        try (var connection = connection(); var statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery("SELECT 1")) {
                resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not connect to database!", exception);
        }
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    public ExecutorService executor() {
        return executorService;
    }
}
