/*
 * Copyright (c) 2021-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.geysermc.globallinkserver.bedrock.BedrockServer;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.config.ConfigReader;
import org.geysermc.globallinkserver.java.JavaServer;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;

public class GlobalLinkServer {
    private static final Timer TIMER = new Timer();
    public static final Logger LOGGER = Logger.getGlobal();

    public static void main(String... args) {
        // Make logging more simple, adopted from https://stackoverflow.com/a/5937929
        System.setProperty(
                "java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %5$s%6$s%n");

        Config config = ConfigReader.readConfig();

        PlayerManager playerManager = new PlayerManager();
        LinkManager linkManager = new LinkManager(config);

        new JavaServer(playerManager, linkManager).startServer(config);
        new BedrockServer(playerManager, linkManager).startServer(config);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                linkManager.cleanupTempLinks(playerManager);
            }
        };
        TIMER.scheduleAtFixedRate(task, 0L, 60_000L);

        LOGGER.info(
                "Started Global Linking Server on java: " + config.javaPort() + ", bedrock: " + config.bedrockPort());
    }
}
