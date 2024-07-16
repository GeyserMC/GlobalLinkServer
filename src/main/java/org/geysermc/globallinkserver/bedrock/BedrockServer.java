/*
 * Copyright (c) 2021-2023 GeyserMC
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
package org.geysermc.globallinkserver.bedrock;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;

import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.geysermc.globallinkserver.Server;
import org.geysermc.globallinkserver.bedrock.util.BedrockVersionUtils;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;

public class BedrockServer implements Server {
    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private NettyServer server;

    public BedrockServer(PlayerManager playerManager, LinkManager linkManager) {
        this.playerManager = playerManager;
        this.linkManager = linkManager;
    }

    @Override
    public boolean startServer(Config config) {
        if (server != null) {
            return false;
        }

        BedrockCodec latestCodec = BedrockVersionUtils.LATEST_CODEC;
        server = new NettyServer(
                new BedrockPong()
                        .edition("MCPE")
                        .motd("Global Linking")
                        .subMotd("Server")
                        .playerCount(0)
                        .maximumPlayerCount(1)
                        .gameType("Survival")
                        .ipv4Port(config.bedrockPort())
                        .protocolVersion(latestCodec.getProtocolVersion())
                        .version(latestCodec.getMinecraftVersion())
                        .serverId(ThreadLocalRandom.current().nextLong()),
                new ServerInitializer());
        server.bind(new InetSocketAddress(config.bindIp(), config.bedrockPort()))
                .awaitUninterruptibly();
        return true;
    }

    @Override
    public void shutdown() {
        server.shutdown();
        server = null;
    }

    class ServerInitializer extends BedrockServerInitializer {
        @Override
        protected void initSession(BedrockServerSession session) {
            session.setPacketHandler(new PacketHandler(session, playerManager, linkManager));
        }
    }
}
