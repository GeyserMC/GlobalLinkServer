/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

@FunctionalInterface
public interface Object2BooleanFunction<T> {
    boolean apply(T t);
}
