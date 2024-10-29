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
package org.geysermc.globallinkserver.bedrock.util;

import java.util.ArrayList;
import java.util.List;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685;
import org.cloudburstmc.protocol.bedrock.codec.v686.Bedrock_v686;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748;

/**
 * Contains information about the supported Bedrock protocols in GlobalLinkServer.
 */
public class BedrockVersionUtils {
    /**
     * A list of all supported Bedrock versions that can join GlobalLinkServer
     */
    public static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>() {{
        add(Bedrock_v589.CODEC);
        add(Bedrock_v594.CODEC);
        add(Bedrock_v618.CODEC);
        add(Bedrock_v622.CODEC);
        add(Bedrock_v630.CODEC);
        add(Bedrock_v649.CODEC);
        add(Bedrock_v662.CODEC);
        add(Bedrock_v671.CODEC);
        add(Bedrock_v685.CODEC);
        add(Bedrock_v686.CODEC);
        add(Bedrock_v712.CODEC);
        add(Bedrock_v729.CODEC);
        add(Bedrock_v748.CODEC);
    }};

    /**
     * Default Bedrock codec that should act as a fallback. Should represent the latest available
     * release of the game that GlobalLinkServer supports.
     */
    public static final BedrockCodec LATEST_CODEC = SUPPORTED_BEDROCK_CODECS.get(SUPPORTED_BEDROCK_CODECS.size() - 1);

    /**
     * Gets the {@link BedrockCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static BedrockCodec bedrockCodec(int protocolVersion) {
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }

    public static int latestProtocolVersion() {
        return LATEST_CODEC.getProtocolVersion();
    }
}
