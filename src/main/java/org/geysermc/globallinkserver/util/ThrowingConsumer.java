/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import java.sql.SQLException;

@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T t) throws SQLException;
}
