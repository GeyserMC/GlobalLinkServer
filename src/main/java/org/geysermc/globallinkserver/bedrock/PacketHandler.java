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
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
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

    /**
     * In Protocol V554 and above, RequestNetworkSettingsPacket is sent before LoginPacket.
     */
    private boolean loginV554 = false;

    public PacketHandler(
            BedrockServerSession session,
            PlayerManager playerManager,
            LinkManager linkManager) {
        this.session = session;
        this.playerManager = playerManager;
        this.linkManager = linkManager;
    }

    @Override
    public void onDisconnect(String reason) {
        if (player != null) {
            playerManager.removeBedrockPlayer(player);
        }
    }

    private boolean setCorrectCodec(int protocolVersion) {
        BedrockCodec packetCodec = BedrockVersionUtils.getBedrockCodec(protocolVersion);
        if (packetCodec == null) {
            // Protocol version is not supported
            PlayStatusPacket status = new PlayStatusPacket();
            if (protocolVersion > BedrockVersionUtils.getLatestProtocolVersion()) {
                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
            } else {
                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
            }

            session.sendPacketImmediately(status);
            session.disconnect();
            return false;
        }

        session.setCodec(packetCodec);
        return true;
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        if (setCorrectCodec(packet.getProtocolVersion())) {
            loginV554 = true;
        } else {
            return PacketSignal.HANDLED; // Unsupported version, client has been disconnected
        }

        // New since 1.19.30 - sent before login packet
        PacketCompressionAlgorithm algorithm = PacketCompressionAlgorithm.ZLIB;

        NetworkSettingsPacket responsePacket = new NetworkSettingsPacket();
        responsePacket.setCompressionAlgorithm(algorithm);
        responsePacket.setCompressionThreshold(512);
        session.sendPacketImmediately(responsePacket);

        session.setCompression(algorithm);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        if (!loginV554) {
            // This is the first packet and compression has not been set yet
            if (!setCorrectCodec(packet.getProtocolVersion())) {
                return PacketSignal.HANDLED;
            }
        }

        try {
            JsonObject extraData = Utils.validateData(
                    packet.getChain(),
                    packet.getExtra().getParsedString()
            );

            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            session.sendPacket(status);

            ResourcePacksInfoPacket info = new ResourcePacksInfoPacket();
            session.sendPacket(info);

            player = playerManager.addBedrockPlayer(session, extraData);
        } catch (AssertionError | Exception error) {
            session.disconnect("disconnect.loginFailed");
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
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
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        player.sendJoinMessages();
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(CommandRequestPacket packet) {
        String message = packet.getCommand();
        if (message.startsWith("/")) {
            long now = System.currentTimeMillis();
            if (now - lastCommand < 4_000) {
                player.sendMessage("&cYou're sending commands too fast");
            } else {
                lastCommand = now;
                CommandUtils.handleCommand(linkManager, playerManager, player, message);
            }
        } else {
            player.sendMessage("&7The darkness doesn't know how to respond to your message");
        }
        return PacketSignal.HANDLED;
    }
}
