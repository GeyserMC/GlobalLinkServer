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

import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import java.security.PublicKey;
import java.util.List;

public class Utils {

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

    public static ChainValidationResult.IdentityData validateData(List<String> certChainData, String clientDataJwt) throws Exception {
        ChainValidationResult result = EncryptionUtils.validateChain(certChainData);
        if (!result.signed()) {
            throw new IllegalArgumentException("Chain is not signed");
        }

        PublicKey identityPublicKey = result.identityClaims().parsedIdentityPublicKey();
        byte[] clientDataPayload = EncryptionUtils.verifyClientData(clientDataJwt, identityPublicKey);
        if (clientDataPayload == null) {
            throw new IllegalStateException("Client data isn't signed by the given chain data");
        }

        return result.identityClaims().extraData;
    }
}
