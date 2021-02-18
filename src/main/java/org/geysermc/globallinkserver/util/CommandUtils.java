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

import org.geysermc.globallinkserver.java.JavaPlayer;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.link.TempLink;
import org.geysermc.globallinkserver.player.Player;
import org.geysermc.globallinkserver.player.PlayerManager;

public class CommandUtils {
    public static void handleCommand(
            LinkManager linkManager,
            PlayerManager playerManager,
            Player player,
            String message) {

        String[] args = message.split(" ");

        if (args[0].equals("/linkaccount")) {
            if (args.length == 2) {
                int linkId = Utils.parseInt(args[1]);

                if (linkId <= 0) {
                    player.sendMessage("&cInvalid link code");
                    return;
                }

                TempLink tempLink = linkManager.getTempLink(linkId);

                if (tempLink == null) {
                    player.sendMessage("&cCould not find the provided link. Is it expired?");
                    return;
                }

                if (player instanceof JavaPlayer) {
                    tempLink.setJavaId(player.getUniqueId());
                    tempLink.setJavaUsername(player.getUsername());
                } else {
                    tempLink.setBedrockId(player.getUniqueId());
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

                    playerManager.kickPlayers(tempLink.getJavaId(), tempLink.getBedrockId(),
                            "&aYou are now successfully linked! :)");
                });
                return;
            }

            if (args.length == 1) {
                if (player.getLinkId() != 0) {
                    linkManager.removeTempLink(player.getLinkId());
                }

                String code = String.format("%04d", linkManager.createTempLink(player));

                String otherPlatform = player instanceof JavaPlayer ? "Bedrock" : "Java";

                player.sendMessage("&aPlease join on " + otherPlatform +
                        " and run `&9/linkaccount &3" + code + "&a`");
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
