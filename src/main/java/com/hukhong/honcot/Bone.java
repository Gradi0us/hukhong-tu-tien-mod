package com.hukhong.honcot;

import java.util.List;
import java.util.Locale;

/**
 * 12 Hồn Cốt (S3 §5, #1..#12; #13 Vạn Cốt Cộng Minh là kỹ năng chủ động trên phím R).
 * Giá trị chuẩn = phẩm Hắc, roll 50. Thuần dữ liệu (không phụ thuộc Minecraft) để test được.
 * attr: "vanilla:xxx" hoặc tên attribute hukhong. mult=true -> ADD_MULTIPLIED_BASE, false -> ADD_VALUE.
 */
public enum Bone {
    THIEN_MUC(BoneSlot.HEAD, Fx.SEE_HOSTILES, new Stat("crit_chance", false, 0.08)),
    TINH_TAM(BoneSlot.HEAD, Fx.NONE, new Stat("resource_regen", false, 0.10), new Stat("defense", true, 0.04)),
    LONG_HON(BoneSlot.TORSO, Fx.LAST_STAND_SHIELD, new Stat("vanilla:generic.max_health", true, 0.15)),
    HU_KHONG_HACH(BoneSlot.TORSO, Fx.VOID_ABSORB, new Stat("defense", true, 0.10)),
    LOI_CHUONG(BoneSlot.LEFT_ARM, Fx.LIGHTNING_PALM, new Stat("attack_power", true, 0.08)),
    HO_THE(BoneSlot.LEFT_ARM, Fx.GUARD_REDUCE, new Stat("defense", true, 0.08)),
    HUYET_TRAO(BoneSlot.RIGHT_ARM, Fx.BLEED_CLAW, new Stat("lifesteal", false, 0.06)),
    KIEM_Y(BoneSlot.RIGHT_ARM, Fx.NONE, new Stat("attack_power", true, 0.10), new Stat("crit_damage", false, 0.15)),
    PHONG_HANH(BoneSlot.LEFT_LEG, Fx.NONE, new Stat("vanilla:generic.movement_speed", true, 0.12)),
    CU_TUONG(BoneSlot.LEFT_LEG, Fx.STOMP, new Stat("vanilla:generic.knockback_resistance", false, 1.0)),
    ANH_THIEM(BoneSlot.RIGHT_LEG, Fx.SHADOW_STEP, new Stat("vanilla:generic.movement_speed", true, 0.05)),
    HOI_SINH(BoneSlot.RIGHT_LEG, Fx.REVIVE, new Stat("vanilla:generic.max_health", true, 0.05));

    /** Kỹ năng bị động/tự kích hoạt gắn với Hồn Cốt (NONE = chỉ có chỉ số). */
    public enum Fx { NONE, SEE_HOSTILES, LAST_STAND_SHIELD, VOID_ABSORB, LIGHTNING_PALM, GUARD_REDUCE, BLEED_CLAW, STOMP, SHADOW_STEP, REVIVE }

    public record Stat(String attr, boolean mult, double base) {}

    public final BoneSlot slot;
    public final Fx fx;
    public final List<Stat> stats;

    Bone(BoneSlot slot, Fx fx, Stat... stats) {
        this.slot = slot;
        this.fx = fx;
        this.stats = List.of(stats);
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String itemId() { return "hon_cot_" + id(); }

    public String nameKey() { return "item.hukhong." + itemId(); }

    public String fxKey() { return "hukhong.honcot.fx." + id(); }

    public static Bone byId(String id) {
        if (id == null) return null;
        for (Bone b : values()) if (b.id().equals(id)) return b;
        return null;
    }
}
