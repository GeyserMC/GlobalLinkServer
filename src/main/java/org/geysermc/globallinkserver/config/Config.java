/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.config;

import org.bukkit.Location;

public record Config(
    Database database,
    Location spawn
) {
    public record Database(
            String hostname,
            String username,
            String password,
            String database
    ) {}
}
