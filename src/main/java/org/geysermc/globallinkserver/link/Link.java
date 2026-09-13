/*
 * Copyright (c) 2025-2026 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Link(UUID javaId, String javaUsername, long bedrockId, String bedrockName) {
    public Link(UUID javaId, String javaUsername, UUID bedrockId, String bedrockName) {
        this(javaId, javaUsername, bedrockId.getLeastSignificantBits(), bedrockName);
    }

    public static Link fromRequest(LinkRequest left, UUID rightUuid, String rightUsername, boolean isLeftBedrock) {
        if (isLeftBedrock) {
            return new Link(rightUuid, rightUsername, left.requesterUuid(), left.requesterUsername());
        }
        return new Link(left.requesterUuid(), left.requesterUsername(), rightUuid, rightUsername);
    }
}
