/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.manager;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PlayerManager {
    private final FloodgateApi api;
    private final GeyserImpl geyserImpl;

    public PlayerManager(FloodgateApi api, GeyserImpl geyserImpl) {
        this.api = api;
        this.geyserImpl = geyserImpl;
    }

    public boolean isBedrockPlayer(Player player) {
        return api.isFloodgatePlayer(player.getUniqueId());
    }

    public boolean isBedrockId(UUID id) {
        return api.isFloodgateId(id);
    }

    public @Nullable FloodgatePlayer bedrockPlayer(UUID uuid) {
        return api.getPlayer(uuid);
    }

    private @Nullable GeyserSession bedrockSession(Player player) {
        // There is no linking in the linking server itself, only link management
        if (player.getUniqueId().getMostSignificantBits() != 0L) {
            return null;
        }

        String xuid = String.valueOf(player.getUniqueId().getLeastSignificantBits());
        for (GeyserSession session : geyserImpl.getSessionManager().getAllSessions()) {
            if (session.xuid().equals(xuid)) {
                return session;
            }
        }
        return null;
    }

    public String correctUsername(Player player) {
        FloodgatePlayer floodgatePlayer = bedrockPlayer(player.getUniqueId());
        if (floodgatePlayer == null) {
            return player.getName();
        }
        return floodgatePlayer.getUsername();
    }

    /**
     * Returns the earliest known time that the profile has this name.
     * Works for both Java and Bedrock accounts.
     */
    public long nameTimestampMillis(Player player) {
        GeyserSession session = bedrockSession(player);
        if (session == null) {
            long timestamp = player.getPlayerProfile().getTextures().getTimestamp();
            if (timestamp == 0L) {
                return System.currentTimeMillis();
            }
            return timestamp;
        }

        long issuedAt = session.getAuthData().issuedAt();
        if (issuedAt == -1) {
            return System.currentTimeMillis();
        }
        return issuedAt * 1000L;
    }
}
