/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.link.TempLink;

public class CommandUtils {
    public static void handleCommand(LinkManager linkManager, Player player, String message) {

        String[] args = message.split(" ");

        if (args[0].equals("/linkaccount")) {
            if (args.length == 2) {
                int linkId = Utils.parseInt(args[1]);

                if (linkId <= 0) {
                    player.sendMessage("&cInvalid link code");
                    return;
                }

                TempLink tempLink = linkManager.tempLinkById(linkId);

                if (tempLink == null) {
                    player.sendMessage("&cCould not find the provided link. Is it expired?");
                    return;
                }

                if (Utils.isBedrockPlayer(player)) {
                    tempLink.bedrockId(player.getUniqueId());
                } else {
                    tempLink.javaId(player.getUniqueId());
                    tempLink.javaUsername(player.getDisplayName());
                }

                if (tempLink.javaId() == null || tempLink.bedrockId() == null) {
                    player.sendMessage(
                            "&cWelp.. You can only link a Java account to a Bedrock account. Try to start the linking process again.");
                    return;
                }

                linkManager.finaliseLink(tempLink).whenComplete((result, error) -> {
                    if (error != null || !result) {
                        if (error != null) {
                            error.printStackTrace();
                        }
                        System.out.println(result);
                        player.sendMessage(
                                "&cAn unknown error happened while linking your account. Try it again later");
                        return;
                    }

                    Player javaPlayer = Bukkit.getPlayer(tempLink.javaId());
                    Player bedrockPlayer = Bukkit.getPlayer(tempLink.bedrockId());

                    if (javaPlayer != null) {
                        javaPlayer.kickPlayer("&aYou are now successfully linked! :)");
                    }

                    if (bedrockPlayer != null) {
                        bedrockPlayer.kickPlayer("&aYou are now successfully linked! :)");
                    }
                });
                return;
            }

            if (args.length == 1) {
                linkManager.removeTempLinkIfPresent(player);

                String code = String.format("%04d", linkManager.createTempLink(player));
                String otherPlatform = Utils.isBedrockPlayer(player) ? "Java" : "Bedrock";

                player.sendMessage("&aPlease join on " + otherPlatform + " and run `&9/linkaccount &3" + code + "&a`");
                return;
            }

            player.sendMessage(
                    "&cInvalid format! &fValid versions are: `&9/linkaccount&c` to make a link or `&9/linkaccount &3<code>&c` to finalise a link");
            return;
        }

        if (args[0].equals("/unlinkaccount")) {
            if (args.length == 1) {
                linkManager.unlinkAccount(player).whenComplete((result, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                        System.out.println(result);
                        player.sendMessage(
                                "&cAn unknown error happened while unlinking your account. Try it again later");
                        return;
                    }

                    if (result) {
                        player.sendMessage("&aYou are successfully unlinked");
                    } else {
                        player.sendMessage("&eYou aren't linked to any account");
                    }
                });
                return;
            }

            player.sendMessage("&cInvalid format! Use: `&9/unlinkaccount&c`");
            return;
        }

        player.sendMessage("&cUnknown command");
    }
}
