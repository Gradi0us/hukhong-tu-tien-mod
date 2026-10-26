package com.hukhong.charcreate;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** 6 xuất thân (S2 §5). weight = tỉ lệ %. */
public enum Origin implements StringRepresentable {
    VILLAGE("village", 28), CITY("city", 22), BASTARD("bastard", 18),
    SECT("sect", 14), RUINS("ruins", 10), NOBLE("noble", 8);

    public static final Codec<Origin> CODEC = StringRepresentable.fromEnum(Origin::values);
    public final String id;
    public final int weight;

    Origin(String id, int weight) { this.id = id; this.weight = weight; }

    public String langKey() { return "hukhong.origin." + id; }

    @Override
    public String getSerializedName() { return id; }
}
