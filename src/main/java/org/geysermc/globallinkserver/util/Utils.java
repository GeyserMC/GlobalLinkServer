/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;

public class Utils {

    public static int parseInt(String toParse) {
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    public static boolean isBedrockPlayer(Player player) {
        return GeyserApi.api().isBedrockPlayer(player.getUniqueId());
    }
}
