/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import org.bukkit.entity.Player;

import java.util.UUID;

public class Link {
    private UUID bedrockId;
    private UUID javaId;
    private String javaUsername;
    private String bedrockUsername;

    public Link() {
    }

    public Link(Player javaPlayer) {
        this.javaId = javaPlayer.getUniqueId();
        this.javaUsername = javaPlayer.getName();
    }

    public UUID bedrockId() {
        return bedrockId;
    }

    public Link bedrockId(UUID bedrockId) {
        this.bedrockId = bedrockId;
        return this;
    }

    public UUID javaId() {
        return javaId;
    }

    public Link javaId(UUID javaId) {
        this.javaId = javaId;
        return this;
    }

    public String javaUsername() {
        return javaUsername;
    }

    public Link javaUsername(String javaUsername) {
        this.javaUsername = javaUsername;
        return this;
    }

    public String bedrockUsername() {
        return bedrockUsername;
    }

    public Link bedrockUsername(String bedrockUsername) {
        this.bedrockUsername = bedrockUsername;
        return this;
    }
}
