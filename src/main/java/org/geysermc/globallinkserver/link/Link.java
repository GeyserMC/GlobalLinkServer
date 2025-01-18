/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Link(UUID javaId, String javaUsername, long bedrockId) {
    public Link(UUID javaId, String javaUsername, UUID bedrockId) {
        this(javaId, javaUsername, bedrockId.getLeastSignificantBits());
    }

    public static Link fromRequest(LinkRequest left, UUID rightId, String rightName, boolean isLeftBedrock) {
        if (isLeftBedrock) {
            return new Link(rightId, rightName, left.requesterUuid());
        }
        return new Link(left.requesterUuid(), left.requesterUsername(), rightId);
    }
}
