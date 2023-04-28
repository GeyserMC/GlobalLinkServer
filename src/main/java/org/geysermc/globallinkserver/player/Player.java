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

package org.geysermc.globallinkserver.player;

import java.util.UUID;

public interface Player {
    UUID getUniqueId();

    String getUsername();

    int getLinkId();

    void setLinkId(int linkId);

    void sendMessage(String message);

    void disconnect(String reason);

    default void sendJoinMessages() {
        sendMessage("&eTo start the linking process run `&9/linkaccount&e` or run `&9/linkaccount &3<code>&e` to finish the process.");
        sendMessage("&eTo unlink your account (if it is linked) run `&9/unlinkaccount&e`.");
    }

    default String formatMessage(String message) {
        return message.replace("&", "§");
    }

    default String jsonFormatMessage(String message) {
        return "{\"text\": \"" + formatMessage(message) + "\"}";
    }
}
