/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import java.sql.SQLException;

@FunctionalInterface
public interface ThrowingFunction<T, R> {
    R apply(T t) throws SQLException;
}
