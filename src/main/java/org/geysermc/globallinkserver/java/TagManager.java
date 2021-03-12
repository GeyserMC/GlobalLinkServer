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

package org.geysermc.globallinkserver.java;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;

import java.util.Map;

public class TagManager {
    public static NbtMap getDimensionTag() {
        NbtMapBuilder tag = NbtMap.builder();

        NbtMapBuilder dimensionTypes = NbtMap.builder();
        dimensionTypes.putString("type", "minecraft:dimension_type");

        dimensionTypes.putList("value", NbtType.COMPOUND,
                convertToValue("minecraft:the_end", 0, getEndTag()));
        tag.putCompound("minecraft:dimension_type", dimensionTypes.build());

        NbtMapBuilder biomeTypes = NbtMap.builder();
        biomeTypes.putString("type", "minecraft:worldgen/biome");
        biomeTypes.putList("value", NbtType.COMPOUND,
                convertToValue("minecraft:plains", 0, getEndBiomeTag()));

        tag.putCompound("minecraft:worldgen/biome", biomeTypes.build());
        return tag.build();
    }

    public static NbtMap getEndTag() {
        NbtMapBuilder overworldTag = NbtMap.builder();
        overworldTag.putString("name", "minecraft:the_end");
        overworldTag.putByte("piglin_safe", (byte) 0);
        overworldTag.putByte("natural", (byte) 0);
        overworldTag.putFloat("ambient_light", 0f);
        overworldTag.putString("infiniburn", "minecraft:infiniburn_end");
        overworldTag.putByte("respawn_anchor_works", (byte) 0);
        overworldTag.putByte("has_skylight", (byte) 0);
        overworldTag.putByte("bed_works", (byte) 0);
        overworldTag.putString("effects", "minecraft:the_end");
        overworldTag.putLong("fixed_time", 6000L);
        overworldTag.putByte("has_raids", (byte) 1);
        overworldTag.putInt("logical_height", 256);
        overworldTag.putFloat("coordinate_scale", 1f);
        overworldTag.putByte("ultrawarm", (byte) 0);
        overworldTag.putByte("has_ceiling", (byte) 0);
        return overworldTag.build();
    }

    private static NbtMap getEndBiomeTag() {
        NbtMapBuilder plainsTag = NbtMap.builder();
        plainsTag.putString("name", "minecraft:the_end");
        plainsTag.putString("precipitation", "none");
        plainsTag.putFloat("depth", 0.1f);
        plainsTag.putFloat("temperature", 0.5f);
        plainsTag.putFloat("scale", 0.2f);
        plainsTag.putFloat("downfall", 0.5f);
        plainsTag.putString("category", "the_end");

        NbtMapBuilder effects = NbtMap.builder();
        effects.putLong("sky_color", 0);
        effects.putLong("water_fog_color", 329011);
        effects.putLong("fog_color", 10518688);
        effects.putLong("water_color", 4159204);
        plainsTag.putCompound("effects", effects.build());

        NbtMapBuilder moodSound = NbtMap.builder();
        moodSound.putInt("tick_delay", 6000);
        moodSound.putFloat("offset", 2.0f);
        moodSound.putString("sound", "minecraft:ambient.cave");
        moodSound.putInt("block_search_extent", 8);

        effects.putCompound("mood_sound", moodSound.build());

        return plainsTag.build();
    }

    private static NbtMap convertToValue(String name, int id, Map<String, Object> values) {
        NbtMapBuilder tag = NbtMap.builder();
        tag.putString("name", name);
        tag.putInt("id", id);

        NbtMapBuilder element = NbtMap.builder();
        element.putAll(values);

        tag.putCompound("element", element.build());
        return tag.build();
    }
}
