/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.handler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.globallinkserver.Components;
import org.geysermc.globallinkserver.link.FullLink;
import org.geysermc.globallinkserver.link.Link;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.link.LinkRequest;
import org.geysermc.globallinkserver.manager.PlayerManager;
import org.geysermc.globallinkserver.service.LinkInfoService;
import org.geysermc.globallinkserver.service.LinkLookupService;
import org.jspecify.annotations.NullMarked;

import static org.geysermc.globallinkserver.util.Utils.contextExecutor;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class CommandHandler {
    private final LinkLookupService linkLookupService;
    private final LinkInfoService linkInfoService;
    private final LinkManager linkManager;
    private final PlayerManager playerManager;
    private final Plugin plugin;

    public CommandHandler(LinkLookupService linkLookupService, LinkInfoService linkInfoService, LinkManager linkManager, PlayerManager playerManager, Plugin plugin) {
        this.linkLookupService = linkLookupService;
        this.linkInfoService = linkInfoService;
        this.linkManager = linkManager;
        this.playerManager = playerManager;
        this.plugin = plugin;
    }

    public int startLink(CommandContext<CommandSourceStack> ctx) {
        Player player = contextExecutor(ctx);

        if (linkLookupService.isLinkedCached(player)) {
            player.sendMessage(Components.LINK_ALREADY_LINKED);
            linkInfoService.sendCurrentLinkInfo(player);
            return Command.SINGLE_SUCCESS;
        }

        //todo use the boolean return and send a message that the active link request has been invalidated
        linkManager.removeActiveLinkRequest(player);

        String code = String.format("%04d", linkManager.createTempLink(player));
        String otherPlatform = playerManager.isBedrockPlayer(player) ? "Java" : "Bedrock";
        player.sendMessage(Components.linkStarted(otherPlatform, code));
        return Command.SINGLE_SUCCESS;
    }

    public int linkWithCode(CommandContext<CommandSourceStack> ctx) {
        int code = IntegerArgumentType.getInteger(ctx, "code");
        Player player = contextExecutor(ctx);

        if (code < 0 || code > 9999) {
            player.sendMessage(Components.LINK_CODE_INVALID_RANGE);
            return Command.SINGLE_SUCCESS;
        }

        LinkRequest linkRequest = linkManager.linkRequestByCode(code);
        if (linkRequest == null) {
            player.sendMessage(Components.LINK_REQUEST_NOT_FOUND);
            return Command.SINGLE_SUCCESS;
        }

        boolean isRequesterBedrock = playerManager.isBedrockId(linkRequest.requesterUuid());
        boolean isCompleteLink = isRequesterBedrock != playerManager.isBedrockId(player.getUniqueId());

        if (!isCompleteLink) {
            player.sendMessage(Components.LINK_REQUEST_SAME_PLATFORM);
            return Command.SINGLE_SUCCESS;
        }

        var completedLink = Link.fromRequest(linkRequest, player.getUniqueId(), player.getName(), isRequesterBedrock);

        linkManager.finaliseLink(completedLink).whenComplete((result, error) -> {
            if (error != null || !result) {
                if (error != null) {
                    error.printStackTrace();
                }
                System.out.println(result);
                player.sendMessage(Components.LINK_CREATE_ERROR);
                return;
            }

            var requestPlayer = linkRequest.requester();

            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                player.kick(Components.LINK_CREATE_SUCCESS);

                if (requestPlayer != null) {
                    requestPlayer.kick(Components.LINK_CREATE_SUCCESS);
                }

                return null;
            });
        });
        return Command.SINGLE_SUCCESS;
    }

    public int unlink(CommandContext<CommandSourceStack> ctx) {
        Player player = contextExecutor(ctx);

        FullLink currentLink = linkLookupService.cachedLookup(player);
        if (currentLink == null) {
            player.sendMessage(Components.UNLINK_NOT_LINKED);
            return Command.SINGLE_SUCCESS;
        }

        linkManager.unlinkAccount(player).whenComplete((result, error) -> {
            if (error != null) {
                error.printStackTrace();
                player.sendMessage(Components.UNLINK_ERROR);
                return;
            }

            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                if (result) {
                    player.kick(Components.UNLINK_SUCCESS);

                    // Lookup whether the player's link is online, kick em too
                    Player otherLink = Bukkit.getServer().getPlayer(currentLink.getOpposed(player));
                    if (otherLink != null) {
                        otherLink.kick(Components.UNLINK_SUCCESS);
                    }
                } else {
                    // Technically impossible
                    player.kick(Components.UNLINK_NOT_LINKED);
                }

                return null;
            });
        });
        return Command.SINGLE_SUCCESS;
    }
}
