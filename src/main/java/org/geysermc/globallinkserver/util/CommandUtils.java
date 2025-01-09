/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.globallinkserver.link.Link;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.link.TempLink;

@SuppressWarnings("UnstableApiUsage")
public class CommandUtils {

    public CommandUtils(LinkManager linkManager) {
        this.linkManager = linkManager;
    }

    LinkManager linkManager;

    public int startLink(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);

        if (Utils.isLinked(player)) {
            player.sendMessage("You are already linked!");
            return 0;
        }

        linkManager.removeTempLinkIfPresent(player);

        String code = String.format("%04d", linkManager.createTempLink(player));
        String otherPlatform = Utils.isBedrockPlayerId(player) ? "Java" : "Bedrock";

        player.sendMessage(Component.text("Please join on %s and run ".formatted(otherPlatform))
                .color(NamedTextColor.GREEN)
                .append(Component.text("`/link " + code + "`", NamedTextColor.AQUA)));
        return 1;
    }

    public int linkWithCode(CommandContext<CommandSourceStack> ctx) {
        int linkId = ctx.getArgument("code", Integer.class);

        Player player = getPlayer(ctx);

        if (linkId <= 0) {
            player.sendMessage(Component.text("Invalid link code!").color(NamedTextColor.RED));
            return 0;
        }

        TempLink tempLink = linkManager.tempLinkById(linkId);

        if (tempLink == null) {
            player.sendMessage(Component.text("Could not find the provided link. Is it expired?").color(NamedTextColor.RED));
            return 0;
        }

        if (Utils.isBedrockPlayerId(player)) {
            tempLink.bedrockId(player.getUniqueId());
        } else {
            tempLink.javaId(player.getUniqueId());
            tempLink.javaUsername(player.getName());
        }

        if (tempLink.javaId() == null || tempLink.bedrockId() == null) {
            player.sendMessage(Component.text("You can only link a Java account to a Bedrock account.")
                    .color(NamedTextColor.RED)
                    .append(Component.text("Try to start the linking process again!")));
            return 0;
        }

        linkManager.finaliseLink(tempLink).whenComplete((result, error) -> {
            if (error != null || !result) {
                if (error != null) {
                    error.printStackTrace();
                }
                System.out.println(result);
                player.sendMessage(Component.text("An unknown error occurred while linking your account. Try it again later!")
                        .color(NamedTextColor.RED));
                return;
            }

            Player javaPlayer = Bukkit.getPlayer(tempLink.javaId());
            Player bedrockPlayer = Bukkit.getPlayer(tempLink.bedrockId());

            if (javaPlayer != null) {
                javaPlayer.kick(Component.text("You are now successfully linked! :)").color(NamedTextColor.GREEN));
            }

            if (bedrockPlayer != null) {
                bedrockPlayer.kick(Component.text("You are now successfully linked! :)").color(NamedTextColor.GREEN));
            }
        });
        return 1;
    }

    public int unlink(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        linkManager.unlinkAccount(player).whenComplete((result, error) -> {
            if (error != null) {
                error.printStackTrace();
                System.out.println(result);
                player.sendMessage(Component.text("An unknown error occurred while unlinking your account. Try it again later!")
                        .color(NamedTextColor.RED));
                return;
            }

            if (result) {
                player.kick(Component.text("You are successfully unlinked.").color(NamedTextColor.GREEN));
            } else {
                player.kick(Component.text("You are not linked to any account!").color(NamedTextColor.RED));
            }
        });
        return 1;
    }

    public static Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        return (Player) ctx.getSource().getExecutor();
    }

    public int info(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);

        Link link = Utils.getLink(player);
        if (link == null) {
            player.sendMessage(Component.text("You are not currently linked!").color(NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(Component.text("You are currently linked!").color(NamedTextColor.GREEN));
        return 1;
    }
}
