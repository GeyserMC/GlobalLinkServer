/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import org.bukkit.entity.Player;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record FullLink(UUID bedrockId, String bedrockUsername, UUID javaId, String javaUsername) {
    public UUID getOpposed(Player player) {
        return player.getUniqueId().equals(bedrockId) ? javaId : bedrockId;
    }
}
