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

import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v419.Bedrock_v419;
import org.cloudburstmc.protocol.bedrock.codec.v422.Bedrock_v422;
import org.cloudburstmc.protocol.bedrock.codec.v428.Bedrock_v428;
import org.cloudburstmc.protocol.bedrock.codec.v431.Bedrock_v431;
import org.cloudburstmc.protocol.bedrock.codec.v440.Bedrock_v440;
import org.cloudburstmc.protocol.bedrock.codec.v448.Bedrock_v448;
import org.cloudburstmc.protocol.bedrock.codec.v465.Bedrock_v465;
import org.cloudburstmc.protocol.bedrock.codec.v471.Bedrock_v471;
import org.cloudburstmc.protocol.bedrock.codec.v475.Bedrock_v475;
import org.cloudburstmc.protocol.bedrock.codec.v486.Bedrock_v486;
import org.cloudburstmc.protocol.bedrock.codec.v503.Bedrock_v503;
import org.cloudburstmc.protocol.bedrock.codec.v527.Bedrock_v527;
import org.cloudburstmc.protocol.bedrock.codec.v534.Bedrock_v534;
import org.cloudburstmc.protocol.bedrock.codec.v544.Bedrock_v544;
import org.cloudburstmc.protocol.bedrock.codec.v545.Bedrock_v545;
import org.cloudburstmc.protocol.bedrock.codec.v554.Bedrock_v554;
import org.cloudburstmc.protocol.bedrock.codec.v557.Bedrock_v557;
import org.cloudburstmc.protocol.bedrock.codec.v560.Bedrock_v560;
import org.cloudburstmc.protocol.bedrock.codec.v567.Bedrock_v567;
import org.cloudburstmc.protocol.bedrock.codec.v568.Bedrock_v568;
import org.cloudburstmc.protocol.bedrock.codec.v575.Bedrock_v575;
import org.cloudburstmc.protocol.bedrock.codec.v582.Bedrock_v582;

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
    public static final BedrockCodec LATEST_CODEC = Bedrock_v582.CODEC;
    /**
     * A list of all supported Bedrock versions that can join Geyser
     */
    public static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    static {
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v419.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v422.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v428.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v431.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v440.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v448.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v465.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v471.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v475.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v486.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v503.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v527.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v534.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v544.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v545.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v554.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v557.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v560.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v567.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v568.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(Bedrock_v575.CODEC);
        SUPPORTED_BEDROCK_CODECS.add(LATEST_CODEC);
    }

    /**
     * Gets the {@link BedrockCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static BedrockCodec getBedrockCodec(int protocolVersion) {
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
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
