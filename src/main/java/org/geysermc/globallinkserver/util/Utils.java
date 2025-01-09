/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.globallinkserver.GlobalLinkServer;
import org.geysermc.globallinkserver.link.Link;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Utils {

    private static final Map<UUID, Link> linkedPlayers = new Object2ObjectOpenHashMap<>();

    public static boolean isBedrockPlayerId(Player player) {
        return FloodgateApi.getInstance().isFloodgateId(player.getUniqueId());
    }

    public static boolean isLinked(Player player) {
        return linkedPlayers.containsKey(player.getUniqueId());
    }

    public static @Nullable Link getLink(Player player) {
        return linkedPlayers.get(player.getUniqueId());
    }

    // TODO we do not have bedrock player names!
    public static Component getLinkInfo(Player player) {
        Link link = getLink(player);
        if (link == null) {
            return null;
        }

        FloodgatePlayer floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        if (floodgatePlayer == null) {
            // Java player
            return Component.empty();
        } else {
            // Bedrock player
            return Component.empty();
        }
    }

    public static void processJoin(Player player) {
        FloodgatePlayer floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        if (floodgatePlayer == null) {
            // Not dealing with a Bedrock player - now check if this Java player has a link
            GlobalLinkServer.linkManager.attemptFindJavaLink(player).whenComplete((link, throwable) -> {
                if (throwable != null) {
                    player.sendMessage(Component.text("Failed to find Java link.").color(NamedTextColor.RED));
                    throwable.printStackTrace();
                    return;
                }

                link.ifPresent(value -> linkedPlayers.put(player.getUniqueId(), value));
            });
        } else {
            // easy
            if (floodgatePlayer.isLinked()) {
                linkedPlayers.put(player.getUniqueId(), new Link()
                        .javaUsername(player.getName())
                        .javaId(player.getUniqueId())
                        .bedrockId(floodgatePlayer.getJavaUniqueId()));
            }
        }
    }

    public static void processLeave(Player player) {
        linkedPlayers.remove(player.getUniqueId());
    }
}
