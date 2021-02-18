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

package org.geysermc.globallinkserver.util;

import com.google.gson.*;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;

import java.security.interfaces.ECPublicKey;

public class Utils {
    private static final Gson GSON = new Gson();

    public static int parseInt(String toParse) {
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    public static long parseLong(String toParse) {
        try {
            return Long.parseLong(toParse);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    public static JsonObject validateData(String chainDataString, String skinDataString) {
        // Read the raw chain data
        JsonObject rawChainData;
        try {
            rawChainData = GSON.fromJson(chainDataString, JsonObject.class);
        } catch (JsonSyntaxException e) {
            throw new AssertionError("Unable to read chain data!");
        }

        // Get the parsed chain data
        JsonElement chainDataTemp = rawChainData.get("chain");
        if (chainDataTemp == null || !chainDataTemp.isJsonArray()) {
            throw new AssertionError("Invalid chain data!");
        }

        JsonArray chainData = chainDataTemp.getAsJsonArray();

        try {
            // Parse the signed jws object
            JWSObject jwsObject;
            jwsObject = JWSObject.parse(chainData.get(chainData.size() - 1).getAsString());

            // Read the JWS payload
            JsonObject payload = GSON.fromJson(jwsObject.getPayload().toString(), JsonObject.class);

            JsonElement publicKey = payload.get("identityPublicKey");

            // Check if the identityPublicKey is there
            if (publicKey == null) {
                throw new AssertionError("Missing identity public key!");
            }

            // Create an ECPublicKey from the identityPublicKey
            ECPublicKey identityPublicKey = EncryptionUtils.generateKey(publicKey.getAsString());

            // Get the skin data to validate the JWS token
            JWSObject skinData = JWSObject.parse(skinDataString);
            if (skinData.verify(new DefaultJWSVerifierFactory()
                    .createJWSVerifier(skinData.getHeader(), identityPublicKey))) {
                JsonElement extraDataTemp = payload.get("extraData");

                // Make sure the client sent over the username, xuid and other info
                if (extraDataTemp == null || !extraDataTemp.isJsonObject()) {
                    throw new AssertionError("Missing client data");
                }

                // Fetch the client data
                return extraDataTemp.getAsJsonObject();
            } else {
                throw new AssertionError("Invalid identity public key!");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to login", e);
        }
    }
}
