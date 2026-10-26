package com.hukhong.cores;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Phẩm Lõi: Bạch → Hoàng → Tử → Hắc → Hồng → Kim (Thần Lõi). */
public enum CoreTier implements StringRepresentable {
    BACH("bach", 0xFFFFFF), HOANG("hoang", 0xE8C84A), TU("tu", 0xB060FF),
    HAC("hac", 0x6A6A7A), HONG("hong", 0xFF4040), KIM("kim", 0xFFD700);

    public static final Codec<CoreTier> CODEC = StringRepresentable.fromEnum(CoreTier::values);
    public final String id;
    public final int rgb;

    CoreTier(String id, int rgb) { this.id = id; this.rgb = rgb; }

    @Override
    public String getSerializedName() { return id; }

    public static CoreTier of(int ordinal) {
        CoreTier[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, ordinal))];
    }

    public String langKey() { return "hukhong.tier." + id; }
}
