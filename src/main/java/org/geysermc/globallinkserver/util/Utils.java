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
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.JSONValue;
import com.nukkitx.network.util.Preconditions;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;

import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Iterator;

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

    public static JsonObject validateData(String chainDataString, String skinDataString) throws Exception {
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

        if (!validateChainData(chainData)) {
            throw new AssertionError("Invalid chain data");
        }
        JWSObject jwsObject = JWSObject.parse(chainData.get(chainData.size() - 1).getAsString());
        JsonObject payload = GSON.fromJson(jwsObject.getPayload().toString(), JsonObject.class);

        JsonElement publicKey = payload.get("identityPublicKey");

        if (publicKey == null) {
            throw new AssertionError("Missing identity public key!");
        }
        ECPublicKey identityPublicKey = EncryptionUtils.generateKey(publicKey.getAsString());

        JWSObject skinData = JWSObject.parse(skinDataString);
        if (!EncryptionUtils.verifyJwt(skinData, identityPublicKey)) {
            throw new AssertionError("Invalid skin data");
        }

        JsonElement extraDataTemp = payload.get("extraData");

        // Make sure the client sent over the username, xuid and other info
        if (extraDataTemp == null || !extraDataTemp.isJsonObject()) {
            throw new AssertionError("Missing client data");
        }

        // Fetch the client data
        return extraDataTemp.getAsJsonObject();
    }

    private static boolean validateChainData(JsonArray data) throws Exception {
        if (data.size() != 3) {
            return false;
        }

        ECPublicKey lastKey = null;
        boolean mojangSigned = false;
        Iterator<JsonElement> iterator = data.iterator();
        while (iterator.hasNext()) {
            JsonElement node = iterator.next();
            JWSObject jwt = JWSObject.parse(node.getAsString());

            // x509 cert is expected in every claim
            URI x5u = jwt.getHeader().getX509CertURL();
            if (x5u == null) {
                return false;
            }

            ECPublicKey expectedKey = EncryptionUtils.generateKey(jwt.getHeader().getX509CertURL().toString());
            // First key is self-signed
            if (lastKey == null) {
                lastKey = expectedKey;
            } else if (!lastKey.equals(expectedKey)) {
                return false;
            }

            if (!EncryptionUtils.verifyJwt(jwt, lastKey)) {
                return false;
            }

            if (mojangSigned) {
                return !iterator.hasNext();
            }

            if (lastKey.equals(EncryptionUtils.getMojangPublicKey())) {
                mojangSigned = true;
            }

            Object payload = JSONValue.parse(jwt.getPayload().toString());
            Preconditions.checkArgument(payload instanceof JSONObject, "Payload is not an object");

            Object identityPublicKey = ((JSONObject) payload).get("identityPublicKey");
            Preconditions.checkArgument(identityPublicKey instanceof String, "identityPublicKey node is missing in chain");
            lastKey = EncryptionUtils.generateKey((String) identityPublicKey);
        }

        return mojangSigned;
    }
}
