/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.entity.Player;
import org.geysermc.globallinkserver.Components;
import org.geysermc.globallinkserver.manager.DatabaseManager;
import org.geysermc.globallinkserver.manager.PlayerManager;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class LinkManager {
    private static final int PENDING_LINK_TTL_MILLIS = 15 * 60 * 1000; // 15 min

    private final PlayerManager playerManager;
    private final DatabaseManager database;

    private final Int2ObjectMap<LinkRequest> linkRequests = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<UUID> linkRequestForPlayer = new Object2IntOpenHashMap<>() {
        {
            defaultReturnValue(-1);
        }
    };

    private final Random random = new Random();

    public LinkManager(PlayerManager playerManager, DatabaseManager database) {
        this.playerManager = playerManager;
        this.database = database;
    }

    public int createTempLink(Player player) {
        var linkRequest = new LinkRequest(createCode(), PENDING_LINK_TTL_MILLIS, player);

        linkRequests.put(linkRequest.code(), linkRequest);
        linkRequestForPlayer.put(player.getUniqueId(), linkRequest.code());
        return linkRequest.code();
    }

    private int createCode() {
        int code = -1;
        while (code == -1) {
            code = random.nextInt(9999 + 1); // the bound is exclusive

            LinkRequest link = linkRequests.get(code);
            if (isLinkValid(link)) {
                code = -1;
            }
        }
        return code;
    }

    public @Nullable LinkRequest linkRequestByCode(int code) {
        LinkRequest link = linkRequests.remove(code);
        return isLinkValid(link) ? link : null;
    }

    private boolean isLinkValid(@Nullable LinkRequest link) {
        long currentMillis = System.currentTimeMillis();
        return link != null && currentMillis < link.expiryTime();
    }

    public boolean removeActiveLinkRequest(Player player) {
        int code = linkRequestForPlayer.removeInt(player.getUniqueId());
        if (code != -1) {
            linkRequests.remove(code);
        }
        return code != -1;
    }

    public boolean hasActiveLinkRequest(UUID uuid) {
        int code = linkRequestForPlayer.getInt(uuid);
        if (code == -1) {
            return false;
        }
        var request = linkRequests.get(code);
        //noinspection ConstantValue ??
        return request != null && isLinkValid(request);
    }

    public CompletableFuture<Boolean> finaliseLink(Link linkRequest) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (Connection connection = database.connection()) {
                        try (PreparedStatement query = connection.prepareStatement(
                                "INSERT INTO `links` (`java_id`, `bedrock_id`, `java_name`) VALUES (?, ?, ?) "
                                        + "ON DUPLICATE KEY UPDATE "
                                        + "`java_id` = VALUES(`java_id`),"
                                        + "`bedrock_id` = VALUES(`bedrock_id`),"
                                        + "`java_name` = VALUES(`java_name`);")) {
                            query.setString(1, linkRequest.javaId().toString());
                            query.setLong(2, linkRequest.bedrockId());
                            query.setString(3, linkRequest.javaUsername());
                            return query.executeUpdate() != 0;
                        }
                    } catch (SQLException exception) {
                        throw new CompletionException("Error while linking player", exception);
                    }
                },
                database.executor());
    }

    public CompletableFuture<Boolean> unlinkAccount(Player player) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (Connection connection = database.connection()) {
                        PreparedStatement query;
                        if (playerManager.isBedrockPlayer(player)) {
                            query = connection.prepareStatement("DELETE FROM `links` WHERE `bedrock_id` = ?;");
                            query.setLong(1, player.getUniqueId().getLeastSignificantBits());
                        } else {
                            query = connection.prepareStatement("DELETE FROM `links` WHERE `java_id` = ?;");
                            query.setString(1, player.getUniqueId().toString());
                        }
                        boolean affected = query.executeUpdate() != 0;
                        query.close();
                        return affected;
                    } catch (SQLException exception) {
                        throw new CompletionException("Error while unlinking player", exception);
                    }
                },
                database.executor());
    }

    public void cleanupLinkRequests() {
        Iterator<LinkRequest> iterator = linkRequests.values().iterator();

        long ctm = System.currentTimeMillis();
        while (iterator.hasNext()) {
            LinkRequest linkRequest = iterator.next();

            if (ctm > linkRequest.expiryTime()) {
                iterator.remove();
                linkRequestForPlayer.remove(linkRequest.requesterUuid(), linkRequest.code());

                var requester = linkRequest.requester();
                if (requester != null) {
                    requester.sendMessage(Components.cleanupLinkRequestExpired(linkRequest.code()));
                }
            }
        }
    }
}
