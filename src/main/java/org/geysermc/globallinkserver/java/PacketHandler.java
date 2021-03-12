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

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.network.util.DisconnectReason;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.java.JavaServerSession;
import org.cloudburstmc.protocol.java.data.GameType;
import org.cloudburstmc.protocol.java.handler.JavaHandshakePacketHandler;
import org.cloudburstmc.protocol.java.handler.JavaPlayPacketHandler;
import org.cloudburstmc.protocol.java.packet.handshake.HandshakingPacket;
import org.cloudburstmc.protocol.java.packet.play.clientbound.LoginPacket;
import org.cloudburstmc.protocol.java.packet.play.clientbound.PlayerPositionPacket;
import org.cloudburstmc.protocol.java.packet.play.serverbound.ClientChatPacket;
import org.cloudburstmc.protocol.java.v754.Java_v754;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;
import org.geysermc.globallinkserver.util.CommandUtils;

@RequiredArgsConstructor
public class PacketHandler implements JavaPlayPacketHandler, JavaHandshakePacketHandler {
    private final JavaServerSession session;
    private final LinkManager linkManager;
    private final PlayerManager playerManager;

    private JavaPlayer player;
    private long lastCommand;

    public void login() {
        // call when the player disconnected
        session.getDisconnectHandlers().add(this::disconnected);

        LoginPacket loginPacket = new LoginPacket();

        loginPacket.setEntityId(0);
        loginPacket.setHardcore(false);
        loginPacket.setGameType(GameType.SPECTATOR);
        loginPacket.setPreviousGameType(GameType.SPECTATOR);
        loginPacket.setDimensions(new Key[]{Key.key("the_end")});
        loginPacket.setDimensionCodec(TagManager.getDimensionTag());
        loginPacket.setDimension(TagManager.getEndTag());
        loginPacket.setDimensionName(Key.key("the_end"));
        loginPacket.setSeedHash(100);
        loginPacket.setMaxPlayers(1);
        loginPacket.setChunkRadius(0);

        PlayerPositionPacket positionPacket = new PlayerPositionPacket();

        positionPacket.setPosition(Vector3d.from(0, 64, 0));
        positionPacket.setRotation(Vector2f.ZERO);

        session.sendPacket(loginPacket);
        session.sendPacket(positionPacket);

        player = playerManager.addJavaPlayer(session, session.getProfile());
        player.sendJoinMessages();
    }

    public void disconnected(DisconnectReason reason) {
        if (player != null) {
            playerManager.removeJavaPlayer(player);
        }
    }

    @Override
    public boolean handle(HandshakingPacket packet) {
        if (packet.getProtocolVersion() != Java_v754.V754_CODEC.getProtocolVersion()) {
            session.disconnect("You aren't running: " + Java_v754.V754_CODEC.getMinecraftVersion()
                    + ". Please use that version and try again");
        }
        return true;
    }

    @Override
    public boolean handle(ClientChatPacket packet) {
        String message = packet.getMessage();

        if (message.startsWith("/")) {
            long now = System.currentTimeMillis();
            if (now - lastCommand < 4_000) {
                player.sendMessage("&cYou're sending commands too fast");
                return true;
            }
            lastCommand = now;
            CommandUtils.handleCommand(linkManager, playerManager, player, message);
        } else {
            player.sendMessage("&7The darkness doesn't know how to respond to your message");
        }
        return true;
    }
}
