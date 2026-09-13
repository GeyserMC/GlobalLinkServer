/*
 * Copyright (c) 2021-2026 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class LinkRequest {
    private final int code;
    private final long expiryTime;
    private final UUID requesterUuid;
    private final String requesterUsername;

    public LinkRequest(int code, long ttl, UUID requesterUuid, String requesterUsername) {
        this.code = code;
        this.expiryTime = System.nanoTime() + ttl;
        this.requesterUuid = requesterUuid;
        this.requesterUsername = requesterUsername;
    }

    public @Nullable Player requester() {
        return Bukkit.getPlayer(requesterUuid);
    }

    public int code() {
        return code;
    }

    public long expiryTime() {
        return expiryTime;
    }

    public UUID requesterUuid() {
        return requesterUuid;
    }

    public String requesterUsername() {
        return requesterUsername;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LinkRequest) obj;
        return this.code == that.code &&
            this.expiryTime == that.expiryTime &&
            Objects.equals(this.requesterUuid, that.requesterUuid) &&
            Objects.equals(this.requesterUsername, that.requesterUsername);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, expiryTime, requesterUuid, requesterUsername);
    }

    @Override
    public String toString() {
        return "LinkRequest[" +
            "code=" + code + ", " +
            "expiryTime=" + expiryTime + ", " +
            "requesterUuid=" + requesterUuid + ", " +
            "requesterUsername=" + requesterUsername + ']';
    }

}
