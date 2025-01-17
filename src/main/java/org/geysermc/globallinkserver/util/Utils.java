/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.globallinkserver.GlobalLinkServer;
import org.geysermc.globallinkserver.link.Link;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class Utils {

    private static final Map<UUID, Link> linkedPlayers = new Object2ObjectOpenHashMap<>();
    private static final Set<UUID> lookupInProcess = new ObjectOpenHashSet<>();

    public static boolean shouldShowSuggestion(Player player) {
        return !lookupInProcess.contains(player.getUniqueId());
    }

    public static boolean isBedrockPlayerId(Player player) {
        return player.getUniqueId().version() == 0;
    }

    public static boolean isLinked(Player player) {
        return linkedPlayers.containsKey(player.getUniqueId());
    }

    public static Link getLink(Player player) {
        return linkedPlayers.get(player.getUniqueId());
    }

    public static Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        return (Player) ctx.getSource().getExecutor();
    }

    public static void processJoin(Player player) {
        lookupInProcess.add(player.getUniqueId());

        FloodgatePlayer floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        CompletableFuture<Optional<Link>> linkFuture;

        if (floodgatePlayer != null) {
            // Dealing with a Bedrock player
            linkFuture = GlobalLinkServer.linkManager.attemptFindBedrockLink(player, floodgatePlayer.getUsername());
        } else {
            linkFuture = GlobalLinkServer.linkManager.attemptFindJavaLink(player);
        }

        // Handle the result of the lookup
        linkFuture.whenComplete((link, throwable) -> handleLinkLookupResult(player, link, throwable));
    }

    private static void handleLinkLookupResult(Player player, Optional<Link> link, Throwable throwable) {
        lookupInProcess.remove(player.getUniqueId());
        if (throwable != null) {
            player.sendMessage(Component.text("Failed to find current link!").color(NamedTextColor.RED));
            throwable.printStackTrace();
            return;
        }

        link.ifPresent(value -> linkedPlayers.put(player.getUniqueId(), value));
    }

    public static void processLeave(Player player) {
        linkedPlayers.remove(player.getUniqueId());
        lookupInProcess.remove(player.getUniqueId());
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

    public static void fakeRespawn(Player player) {
        player.teleport(player.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        player.setFallDistance(0);
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
}
