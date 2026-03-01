/*
 * Copyright (c) 2026 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.geysermc.globallinkserver.manager.DatabaseManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class MappingService {
    private final DatabaseManager database;

    public MappingService(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Void> insertJavaProfile(UUID uuid, String username, long retrievedAtMillis) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = database.connection()) {
                try (PreparedStatement query = connection.prepareStatement("""
                        INSERT INTO java_identity_current AS c(id, username, detected_at)
                        VALUES (?::uuid, ?, ?)
                        ON CONFLICT (id) DO
                          UPDATE SET username = EXCLUDED.username, detected_at = EXCLUDED.detected_at
                          WHERE c.detected_at <= EXCLUDED.detected_at AND c.username != EXCLUDED.username""")) {
                    query.setString(1, uuid.toString());
                    query.setString(2, username);
                    query.setTimestamp(3, Timestamp.from(Instant.ofEpochMilli(retrievedAtMillis)));
                    query.executeUpdate();
                }
            } catch (SQLException exception) {
                throw new CompletionException("Error while inserting Java profile", exception);
            }
        }, database.executor());
    }
    public CompletableFuture<Void> insertBedrockProfile(long xuid, String gamertag, String playfabId, long issuedAt) {
        Instant issuedAtInstant = Instant.ofEpochSecond(issuedAt);

        return CompletableFuture.runAsync(() -> {
            try (Connection connection = database.connection()) {
                try (PreparedStatement query = connection.prepareStatement("""
                        INSERT INTO xbox_identity_current AS c(xuid, gamertag, detected_at)
                        VALUES (?::xuid, ?, ?)
                        ON CONFLICT (xuid) DO
                          UPDATE SET gamertag = EXCLUDED.gamertag, detected_at = EXCLUDED.detected_at
                          WHERE c.detected_at <= EXCLUDED.detected_at AND c.gamertag != EXCLUDED.gamertag""")) {
                    query.setLong(1, xuid);
                    query.setString(2, gamertag);
                    query.setTimestamp(3, Timestamp.from(issuedAtInstant));
                    query.executeUpdate();
                }
                try (PreparedStatement query = connection.prepareStatement("""
                        INSERT INTO playfab_identity (id, xuid, detected_at)
                        VALUES (?, ?, ?)
                        ON CONFLICT (id) DO NOTHING""")) {
                    query.setString(1, playfabId);
                    query.setLong(2, xuid);
                    query.setTimestamp(3, Timestamp.from(issuedAtInstant));
                    query.executeUpdate();
                }
            } catch (SQLException exception) {
                throw new CompletionException("Error while inserting Java profile", exception);
            }
        }, database.executor());
    }
}
