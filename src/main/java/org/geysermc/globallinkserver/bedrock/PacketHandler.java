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

package org.geysermc.globallinkserver.bedrock;

import com.google.gson.JsonObject;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.globallinkserver.bedrock.util.BedrockVersionUtils;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;
import org.geysermc.globallinkserver.util.CommandUtils;
import org.geysermc.globallinkserver.util.Utils;

public class PacketHandler implements BedrockPacketHandler {
    private final BedrockServerSession session;
    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private BedrockPlayer player;
    private long lastCommand;

    public PacketHandler(
            BedrockServerSession session,
            PlayerManager playerManager,
            LinkManager linkManager) {
        this.session = session;
        this.playerManager = playerManager;
        this.linkManager = linkManager;

        session.addDisconnectHandler(this::disconnect);
    }

    public void disconnect(DisconnectReason reason) {
        if (player != null) {
            playerManager.removeBedrockPlayer(player);
        }
    }

    @Override
    public boolean handle(LoginPacket packet) {
        BedrockPacketCodec packetCodec =
                BedrockVersionUtils.getBedrockCodec(packet.getProtocolVersion());

        if (packetCodec == null) {
            PlayStatusPacket status = new PlayStatusPacket();
            if (packet.getProtocolVersion() > packet.getProtocolVersion()) {
                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
            } else {
                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
            }
            session.sendPacket(status);
            session.disconnect();
            return true;
        }

        session.setPacketCodec(packetCodec);

        try {
            JsonObject extraData = Utils.validateData(
                    packet.getChainData().toString(),
                    packet.getSkinData().toString()
            );

            player = playerManager.addBedrockPlayer(session, extraData);

            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            session.sendPacket(status);

            ResourcePacksInfoPacket info = new ResourcePacksInfoPacket();
            session.sendPacket(info);
        } catch (AssertionError | Exception error) {
            session.disconnect("disconnect.loginFailed");
        }
        return true;
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket packet) {
        switch (packet.getStatus()) {
            case COMPLETED:
                player.sendStartGame();
                break;
            case HAVE_ALL_PACKS:
                ResourcePackStackPacket stack = new ResourcePackStackPacket();
                stack.setExperimentsPreviouslyToggled(false);
                stack.setForcedToAccept(false);
                stack.setGameVersion("*");
                session.sendPacket(stack);
                break;
            default:
                session.disconnect("disconnectionScreen.resourcePack");
                break;
        }
        return true;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        player.sendJoinMessages();
        return true;
    }

    @Override
    public boolean handle(CommandRequestPacket packet) {
        String message = packet.getCommand();
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
