/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.config.ConfigReader;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.util.CommandUtils;

public class GlobalLinkServer extends JavaPlugin {
    private static final Timer TIMER = new Timer();
    public static Logger LOGGER;

    private LinkManager linkManager;

    @Override
    public void onEnable() {
        LOGGER = getLogger();

        Config config = ConfigReader.readConfig();
        linkManager = new LinkManager(config);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, linkManager::cleanupTempLinks, 0, 0);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                linkManager.cleanupTempLinks();
            }
        };
        TIMER.scheduleAtFixedRate(task, 0L, 60_000L);

        LOGGER.info("Started Global Linking Server plugin!");
    }

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        if (sender instanceof Player player) {
            CommandUtils.handleCommand(linkManager, player, command.getName() + " " + String.join(" ", args));
        }
        return true;
    }
}
