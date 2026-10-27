package com.hukhong.cores;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Phẩm Lõi theo thứ tự (ordinal = bậc): Bạch → Hoàng → Tử → Hắc → Hồng → Thất Thải Quang → Cửu Thải → Kim (Thần Lõi, lv100).
 * Thất Thải / Cửu Thải chỉ ra ở mốc cấp 99; chỉ số = giá trị Hồng nhân {@link #statMul} (đã ghi sẵn trong JSON Lõi).
 */
public enum CoreTier implements StringRepresentable {
    BACH("bach", 0xFFFFFF, 1.0), HOANG("hoang", 0xE8C84A, 1.0), TU("tu", 0xB060FF, 1.0),
    HAC("hac", 0x6A6A7A, 1.0), HONG("hong", 0xFF4040, 1.0),
    THAT_THAI("that_thai", 0xFF7AE0, 1.5), CUU_THAI("cuu_thai", 0x9FF0FF, 2.25),
    KIM("kim", 0xFFD700, 1.0);

    public static final Codec<CoreTier> CODEC = StringRepresentable.fromEnum(CoreTier::values);
    public final String id;
    public final int rgb;
    /** Hệ số chỉ số so với giá trị Hồng (chỉ phẩm cầu vồng dùng khi JSON thiếu ô giá trị). */
    public final double statMul;

    CoreTier(String id, int rgb, double statMul) { this.id = id; this.rgb = rgb; this.statMul = statMul; }

    @Override
    public String getSerializedName() { return id; }

    public static CoreTier of(int ordinal) {
        CoreTier[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, ordinal))];
    }

    /** Phẩm cầu vồng: viền thẻ / chữ đổi màu theo thời gian (client). */
    public boolean rainbow() { return this == THAT_THAI || this == CUU_THAI; }

    public String langKey() { return "hukhong.tier." + id; }
}
