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

package org.geysermc.globallinkserver.config;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigReader {
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Paths.get("./config.json");

    public static Config readConfig() {
        String data = getConfigContent();
        if (data == null) {
            createConfig();
        }
        data = getConfigContent();

        return GSON.fromJson(data, Config.class);
    }

    private static String getConfigContent() {
        try {
            return new String(Files.readAllBytes(CONFIG_PATH), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return null;
        }
    }

    private static void createConfig() {
        try {
            Files.copy(ConfigReader.class.getResourceAsStream("config.json"), CONFIG_PATH);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to copy config", exception);
        }
    }
}
