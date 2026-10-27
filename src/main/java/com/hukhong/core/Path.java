package com.hukhong.core;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Năm đạo tu luyện. Tập rỗng = chưa nhập đạo. */
public enum Path implements StringRepresentable {
    KIEM("kiem", 0xFF4FA8FF, "hukhong.resource.mana"),
    DAO("dao", 0xFFFFD34F, "hukhong.resource.holy"),
    THE("the", 0xFFE8572A, "hukhong.resource.aura"),
    MA("ma", 0xFF9B3FD6, "hukhong.resource.corrupt"),
    PHAP("phap", 0xFF3FD6C4, "hukhong.resource.phapluc");

    public static final Codec<Path> CODEC = StringRepresentable.fromEnum(Path::values);
    public final String id;
    public final int argb;
    public final String resourceKey;

    Path(String id, int argb, String resourceKey) {
        this.id = id;
        this.argb = argb;
        this.resourceKey = resourceKey;
    }

    @Override
    public String getSerializedName() { return id; }
}
