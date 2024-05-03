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
package org.geysermc.globallinkserver.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.geysermc.globallinkserver.bedrock.BedrockPlayer;
import org.geysermc.globallinkserver.java.JavaPlayer;
import org.geysermc.mcprotocollib.network.Session;

public class PlayerManager {
    private final Map<String, JavaPlayer> javaPlayers = new HashMap<>();
    private final Map<String, BedrockPlayer> bedrockPlayers = new HashMap<>();

    public BedrockPlayer addBedrockPlayer(BedrockServerSession session, ChainValidationResult.IdentityData identity) {
        BedrockPlayer player = new BedrockPlayer(session, identity);

        BedrockPlayer old = bedrockPlayers.put(player.username(), player);
        if (old != null) {
            old.disconnect("You logged in from somewhere else");
        }

        return player;
    }

    public JavaPlayer addJavaPlayer(Session session, GameProfile gameProfile) {
        JavaPlayer player = new JavaPlayer(session, gameProfile);

        JavaPlayer old = javaPlayers.put(gameProfile.getName(), player);
        if (old != null) {
            old.disconnect("You logged in from somewhere else");
        }

        return player;
    }

    public void removeJavaPlayer(JavaPlayer player) {
        javaPlayers.remove(player.username(), player);
    }

    public void removeBedrockPlayer(BedrockPlayer player) {
        bedrockPlayers.remove(player.username(), player);
    }

    public List<Player> playersByTempLinkIds(IntSet removedTempLinks) {
        List<Player> players = new ArrayList<>();

        for (JavaPlayer player : javaPlayers.values()) {
            if (removedTempLinks.contains(player.linkId())) {
                players.add(player);
            }
        }
        for (BedrockPlayer player : bedrockPlayers.values()) {
            if (removedTempLinks.contains(player.linkId())) {
                players.add(player);
            }
        }

        return players;
    }

    public void kickPlayers(UUID javaPlayer, UUID bedrockPlayer, String reason) {
        for (JavaPlayer player : javaPlayers.values()) {
            if (player.uniqueId().equals(javaPlayer)) {
                player.disconnect(reason);
            }
        }
        for (BedrockPlayer player : bedrockPlayers.values()) {
            if (player.uniqueId().equals(bedrockPlayer)) {
                player.disconnect(reason);
            }
        }
    }
}
