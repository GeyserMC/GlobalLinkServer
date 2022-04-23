/*
 * Copyright (c) 2021-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */

package org.geysermc.globallinkserver.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.globallinkserver.java.JavaPlayer;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.link.TempLink;
import org.geysermc.globallinkserver.player.Player;
import org.geysermc.globallinkserver.player.PlayerManager;

import java.util.concurrent.CompletableFuture;

public class CommandUtils {
    public static void handleCommand(
            LinkManager linkManager,
            PlayerManager playerManager,
            JavaPlayer player,
            String message) {

        String[] args = message.split(" ");

        if (args[0].equals("/linkaccount")) {
            if (args.length == 2) {
                int linkId = Utils.parseInt(args[1]);

                if (linkId <= 0) {
                    player.sendMessage("&cInvalid link code");
                    return;
                }

                String result = linkAccount(player, linkManager, playerManager, linkId);
                if (result != null) {
                    player.sendMessage(result);
                }
                return;
            }

            if (args.length == 1) {
                String code = startAccountLink(player, linkManager);
                player.sendMessage(Component.text("Please join on Bedrock and continue the linking process with the following code: ", NamedTextColor.GREEN)
                        .append(Component.text(code, NamedTextColor.DARK_AQUA)
                                .clickEvent(ClickEvent.copyToClipboard(code))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy to clipboard")))));
                return;
            }

            player.sendMessage(
                    "&cInvalid format! &fValid versions are: `&9/linkaccount&c` to make a link or `&9/linkaccount &3<code>&c` to finalise a link");
            return;
        }

        if (args[0].equals("/unlinkaccount")) {
            if (args.length == 1) {
                unlinkAccount(player, linkManager).whenComplete((result, error) -> {
                    if (error == null) {
                        if (result) {
                            player.sendMessage("&aYou are successfully unlinked");
                        } else {
                            player.sendMessage("&eYou aren't linked to any account");
                        }
                    }
                });
                return;
            }

            player.sendMessage("&cInvalid format! Use: `&9/unlinkaccount&c`");
            return;
        }

        player.sendMessage("&cUnknown command");
    }

    /**
     * @return error string if something goes wrong.
     */
    public static String linkAccount(Player player, LinkManager linkManager, PlayerManager playerManager, int linkId) {
        TempLink tempLink = linkManager.getTempLink(linkId);

        if (tempLink == null) {
            return player.formatMessage("&cCould not find the provided link. Is it expired?");
        }

        if (player instanceof JavaPlayer) {
            tempLink.setJavaId(player.getUniqueId());
            tempLink.setJavaUsername(player.getUsername());
        } else {
            tempLink.setBedrockId(player.getUniqueId());
        }

        if (tempLink.getJavaId() == null || tempLink.getBedrockId() == null) {
            return player.formatMessage(
                    "&cWelp.. You can only link a Java account to a Bedrock account. " +
                            "Try to start the linking process again."
            );
        }

        linkManager.finaliseLink(tempLink).whenComplete((result, error) -> {
            if (error != null || !result) {
                if (error != null) {
                    error.printStackTrace();
                }
                System.out.println(result);
                player.sendMessage("&cAn unknown error happened while linking your account. Try it again later");
                return;
            }

            playerManager.kickPlayers(tempLink.getJavaId(), tempLink.getBedrockId(),
                    "&aYou are now successfully linked! :)");
        });

        return null;
    }

    /**
     * @return temp code.
     */
    public static String startAccountLink(Player player, LinkManager linkManager) {
        if (player.getLinkId() != 0) {
            linkManager.removeTempLink(player.getLinkId());
        }

        return String.format("%04d", linkManager.createTempLink(player));
    }

    public static CompletableFuture<Boolean> unlinkAccount(Player player, LinkManager linkManager) {
        return linkManager.unlinkAccount(player).whenComplete((result, error) -> {
            if (error != null) {
                error.printStackTrace();
                System.out.println(result);
                player.sendMessage(
                        "&cAn unknown error happened while unlinking your account. Try it again later");
            }
        });
    }
}
