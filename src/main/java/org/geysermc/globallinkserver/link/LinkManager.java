/*
 * Copyright (c) 2021-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */

package org.geysermc.globallinkserver.link;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.java.JavaPlayer;
import org.geysermc.globallinkserver.player.Player;
import org.geysermc.globallinkserver.player.PlayerManager;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinkManager {
    private static final int TEMP_LINK_DURATION = 60_000 * 15; // 15 min
    private final Int2ObjectMap<TempLink> tempLinks = new Int2ObjectOpenHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final MariaDbPoolDataSource dataSource;

    private final Random random = new Random();

    public LinkManager(Config config) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            dataSource = new MariaDbPoolDataSource(
                    "jdbc:mariadb://" + config.getHostname() + "/" + config.getDatabase() +
                            "?user=" + config.getUsername() + "&password=" + config.getPassword() +
                            "&minPoolSize=1&maxPoolSize=3");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("Cannot find required class to load the MariaDB database");
        }
    }

    public int createTempLink(Player player) {
        TempLink link = new TempLink();
        if (player instanceof JavaPlayer) {
            link.setJavaId(player.getUniqueId());
            link.setJavaUsername(player.getUsername());
        } else {
            link.setBedrockId(player.getUniqueId());
        }
        link.setExpiryTime(System.currentTimeMillis() + TEMP_LINK_DURATION);
        link.setCode(createCode());

        tempLinks.put(link.getCode(), link);

        player.setLinkId(link.getCode());

        return link.getCode();
    }

    private int createCode() {
        int code = -1;
        while (code == -1) {
            code = random.nextInt(9999) + 1;

            TempLink link = tempLinks.get(code);
            if (isLinkValid(link)) {
                code = -1;
            }
        }
        return code;
    }

    public TempLink getTempLink(int linkId) {
        TempLink link = tempLinks.remove(linkId);
        return isLinkValid(link) ? link : null;
    }

    private boolean isLinkValid(TempLink link) {
        long currentMillis = System.currentTimeMillis();
        return link != null && currentMillis - link.getExpiryTime() < TEMP_LINK_DURATION;
    }

    public void removeTempLink(int linkId) {
        tempLinks.remove(linkId);
    }

    public CompletableFuture<String> findLinkForBedrock(UUID bedrockId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT bedrock_id, java_id, java_name FROM links WHERE bedrock_id = ?")) {
                    query.setLong(1, bedrockId.getLeastSignificantBits());
                    try (ResultSet result = query.executeQuery()) {
                        if (!result.next()) {
                            return null;
                        }
                        return result.getString("java_name");
                    }
                }
            } catch (SQLException exception) {
                throw new CompletionException("Error while linking player", exception);
            }
        });
    }

    public CompletableFuture<Boolean> isJavaPlayerLinked(UUID javaId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT bedrock_id, java_id, java_name FROM links WHERE java_id = ?")) {
                    query.setString(1, javaId.toString());
                    try (ResultSet result = query.executeQuery()) {
                        return result.next();
                    }
                }
            } catch (SQLException exception) {
                throw new CompletionException("Error while linking player", exception);
            }
        });
    }

    public CompletableFuture<Boolean> finaliseLink(TempLink tempLink) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement query = connection.prepareStatement(
                        "INSERT INTO `links` (`java_id`, `bedrock_id`, `java_name`) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "`java_id` = VALUES(`java_id`)," +
                                "`bedrock_id` = VALUES(`bedrock_id`)," +
                                "`java_name` = VALUES(`java_name`);")) {
                    query.setString(1, tempLink.getJavaId().toString());
                    query.setLong(2, tempLink.getBedrockId().getLeastSignificantBits());
                    query.setString(3, tempLink.getJavaUsername());
                    return query.executeUpdate() != 0;
                }
            } catch (SQLException exception) {
                throw new CompletionException("Error while linking player", exception);
            }
        }, executorService);
    }

    public CompletableFuture<Boolean> unlinkAccount(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {

                PreparedStatement query;
                if (player instanceof JavaPlayer) {
                    query = connection.prepareStatement(
                            "DELETE FROM `links` WHERE `java_id` = ?;");
                    query.setString(1, player.getUniqueId().toString());
                } else {
                    query = connection.prepareStatement(
                            "DELETE FROM `links` WHERE `bedrock_id` = ?;");
                    query.setLong(1, player.getUniqueId().getLeastSignificantBits());
                }
                boolean affected = query.executeUpdate() != 0;
                query.close();
                return affected;
            } catch (SQLException exception) {
                throw new CompletionException("Error while unlinking player", exception);
            }
        }, executorService);
    }

    public void cleanupTempLinks(PlayerManager playerManager) {
        IntSet removedLinks = new IntArraySet();

        Iterator<TempLink> iterator = tempLinks.values().iterator();

        long ctm = System.currentTimeMillis();
        while (iterator.hasNext()) {
            TempLink tempLink = iterator.next();

            if (ctm > tempLink.getExpiryTime()) {
                removedLinks.add(tempLink.getCode());
                iterator.remove();
            }
        }

        List<Player> players = playerManager.getPlayers(removedLinks);
        for (Player player : players) {
            player.sendMessage(String.format(
                    "&cYour link (%s) has expired! Run the link account command again if you need a new code.",
                    player.getLinkId()));
            player.setLinkId(0);
        }
    }
}
