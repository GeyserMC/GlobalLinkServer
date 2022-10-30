/*
 * Copyright (c) 2021-2022 GeyserMC. http://geysermc.org
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

import com.nukkitx.network.util.EventLoops;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class NettyServer {
    private final EventLoopGroup group;
    private final ServerBootstrap bootstrap;

    private ChannelFuture future;

    public NettyServer(Supplier<BedrockPong> ponger, BedrockServerInitializer serverInitializer) {
        group = EventLoops.newEventLoopGroup(1);
        bootstrap = new ServerBootstrap()
                .channelFactory(RakChannelFactory.server(EventLoops.getChannelType().getDatagramChannel()))
                .option(RakChannelOption.RAK_ADVERTISEMENT, ponger.get().toByteBuf())
                .group(group)
                .childHandler(serverInitializer);
    }

    public ChannelFuture bind(InetSocketAddress address) {
        return future = bootstrap.bind(address);
    }

    public void shutdown() {
        group.shutdownGracefully();
        future.channel().closeFuture().syncUninterruptibly();
    }
}
