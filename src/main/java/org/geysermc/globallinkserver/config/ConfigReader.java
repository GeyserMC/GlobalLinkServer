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

        var database = config.getConfigurationSection("database");
        var util = config.getConfigurationSection("util");

        return new Config(new Config.Database(
                database.getString("hostname"),
                database.getString("username"),
                database.getString("password"),
                database.getString("database")
        ), new Config.Util(
                util.getBoolean("hide-join-leave-messages"),
                util.getBoolean("hide-death-messages"),
                util.getBoolean("hide-players"),
                util.getBoolean("disable-chat"),
                util.getBoolean("void-teleport"),
                util.getBoolean("prevent-hunger"),
                util.getBoolean("respawn-on-join")
        ));
    }
}
