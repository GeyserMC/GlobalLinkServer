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
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinkManager {
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
        link.setExpiryTime(System.currentTimeMillis() + (60_000 * 30));
        link.setCode(createCode());

        tempLinks.put(link.getCode(), link);

        player.setLinkId(link.getCode());

        return link.getCode();
    }

    private int createCode() {
        int code;
        do {
            code = random.nextInt(9999) + 1;
        } while (tempLinks.containsKey(code));
        return code;
    }

    public TempLink getTempLink(int linkId) {
        TempLink link = tempLinks.remove(linkId);
        if (link != null && System.currentTimeMillis() - link.getExpiryTime() < (60_000 * 30)) {
            return link;
        }
        return null;
    }

    public void removeTempLink(int linkId) {
        tempLinks.remove(linkId);
    }

    public CompletableFuture<Boolean> finaliseLink(TempLink tempLink) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = dataSource.getConnection();

                PreparedStatement query = connection.prepareStatement(
                        "INSERT INTO `links` (`javaId`, `bedrockId`, `javaName`) VALUES (?, ?, ?) "
                                + "ON DUPLICATE KEY UPDATE "
                                + "`javaId` = VALUES(`javaId`), "
                                + "`javaName` = VALUES(`javaName`);");
                query.setString(1, tempLink.getJavaId().toString());
                query.setLong(2, tempLink.getBedrockId().getLeastSignificantBits());
                query.setString(3, tempLink.getJavaUsername());
                return query.executeUpdate() != 0;
            } catch (SQLException exception) {
                throw new CompletionException("Error while linking player", exception);
            }
        }, executorService);
    }

    public CompletableFuture<Boolean> unlinkAccount(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = dataSource.getConnection();

                PreparedStatement query;
                if (player instanceof JavaPlayer) {
                    query = connection.prepareStatement(
                            "DELETE FROM `links` WHERE `javaId` = ?;");
                    query.setString(1, player.getUniqueId().toString());
                } else {
                    query = connection.prepareStatement(
                            "DELETE FROM `links` WHERE `bedrockId` = ?;");
                    query.setLong(1, player.getUniqueId().getLeastSignificantBits());
                }
                return query.executeUpdate() != 0;
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
