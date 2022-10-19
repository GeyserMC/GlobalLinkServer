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

package org.geysermc.globallinkserver.bedrock.util;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v419.Bedrock_v419;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import com.nukkitx.protocol.bedrock.v428.Bedrock_v428;
import com.nukkitx.protocol.bedrock.v431.Bedrock_v431;
import com.nukkitx.protocol.bedrock.v440.Bedrock_v440;
import com.nukkitx.protocol.bedrock.v448.Bedrock_v448;
import com.nukkitx.protocol.bedrock.v465.Bedrock_v465;
import com.nukkitx.protocol.bedrock.v471.Bedrock_v471;
import com.nukkitx.protocol.bedrock.v475.Bedrock_v475;
import com.nukkitx.protocol.bedrock.v486.Bedrock_v486;
import com.nukkitx.protocol.bedrock.v503.Bedrock_v503;
import com.nukkitx.protocol.bedrock.v527.Bedrock_v527;
import com.nukkitx.protocol.bedrock.v534.Bedrock_v534;
import com.nukkitx.protocol.bedrock.v544.Bedrock_v544;
import com.nukkitx.protocol.bedrock.v554.Bedrock_v554;
import com.nukkitx.protocol.bedrock.v557.Bedrock_v557;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about the supported Bedrock protocols in Geyser.
 */
public class BedrockVersionUtils {
    /**
     * Default Bedrock codec that should act as a fallback. Should represent the latest available
     * release of the game that Geyser supports.
     */
    public static final BedrockPacketCodec LATEST_CODEC = Bedrock_v557.V557_CODEC;
    /**
     * A list of all supported Bedrock versions that can join Geyser
     */
    public static final List<BedrockPacketCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    static {
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v419.V419_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v422.V422_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v428.V428_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v431.V431_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v440.V440_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v448.V448_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v465.V465_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v471.V471_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v475.V475_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v486.V486_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v503.V503_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v527.V527_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v534.V534_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v544.V544_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v544.V544_CODEC.toBuilder()
                .minecraftVersion("1.19.21")
                .protocolVersion(545)
                .build());
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v554.V554_CODEC);
        SUPPORTED_BEDROCK_CODECS.add(LATEST_CODEC);
    }

    /**
     * Gets the {@link BedrockPacketCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static BedrockPacketCodec getBedrockCodec(int protocolVersion) {
        for (BedrockPacketCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }

    public static int getLatestProtocolVersion() {
        return LATEST_CODEC.getProtocolVersion();
    }
}
