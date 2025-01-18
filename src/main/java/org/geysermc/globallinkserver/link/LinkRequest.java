/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record LinkRequest(int code, long expiryTime, UUID requesterUuid, String requesterUsername) {
    public LinkRequest(int code, long ttl, Player requester) {
        this(code, System.currentTimeMillis() + ttl, requester.getUniqueId(), requester.getName());
    }

    public @Nullable Player requester() {
        return Bukkit.getPlayer(requesterUuid);
    }
}
