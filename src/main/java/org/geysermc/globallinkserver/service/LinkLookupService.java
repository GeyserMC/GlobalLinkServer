/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.service;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.entity.Player;
import org.geysermc.globallinkserver.link.FullLink;
import org.geysermc.globallinkserver.manager.DatabaseManager;
import org.geysermc.globallinkserver.manager.PlayerManager;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class LinkLookupService {
    private final PlayerManager playerManager;
    private final DatabaseManager database;

    private final Map<UUID, FullLink> linkedPlayers = new Object2ObjectOpenHashMap<>();
    private final Set<UUID> lookupInProcess = new ObjectOpenHashSet<>();

    public LinkLookupService(PlayerManager playerManager, DatabaseManager database) {
        this.playerManager = playerManager;
        this.database = database;
    }

    public boolean isLookupCompleted(Player player) {
        return !lookupInProcess.contains(player.getUniqueId());
    }

    public @Nullable FullLink cachedLookup(Player player) {
        return linkedPlayers.get(player.getUniqueId());
    }

    public boolean isLinkedCached(Player player) {
        return cachedLookup(player) != null;
    }

    public CompletableFuture<@Nullable FullLink> lookup(Player player) {
        var uuid = player.getUniqueId();
        var floodgatePlayer = playerManager.bedrockPlayer(uuid);

        lookupInProcess.add(uuid);

        CompletableFuture<FullLink> future;
        if (floodgatePlayer != null) {
            future = findBedrockLink(uuid, floodgatePlayer.getUsername());
        } else {
            future = findJavaLink(uuid, player.getName());
        }

        return future.whenComplete((link, throwable) -> {
            lookupInProcess.remove(uuid);
            if (throwable == null && link != null) {
                linkedPlayers.put(uuid, link);
            }
        });
    }

    public void invalidate(Player player) {
        linkedPlayers.remove(player.getUniqueId());
        lookupInProcess.remove(player.getUniqueId());
    }

    public CompletableFuture<@Nullable FullLink> findJavaLink(UUID javaId, String javaName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                    SELECT xuid, gamertag
                    FROM links
                    LEFT JOIN xbox_identity_current USING (xuid)
                    WHERE java_id = ?::uuid""")) {

                statement.setString(1, javaId.toString());

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }
                    return new FullLink(
                        new UUID(0, result.getLong("xuid")),
                        result.getString("gamertag"),
                        javaId,
                        javaName);
                }
            } catch (SQLException exception) {
                throw new CompletionException("Error while finding link! ", exception);
            }
        }, database.executor());
    }

    public CompletableFuture<@Nullable FullLink> findBedrockLink(UUID bedrockId, String gamertag) {
        long xuid = bedrockId.getLeastSignificantBits();

        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                    SELECT java_id, username
                    FROM links
                    LEFT JOIN java_identity_current ON java_id = id
                    WHERE xuid = ?::xuid""")) {

                statement.setLong(1, xuid);

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }
                    return new FullLink(
                        new UUID(0, xuid),
                        gamertag,
                        UUID.fromString(result.getString("java_id")),
                        result.getString("username"));
                }
            } catch (SQLException exception) {
                throw new CompletionException("Error while finding link! ", exception);
            }
        }, database.executor());
    }
}
