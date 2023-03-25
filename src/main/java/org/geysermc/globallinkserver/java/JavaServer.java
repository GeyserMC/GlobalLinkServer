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

import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.data.game.command.CommandParser;
import com.github.steveice10.mc.protocol.data.game.command.CommandType;
import com.github.steveice10.mc.protocol.data.game.command.properties.IntegerProperties;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.tcp.TcpServer;
import com.nukkitx.math.vector.Vector3i;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.zip.GZIPInputStream;

import static com.github.steveice10.mc.protocol.codec.MinecraftCodec.CODEC;

@RequiredArgsConstructor
public class JavaServer implements org.geysermc.globallinkserver.Server {
    private static final CompoundTag REGISTRY_CODEC = loadRegistryCodec();

    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private final ServerStatusInfo pong = new ServerStatusInfo(
            new VersionInfo(CODEC.getMinecraftVersion(), CODEC.getProtocolVersion()),
            new PlayerInfo(1, 0, Collections.emptyList()),
            Component.text("Global Link Server"),
            null,
            false);

    private Server server;

    @Override
    public boolean startServer(Config config) {
        if (server != null) {
            return false;
        }

        server = new TcpServer(config.getBindIp(), config.getJavaPort(), MinecraftProtocol::new);

        server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, true);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY,
                (ServerInfoBuilder) session -> pong);
        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY,
                (ServerLoginHandler) session -> {

                    session.send(new ClientboundCommandsPacket(
                            new CommandNode[]{
                                    new CommandNode(CommandType.ROOT, true, new int[]{1, 3}, OptionalInt.empty(), null, null, null, null),
                                    new CommandNode(CommandType.LITERAL, true, new int[]{2}, OptionalInt.empty(), "linkaccount", null, null, null),
                                    new CommandNode(CommandType.ARGUMENT, true, new int[0], OptionalInt.empty(), "code", CommandParser.INTEGER, new IntegerProperties(0, 9999), null),
                                    new CommandNode(CommandType.LITERAL, true, new int[0], OptionalInt.empty(), "unlinkaccount", null, null, null)
                            },
                            0
                    ));

                    session.send(new ClientboundLoginPacket(
                            0,
                            false,
                            GameMode.SPECTATOR,
                            GameMode.SPECTATOR,
                            new String[]{"minecraft:the_end"},
                            REGISTRY_CODEC,
                            "minecraft:the_end",
                            "minecraft:the_end",
                            100,
                            1,
                            0,
                            0,
                            false,
                            false,
                            false,
                            false,
                            null
                    ));

                    session.send(new ClientboundPlayerAbilitiesPacket(false, false, true, false, 0f, 0f));

                    // without this the player will spawn only after waiting 30 seconds
                    // there are multiple options to fix that,
                    // but this is the best option as we don't want to send chunk and the player is in spectator anyway
                    session.send(new ClientboundSetHealthPacket(0, 0, 0));

                    // this packet is also required to let our player spawn,
                    // but the location itself doesn't appear to be used
                    session.send(new ClientboundSetDefaultSpawnPositionPacket(Vector3i.ZERO, 0));

                    // Manually call the connect event
                    session.callEvent(new ConnectedEvent(session));
                });
        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256); // default

        server.addListener(new ServerAdapter() {
            @Override
            public void serverClosed(ServerClosedEvent event) {
                super.serverClosed(event);
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                event.getSession().addListener(new PacketHandler(event.getSession(), linkManager, playerManager));
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

    public static CompoundTag loadRegistryCodec() {
        try (InputStream inputStream = JavaServer.class.getClassLoader().getResourceAsStream("registry_codec.nbt");
             DataInputStream stream = new DataInputStream(new GZIPInputStream(inputStream))) {
            return (CompoundTag) NBTIO.readTag((DataInput) stream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Unable to load login registry.");
        }
    }
}
