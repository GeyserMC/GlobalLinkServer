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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.java.JavaServerSession;
import org.cloudburstmc.protocol.java.data.profile.GameProfile;
import org.cloudburstmc.protocol.java.data.text.ChatPosition;
import org.cloudburstmc.protocol.java.packet.play.clientbound.ServerChatPacket;
import org.geysermc.globallinkserver.player.Player;

import java.util.UUID;

@RequiredArgsConstructor
public class JavaPlayer implements Player {
    private static final UUID SENDER = new UUID(0, 0);

    private final JavaServerSession session;
    private final GameProfile profile;
    @Getter @Setter
    private int linkId;

    @Override
    public void sendMessage(String message) {
        ServerChatPacket packet = new ServerChatPacket();
        packet.setPosition(ChatPosition.CHAT_BOX);
        packet.setMessage(Component.text(formatMessage(message)));
        packet.setSenderUuid(SENDER);

        session.sendPacket(packet);
    }

    @Override
    public void disconnect(String reason) {
        session.disconnect(reason);
    }

    @Override
    public UUID getUniqueId() {
        return profile.getId();
    }

    @Override
    public String getUsername() {
        return profile.getName();
    }
}
