/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.config;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConfigReader {
    public static Config readConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        var config = plugin.getConfig();

        var databaseSection = Objects.requireNonNull(config.getConfigurationSection("database"));

        var database = new Config.Database(
                databaseSection.getString("hostname"),
                databaseSection.getString("username"),
                databaseSection.getString("password"),
                databaseSection.getString("database"),
                databaseSection.getInt("max-pool-size"));

        var locationSection = Objects.requireNonNull(config.getConfigurationSection("spawn"));
        var spawnLocation = Location.deserialize(locationSection.getValues(false));

        if (!spawnLocation.isWorldLoaded()) {
            throw new IllegalArgumentException("World %s is not loaded".formatted(locationSection.getString("world")));
        }

        return new Config(database, spawnLocation);
    }
}
