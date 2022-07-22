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

package org.geysermc.globallinkserver.java;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.RequiredArgsConstructor;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;
import org.geysermc.globallinkserver.util.CommandUtils;

@RequiredArgsConstructor
public class PacketHandler extends SessionAdapter {
    private final Session session;
    private final LinkManager linkManager;
    private final PlayerManager playerManager;

    private JavaPlayer player;
    private long lastCommand;

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            if (packet instanceof ServerboundChatCommandPacket) {
                long now = System.currentTimeMillis();
                if (now - lastCommand < 4_000) {
                    player.sendMessage("&cYou're sending commands too fast");
                    return;
                }
                lastCommand = now;
                String message = "/" + ((ServerboundChatCommandPacket) packet).getCommand();
                CommandUtils.handleCommand(linkManager, playerManager, player, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        try {
            GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);

            player = playerManager.addJavaPlayer(session, profile);
            player.sendJoinMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        if (player != null) {
            playerManager.removeJavaPlayer(player);
        }
    }
}
