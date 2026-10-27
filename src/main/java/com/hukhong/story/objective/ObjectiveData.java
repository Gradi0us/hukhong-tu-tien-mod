package com.hukhong.story.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Mục tiêu chính hiện tại. id rỗng = không có mục tiêu. */
public record ObjectiveData(String id, Component text, Optional<BlockPos> target, Optional<ResourceKey<Level>> dim) {
    public static final ObjectiveData EMPTY = new ObjectiveData("", Component.empty(), Optional.empty(), Optional.empty());

    public static final Codec<ObjectiveData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ObjectiveData::id),
            ComponentSerialization.CODEC.fieldOf("text").forGetter(ObjectiveData::text),
            BlockPos.CODEC.optionalFieldOf("target").forGetter(ObjectiveData::target),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dim").forGetter(ObjectiveData::dim)
    ).apply(i, ObjectiveData::new));

    public boolean isEmpty() { return id.isEmpty(); }
}
