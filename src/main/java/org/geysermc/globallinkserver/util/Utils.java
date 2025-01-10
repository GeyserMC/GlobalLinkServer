/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.globallinkserver.GlobalLinkServer;
import org.geysermc.globallinkserver.link.Link;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
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

    public static Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        return (Player) ctx.getSource().getExecutor();
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
                        .bedrockUsername(floodgatePlayer.getUsername())
                        .bedrockId(floodgatePlayer.getJavaUniqueId()));
            }
        }
    }

    public static void processLeave(Player player) {
        linkedPlayers.remove(player.getUniqueId());
    }

    public static void sendCurrentLinkInfo(Player player) {
        Link link = linkedPlayers.get(player.getUniqueId());
        if (link == null) {
            player.sendMessage(Component.text("You are not currently linked.").color(NamedTextColor.AQUA));
            return;
        }

        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            // Bedrock player, show Java info
            player.sendMessage(Component.text("You are currently linked to the Java player %s (%s).".formatted(
                    link.javaUsername(), link.javaId())).color(NamedTextColor.GREEN)
            );
        } else {
            player.sendMessage(Component.text("You are currently linked to the Bedrock player %s (%s).".formatted(
                    link.bedrockUsername(), link.bedrockId())).color(NamedTextColor.GREEN)
            );
        }
    }
}
