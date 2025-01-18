/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.service;

import org.bukkit.entity.Player;
import org.geysermc.globallinkserver.Components;
import org.geysermc.globallinkserver.link.FullLink;
import org.geysermc.globallinkserver.manager.PlayerManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LinkInfoService {
    private final LinkLookupService linkLookupService;
    private final PlayerManager playerManager;

    public LinkInfoService(LinkLookupService linkLookupService, PlayerManager playerManager) {
        this.linkLookupService = linkLookupService;
        this.playerManager = playerManager;
    }

    public void sendCurrentLinkInfo(Player player) {
        FullLink link = linkLookupService.cachedLookup(player);
        if (link == null) {
            player.sendMessage(Components.INFO_NOT_LINKED);
            return;
        }

        // Show info from the opposite platform
        if (playerManager.isBedrockPlayer(player)) {
            player.sendMessage(Components.infoLinkInfo(link.javaUsername(), link.javaId(), false));
        } else {
            player.sendMessage(Components.infoLinkInfo(link.bedrockUsername(), link.bedrockId(), true));
        }
    }
}
