/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.link;

public class TempLink extends Link {
    private int code;
    private long expiryTime;

    public int code() {
        return code;
    }

    public void code(int code) {
        this.code = code;
    }

    public long expiryTime() {
        return expiryTime;
    }

    public void expiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }
}
