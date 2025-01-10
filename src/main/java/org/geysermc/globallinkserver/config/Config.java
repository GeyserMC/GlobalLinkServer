/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.config;

public record Config(
    Database database,
    Util util
) {
    public record Database(
            String hostname,
            String username,
            String password,
            String database
    ) {}

    public record Util(
            boolean hideJoinLeaveMessages,
            boolean hideDeathMessages,
            boolean hidePlayers,
            boolean disableChat,
            boolean voidTeleport,
            boolean preventHunger,
            boolean respawnOnJoin
    ) {}
}
