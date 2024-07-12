/*
 * Copyright (c) 2021-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.config;

import static org.geysermc.globallinkserver.GlobalLinkServer.LOGGER;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigReader {
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Paths.get("config.json");

    public static Config readConfig() {
        LOGGER.info("Reading config from " + CONFIG_PATH.toAbsolutePath());
        String data = configContent();
        if (data == null) {
            createConfig();
        }
        data = configContent();

        return GSON.fromJson(data, Config.class);
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
            Files.copy(ConfigReader.class.getResourceAsStream("/config.json"), CONFIG_PATH);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to copy config", exception);
        }
    }
}
