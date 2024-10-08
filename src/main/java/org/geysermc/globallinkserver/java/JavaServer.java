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

import static org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec.CODEC;

import java.util.BitSet;
import java.util.Collections;
import java.util.OptionalInt;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.event.server.ServerAdapter;
import org.geysermc.mcprotocollib.network.event.server.ServerClosedEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionAddedEvent;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.tcp.TcpServer;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.ServerLoginHandler;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandParser;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.IntegerProperties;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.LightUpdateData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.status.PlayerInfo;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.VersionInfo;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoBuilder;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;

public class JavaServer implements org.geysermc.globallinkserver.Server {
    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private final ClientboundLevelChunkWithLightPacket cachedChunk = cachedChunk();

    private final ServerStatusInfo pong = new ServerStatusInfo(
            Component.text("Global Link Server"),
            new PlayerInfo(1, 0, Collections.emptyList()),
            new VersionInfo(CODEC.getMinecraftVersion(), CODEC.getProtocolVersion()),
            null,
            false);

    private Server server;

    public JavaServer(PlayerManager playerManager, LinkManager linkManager) {
        this.playerManager = playerManager;
        this.linkManager = linkManager;
    }

    @Override
    public boolean startServer(Config config) {
        if (server != null) {
            return false;
        }

        server = new TcpServer(config.bindIp(), config.javaPort(), MinecraftProtocol::new);

        server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, true);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, session -> pong);
        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, session -> {
            session.send(new ClientboundCommandsPacket(
                    new CommandNode[] {
                        new CommandNode(
                                CommandType.ROOT, true, new int[] {1, 3}, OptionalInt.empty(), null, null, null, null),
                        new CommandNode(
                                CommandType.LITERAL,
                                true,
                                new int[] {2},
                                OptionalInt.empty(),
                                "linkaccount",
                                null,
                                null,
                                null),
                        new CommandNode(
                                CommandType.ARGUMENT,
                                true,
                                new int[0],
                                OptionalInt.empty(),
                                "code",
                                CommandParser.INTEGER,
                                new IntegerProperties(0, 9999),
                                null),
                        new CommandNode(
                                CommandType.LITERAL,
                                true,
                                new int[0],
                                OptionalInt.empty(),
                                "unlinkaccount",
                                null,
                                null,
                                null)
                    },
                    0));

            session.send(new ClientboundLoginPacket(
                    0,
                    false,
                    new Key[] {Key.key("minecraft:the_end")},
                    1,
                    0,
                    0,
                    false,
                    false,
                    false,
                    new PlayerSpawnInfo(
                            2,
                            Key.key("minecraft:the_end"),
                            100,
                            GameMode.SPECTATOR,
                            GameMode.SPECTATOR,
                            false,
                            false,
                            null,
                            100),
                    true));

            session.send(new ClientboundPlayerAbilitiesPacket(false, false, true, false, 0f, 0f));

            // this packet is also required to let our player spawn, but the location itself doesn't matter
            session.send(new ClientboundSetDefaultSpawnPositionPacket(Vector3i.ZERO, 0));

            // we have to listen to the teleport confirm on the PacketHandler to prevent respawn request packet spam,
            // so send it after calling ConnectedEvent which adds the PacketHandler as listener
            session.send(new ClientboundPlayerPositionPacket(0, 0, 0, 0, 0, 0));

            // these packet are required since 1.20.3
            session.send(new ClientboundGameEventPacket(GameEvent.LEVEL_CHUNKS_LOAD_START, null));
            session.send(cachedChunk);

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

    private ClientboundLevelChunkWithLightPacket cachedChunk() {
        // Based off https://github.com/Nan1t/NanoLimbo/pull/79
        byte[] sectionData = new byte[]{0, 0, 0, 0, 0, 0, 1, 0};
        var chunkData = new byte[sectionData.length * 16];
        var lastIndex = 0;
        for (int i = 0; i < 16; i++) {
            System.arraycopy(sectionData, 0, chunkData, lastIndex, sectionData.length);
            lastIndex += sectionData.length;
        }
        // new byte[] { 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 3, -1, -1, 0, 0 } but then decoded
        var lightData = new LightUpdateData(new BitSet(), new BitSet(), new BitSet(), BitSet.valueOf(new long[] {262143}), Collections.emptyList(), Collections.emptyList());
        var nbt = NbtMap.builder().putCompound("root", NbtMap.builder().putLongArray("MOTION_BLOCKING", new long[37]).build()).build();
        return new ClientboundLevelChunkWithLightPacket(0, 0, chunkData, nbt, new BlockEntityInfo[0], lightData);
    }
}
