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

import com.github.steveice10.opennbt.tag.builtin.*;

import java.util.Map;

public class TagManager {
    public static CompoundTag getDimensionTag() {
        CompoundTag tag = new CompoundTag("");

        CompoundTag dimensionTypes = new CompoundTag("minecraft:dimension_type");
        dimensionTypes.put(new StringTag("type", "minecraft:dimension_type"));
        ListTag dimensionTag = new ListTag("value");
        CompoundTag overworldTag = convertToValue("minecraft:the_end", 0, getEndTag().getValue());
        dimensionTag.add(overworldTag);
        dimensionTypes.put(dimensionTag);
        tag.put(dimensionTypes);

        CompoundTag biomeTypes = new CompoundTag("minecraft:worldgen/biome");
        biomeTypes.put(new StringTag("type", "minecraft:worldgen/biome"));
        ListTag biomeTag = new ListTag("value");
        CompoundTag plainsTag = convertToValue("minecraft:plains", 0, getEndBiomeTag().getValue());
        biomeTag.add(plainsTag);
        biomeTypes.put(biomeTag);
        tag.put(biomeTypes);

        return tag;
    }

    public static CompoundTag getEndTag() {
        CompoundTag overworldTag = new CompoundTag("");
        overworldTag.put(new StringTag("name", "minecraft:the_end"));
        overworldTag.put(new ByteTag("piglin_safe", (byte) 0));
        overworldTag.put(new ByteTag("natural", (byte) 0));
        overworldTag.put(new FloatTag("ambient_light", 0f));
        overworldTag.put(new StringTag("infiniburn", "minecraft:infiniburn_end"));
        overworldTag.put(new ByteTag("respawn_anchor_works", (byte) 0));
        overworldTag.put(new ByteTag("has_skylight", (byte) 0));
        overworldTag.put(new ByteTag("bed_works", (byte) 0));
        overworldTag.put(new StringTag("effects", "minecraft:the_end"));
        overworldTag.put(new LongTag("fixed_time", 6000L));
        overworldTag.put(new ByteTag("has_raids", (byte) 1));
        overworldTag.put(new IntTag("logical_height", 256));
        overworldTag.put(new FloatTag("coordinate_scale", 1f));
        overworldTag.put(new ByteTag("ultrawarm", (byte) 0));
        overworldTag.put(new ByteTag("has_ceiling", (byte) 0));
        overworldTag.put(new IntTag("height", 256));
        overworldTag.put(new IntTag("min_y", 0));
        return overworldTag;
    }

    private static CompoundTag getEndBiomeTag() {
        CompoundTag plainsTag = new CompoundTag("");
        plainsTag.put(new StringTag("name", "minecraft:the_end"));
        plainsTag.put(new StringTag("precipitation", "none"));
        plainsTag.put(new FloatTag("depth", 0.1f));
        plainsTag.put(new FloatTag("temperature", 0.5f));
        plainsTag.put(new FloatTag("scale", 0.2f));
        plainsTag.put(new FloatTag("downfall", 0.5f));
        plainsTag.put(new StringTag("category", "the_end"));

        CompoundTag effects = new CompoundTag("effects");
        effects.put(new LongTag("sky_color", 0));
        effects.put(new LongTag("water_fog_color", 329011));
        effects.put(new LongTag("fog_color", 10518688));
        effects.put(new LongTag("water_color", 4159204));

        CompoundTag moodSound = new CompoundTag("mood_sound");
        moodSound.put(new IntTag("tick_delay", 6000));
        moodSound.put(new FloatTag("offset", 2.0f));
        moodSound.put(new StringTag("sound", "minecraft:ambient.cave"));
        moodSound.put(new IntTag("block_search_extent", 8));

        effects.put(moodSound);

        plainsTag.put(effects);

        return plainsTag;
    }

    private static CompoundTag convertToValue(String name, int id, Map<String, Tag> values) {
        CompoundTag tag = new CompoundTag(name);
        tag.put(new StringTag("name", name));
        tag.put(new IntTag("id", id));
        CompoundTag element = new CompoundTag("element");
        element.setValue(values);
        tag.put(element);

        return tag;
    }
}
