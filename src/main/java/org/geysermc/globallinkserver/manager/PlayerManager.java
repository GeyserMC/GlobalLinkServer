/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.manager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PlayerManager {
    private final FloodgateApi api;

    public PlayerManager(FloodgateApi api) {
        this.api = api;
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

    public CompletableFuture<@Nullable String> fetchGamertagFor(long xuid) {
        return api.getGamertagFor(xuid);
    }
}
