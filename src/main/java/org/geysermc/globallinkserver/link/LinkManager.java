/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.util.Utils;
import org.mariadb.jdbc.MariaDbPoolDataSource;

public class LinkManager {
    private static final int TEMP_LINK_DURATION = 60_000 * 15; // 15 min
    private final Int2ObjectMap<TempLink> tempLinks = new Int2ObjectOpenHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final MariaDbPoolDataSource dataSource;

    private final Random random = new Random();
    private final HashMap<UUID, Integer> CURRENT_LINK_CODES = new HashMap<>();

    public LinkManager(Config config) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            dataSource = new MariaDbPoolDataSource("jdbc:mariadb://" + config.hostname() + "/" + config.database()
                    + "?user=" + config.username() + "&password=" + config.password() + "&minPoolSize=1&maxPoolSize=3");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("Cannot find required class to load the MariaDB database");
        }
    }

    public int createTempLink(Player player) {
        TempLink link = new TempLink();
        if (Utils.isBedrockPlayerId(player)) {
            link.bedrockId(player.getUniqueId());
        } else {
            link.javaId(player.getUniqueId());
            link.javaUsername(player.getName());
        }
        link.expiryTime(System.currentTimeMillis() + TEMP_LINK_DURATION);
        link.code(createCode());

        tempLinks.put(link.code(), link);
        CURRENT_LINK_CODES.put(player.getUniqueId(), link.code());

        return link.code();
    }

    private int createCode() {
        int code = -1;
        while (code == -1) {
            code = random.nextInt(9999 + 1); // the bound is exclusive

            TempLink link = tempLinks.get(code);
            if (isLinkValid(link)) {
                code = -1;
            }
        }
        return code;
    }

    public TempLink tempLinkById(int linkId) {
        TempLink link = tempLinks.remove(linkId);
        return isLinkValid(link) ? link : null;
    }

    private boolean isLinkValid(TempLink link) {
        long currentMillis = System.currentTimeMillis();
        return link != null && currentMillis - link.expiryTime() < TEMP_LINK_DURATION;
    }

    public void removeTempLinkIfPresent(Player player) {
        Integer linkId = CURRENT_LINK_CODES.remove(player.getUniqueId());
        if (linkId != null) {
            tempLinks.remove((int) linkId);
        }
    }

    public CompletableFuture<Boolean> finaliseLink(TempLink tempLink) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (Connection connection = dataSource.getConnection()) {
                        try (PreparedStatement query = connection.prepareStatement(
                                "INSERT INTO `links` (`java_id`, `bedrock_id`, `java_name`) VALUES (?, ?, ?) "
                                        + "ON DUPLICATE KEY UPDATE "
                                        + "`java_id` = VALUES(`java_id`),"
                                        + "`bedrock_id` = VALUES(`bedrock_id`),"
                                        + "`java_name` = VALUES(`java_name`);")) {
                            query.setString(1, tempLink.javaId().toString());
                            query.setLong(2, tempLink.bedrockId().getLeastSignificantBits());
                            query.setString(3, tempLink.javaUsername());
                            return query.executeUpdate() != 0;
                        }
                    } catch (SQLException exception) {
                        throw new CompletionException("Error while linking player", exception);
                    }
                },
                executorService);
    }

    public CompletableFuture<Boolean> unlinkAccount(Player player) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (Connection connection = dataSource.getConnection()) {

                        PreparedStatement query;
                        if (Utils.isBedrockPlayerId(player)) { // Should never happen
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
                executorService);
    }

    public CompletableFuture<Optional<Link>> attemptFindJavaLink(Player player) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (Connection connection = dataSource.getConnection()) {
                        try (PreparedStatement query = connection.prepareStatement(
                                "SELECT `bedrock_id` FROM `links` WHERE `java_id` = ? LIMIT 1")) {
                            query.setString(1, player.getUniqueId().toString());

                            try (ResultSet resultSet = query.executeQuery()) {
                                if (resultSet.next()) {
                                    long bedrockId = resultSet.getLong("bedrock_id");
                                    String bedrockTag = FloodgateApi.getInstance().getGamertagFor(bedrockId).join();
                                    return Optional.of(
                                            new Link(player)
                                                    .bedrockId(new UUID(0, bedrockId))
                                                    .bedrockUsername(bedrockTag)
                                    );
                                } else {
                                    return Optional.empty(); // No match found
                                }
                            }
                        }
                    } catch (SQLException exception) {
                        throw new CompletionException("Error while finding Java link", exception);
                    }
                },
                executorService);
    }


    public void cleanupTempLinks() {
        IntSet removedLinks = new IntArraySet();
        Iterator<TempLink> iterator = tempLinks.values().iterator();

        long ctm = System.currentTimeMillis();
        while (iterator.hasNext()) {
            TempLink tempLink = iterator.next();

            if (ctm > tempLink.expiryTime()) {
                removedLinks.add(tempLink.code());
                iterator.remove();
            }
        }

        for (Map.Entry<UUID, Integer> entry : CURRENT_LINK_CODES.entrySet()) {
            if (removedLinks.contains((int) entry.getValue())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                int currentLinkCode = CURRENT_LINK_CODES.remove(entry.getKey());
                if (player != null) {
                    player.sendMessage(Component.text("Your link (%s) has expired! Run the link account again if you need a new code.".formatted(currentLinkCode))
                            .color(NamedTextColor.RED));
                }
            }
        }
    }
}
