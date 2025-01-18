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
import org.geysermc.globallinkserver.util.ThrowingConsumer;
import org.geysermc.globallinkserver.util.ThrowingFunction;
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
        return attemptFindLink(
                        "SELECT `bedrock_id` FROM `links` WHERE `java_id` = ?",
                        stmt -> stmt.setString(1, javaId.toString()),
                        resultSet -> resultSet.getLong("bedrock_id"))
                .thenCompose(xuid -> {
                    if (xuid == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return playerManager.fetchGamertagFor(xuid).thenApply(gamertag -> {
                        return new FullLink(new UUID(0, xuid), gamertag, javaId, javaName);
                    });
                });
    }

    public CompletableFuture<@Nullable FullLink> findBedrockLink(UUID bedrockId, String gamertag) {
        return attemptFindLink(
                "SELECT `java_id`, `java_name` FROM `links` WHERE `bedrock_id` = ?",
                stmt -> stmt.setLong(1, bedrockId.getLeastSignificantBits()),
                resultSet -> {
                    UUID javaId = UUID.fromString(resultSet.getString("java_id"));
                    String javaName = resultSet.getString("java_name");
                    return new FullLink(bedrockId, gamertag, javaId, javaName);
                });
    }

    private <T> CompletableFuture<T> attemptFindLink(
            String query,
            ThrowingConsumer<PreparedStatement> parameterSetter,
            ThrowingFunction<ResultSet, T> resultProcessor) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (Connection connection = database.connection();
                            PreparedStatement queryStmt = connection.prepareStatement(query)) {
                        parameterSetter.accept(queryStmt);

                        try (ResultSet resultSet = queryStmt.executeQuery()) {
                            if (resultSet.next()) {
                                return resultProcessor.apply(resultSet);
                            } else {
                                return null;
                            }
                        }

                    } catch (SQLException exception) {
                        throw new CompletionException("Error while finding link! ", exception);
                    }
                },
                database.executor());
    }
}
