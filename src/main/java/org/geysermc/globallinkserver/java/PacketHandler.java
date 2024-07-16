/*
 * Copyright (c) 2021-2024 GeyserMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.java;

import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;
import org.geysermc.globallinkserver.util.CommandUtils;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;

public class PacketHandler extends SessionAdapter {
    private final Session session;
    private final LinkManager linkManager;
    private final PlayerManager playerManager;

    private JavaPlayer player;
    private long lastCommand;

    public PacketHandler(Session session, LinkManager linkManager, PlayerManager playerManager) {
        this.session = session;
        this.linkManager = linkManager;
        this.playerManager = playerManager;
    }

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

            if (packet instanceof ServerboundAcceptTeleportationPacket) {
                // if we keep the health on 0, the client will spam us respawn request packets :/
                session.send(new ClientboundSetHealthPacket(1, 0, 0));
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
