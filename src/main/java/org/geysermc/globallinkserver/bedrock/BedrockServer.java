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

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.v419.Bedrock_v419;
import lombok.RequiredArgsConstructor;
import org.geysermc.globallinkserver.Server;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class BedrockServer implements Server {
    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private com.nukkitx.protocol.bedrock.BedrockServer server;

    @Override
    public boolean startServer(Config config) {
        if (server != null) {
            return false;
        }

        InetSocketAddress bindAddress = new InetSocketAddress(config.getBindIp(),
                config.getBedrockPort());

        server = new com.nukkitx.protocol.bedrock.BedrockServer(bindAddress);

        BedrockPong pong = new BedrockPong();
        pong.setEdition("MCPE");
        pong.setMotd("Global Linking Server");
        pong.setPlayerCount(0);
        pong.setMaximumPlayerCount(1);
        pong.setGameType("Survival");
        pong.setIpv4Port(config.getBedrockPort());

        BedrockPacketCodec codec = Bedrock_v419.V419_CODEC;
        pong.setProtocolVersion(codec.getProtocolVersion());
        pong.setVersion(codec.getMinecraftVersion());

        server.setHandler(new BedrockServerEventHandler() {
            @Override
            public boolean onConnectionRequest(@Nonnull InetSocketAddress address) {
                return true;
            }

            @Override
            public BedrockPong onQuery(@Nonnull InetSocketAddress address) {
                return pong;
            }

            @Override
            public void onSessionCreation(@Nonnull BedrockServerSession session) {
                session.setPacketHandler(new PacketHandler(session, playerManager, linkManager));
            }
        });

        server.bind().join();
        return true;
    }

    @Override
    public void shutdown() {
        server.close();
        server = null;
    }
}
