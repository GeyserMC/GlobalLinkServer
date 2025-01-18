/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.handler;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.geysermc.globallinkserver.Components;
import org.geysermc.globallinkserver.service.LinkLookupService;
import org.geysermc.globallinkserver.util.MultiConditionSet;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class JoinHandler implements Listener {
    private final LinkLookupService linkLookupService;
    private final MultiConditionSet<UUID> playerIdleTracker;
    private final Plugin plugin;

    public JoinHandler(LinkLookupService linkLookupService, MultiConditionSet<UUID> playerIdleTracker, Plugin plugin) {
        this.linkLookupService = linkLookupService;
        this.playerIdleTracker = playerIdleTracker;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLoad(PlayerJoinEvent event) {
        var player = event.getPlayer();
        event.joinMessage(null);

        player.setPersistent(false);
        player.setAllowFlight(true);

        // Hide all players from each other
        Bukkit.getOnlinePlayers().forEach(otherPlayer -> {
            player.hidePlayer(plugin, otherPlayer);
            otherPlayer.hidePlayer(plugin, player);
        });

        playerIdleTracker.add(player.getUniqueId());

        linkLookupService.lookup(player).whenComplete(($, throwable) -> {
            if (throwable != null) {
                player.sendMessage(Components.INFO_UNAVAILABLE);
                throwable.printStackTrace();
            }
        });
    }
}
