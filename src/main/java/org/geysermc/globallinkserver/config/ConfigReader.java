/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.config;

import com.google.gson.Gson;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigReader {
    private static final Gson GSON = new Gson();
    private static Path CONFIG_PATH;

    public static Config readConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        var config = plugin.getConfig();
        plugin.saveConfig();
        return new Config(config.getString("hostname"),
                config.getString("username"),
                config.getString("password"),
                config.getString("database"));

//        CONFIG_PATH = path;
//        LOGGER.info("Reading config from " + CONFIG_PATH.toAbsolutePath());
//        String data = configContent();
//        if (data == null) {
//            createConfig();
//        }
//        data = configContent();
//
//        return GSON.fromJson(data, Config.class);
    }

    private static String configContent() {
        try {
            return Files.readString(CONFIG_PATH);
        } catch (IOException exception) {
            return null;
        }
    }

    private static void createConfig() {
        try {
            //noinspection DataFlowIssue
            Files.copy(ConfigReader.class.getResourceAsStream("/config.json"), CONFIG_PATH);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to copy config", exception);
        }
    }
}
