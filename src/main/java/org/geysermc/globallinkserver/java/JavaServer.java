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

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.java.JavaEventHandler;
import org.cloudburstmc.protocol.java.JavaPacketCodec;
import org.cloudburstmc.protocol.java.JavaPong;
import org.cloudburstmc.protocol.java.JavaServerSession;
import org.cloudburstmc.protocol.java.v754.Java_v754;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;

import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class JavaServer implements org.geysermc.globallinkserver.Server {
    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private org.cloudburstmc.protocol.java.JavaServer server;

    @Override
    public boolean startServer(Config config) {
        if (server != null) {
            return false;
        }

        JavaPacketCodec codec = Java_v754.V754_CODEC;

        InetSocketAddress bindAddress = new InetSocketAddress(config.getBindIp(),
                config.getJavaPort());

        server = new org.cloudburstmc.protocol.java.JavaServer(bindAddress);

        // redundant, but makes it clear
        server.setHandleLogin(true);
        server.setOnlineMode(true);

        JavaPong pong = new JavaPong();
        pong.setDescription(Component.text("Global Link Server"));
        pong.setPlayers(new JavaPong.Players(1, 0));
        pong.setVersion(
                new JavaPong.Version(codec.getMinecraftVersion(), codec.getProtocolVersion()));

        server.setPong((ignored) -> pong);

        server.setHandler(new JavaEventHandler<JavaServerSession>() {
            @Override
            public void onSessionCreation(JavaServerSession session) {
                session.setPacketCodec(codec);
                session.setPacketHandler(new PacketHandler(session, linkManager, playerManager));
            }

            @Override
            public void onLogin(JavaServerSession session) {
                // is called after the player logged in

                PacketHandler handler = (PacketHandler) session.getPacketHandler();
                handler.login();

                // note to people when something doesn't seem to be called:
                // you probably have an error in your code that is suppressed,
                // so use a try and catch block manually
            }
        });

        server.bind();
        return true;
    }

    @Override
    public void shutdown() {
        server.close();
        server = null;
    }
}
