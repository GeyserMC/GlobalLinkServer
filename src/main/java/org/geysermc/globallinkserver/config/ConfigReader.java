/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.config;

import org.bukkit.plugin.java.JavaPlugin;

public class ConfigReader {

    public static Config readConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        var config = plugin.getConfig();
        plugin.saveConfig();

        return new Config(config.getString("hostname"),
                config.getString("username"),
                config.getString("password"),
                config.getString("database"));
    }
}
