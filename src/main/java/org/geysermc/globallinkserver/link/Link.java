/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Link(UUID javaId, String javaUsername, long javaNameTimestamp, long bedrockId, String bedrockName, long bedrockNameTimestamp) {
    public Link(UUID javaId, String javaUsername, long javaNameTimestamp, UUID bedrockId, String bedrockName, long bedrockNameTimestamp) {
        this(javaId, javaUsername, javaNameTimestamp, bedrockId.getLeastSignificantBits(), bedrockName, bedrockNameTimestamp);
    }

    public static Link fromRequest(LinkRequest left, UUID rightUuid, String rightUsername, long rightNameTimestamp, boolean isLeftBedrock) {
        if (isLeftBedrock) {
            return new Link(rightUuid, rightUsername, rightNameTimestamp, left.requesterUuid(), left.requesterUsername(), left.nameTimestamp());
        }
        return new Link(left.requesterUuid(), left.requesterUsername(), left.nameTimestamp(), rightUuid, rightUsername, rightNameTimestamp);
    }
}
