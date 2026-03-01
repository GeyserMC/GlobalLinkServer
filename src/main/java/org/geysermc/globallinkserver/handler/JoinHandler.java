/*
 * Copyright (c) 2025-2026 GeyserMC
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
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.globallinkserver.Components;
import org.geysermc.globallinkserver.manager.PlayerManager;
import org.geysermc.globallinkserver.service.LinkLookupService;
import org.geysermc.globallinkserver.service.MappingService;
import org.geysermc.globallinkserver.util.MultiConditionSet;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class JoinHandler implements Listener {
    private final PlayerManager playerManager;
    private final LinkLookupService linkLookupService;
    private final MappingService mappingService;
    private final MultiConditionSet<UUID> playerIdleTracker;
    private final Plugin plugin;

    public JoinHandler(
        PlayerManager playerManager,
        LinkLookupService linkLookupService,
        MappingService mappingService,
        MultiConditionSet<UUID> playerIdleTracker,
        Plugin plugin
    ) {
        this.playerManager = playerManager;
        this.linkLookupService = linkLookupService;
        this.mappingService = mappingService;
        this.playerIdleTracker = playerIdleTracker;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        event.joinMessage(null);

        player.setPersistent(false);
        player.setAllowFlight(true);

        // Hide all players from each other
        Bukkit.getOnlinePlayers().forEach(otherPlayer -> {
            player.hidePlayer(plugin, otherPlayer);
            otherPlayer.hidePlayer(plugin, player);
        });

        GeyserSession bedrockSession = playerManager.bedrockSession(player);
        if (bedrockSession != null) {
            AuthData authData = bedrockSession.getAuthData();
            mappingService.insertBedrockProfile(
                player.getUniqueId().getLeastSignificantBits(),
                authData.name(),
                authData.playFabId(),
                authData.issuedAt()
            );
        } else {
            long earliestKnownAt = player.getPlayerProfile().getTextures().getTimestamp();
            if (earliestKnownAt == 0) {
                earliestKnownAt = System.currentTimeMillis();
            }
            mappingService.insertJavaProfile(player.getUniqueId(), player.getName(), earliestKnownAt);
        }

        playerIdleTracker.add(player.getUniqueId());

        linkLookupService.lookup(player).whenComplete(($, throwable) -> {
            if (throwable != null) {
                player.sendMessage(Components.INFO_UNAVAILABLE);
                throwable.printStackTrace();
            }
        });
    }
}
