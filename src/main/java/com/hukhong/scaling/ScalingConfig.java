package com.hukhong.scaling;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Cấu hình (COMMON) cho Kiếp Số / Huyết Ma / Tâm Tinh / Linh Tinh. */
public final class ScalingConfig {
    private ScalingConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLED = B.comment("Bật hệ thống Kiếp Số (độ khó tăng dần)").define("enabled", true);
    public static final ModConfigSpec.DoubleValue KIEP_PER_MINUTE = B.comment("Kiếp Số tăng mỗi phút chơi (cấp 1)").defineInRange("kiepPerMinute", 0.10, 0, 10);
    public static final ModConfigSpec.DoubleValue LEVEL_WEIGHT = B.comment("Hệ số theo cấp tu luyện: tốc độ x (1 + cấp x weight)").defineInRange("levelWeight", 0.04, 0, 1);
    public static final ModConfigSpec.DoubleValue MAX_KIEP = B.comment("Kiếp Số tối đa").defineInRange("maxKiep", 100.0, 1, 10000);
    public static final ModConfigSpec.DoubleValue DEATH_KEEP = B.comment("Khi chết, Kiếp Số nhân hệ số này (0.93 = giảm 7%)").defineInRange("deathKeep", 0.93, 0, 1);
    public static final ModConfigSpec.DoubleValue HP_PER_KIEP = B.comment("HP quái +x mỗi 1 Kiếp Số").defineInRange("hpPerKiep", 0.015, 0, 1);
    public static final ModConfigSpec.DoubleValue DMG_RATIO = B.comment("Sát thương quái tăng = ratio x phần tăng HP").defineInRange("dmgRatio", 0.4, 0, 2);
    public static final ModConfigSpec.DoubleValue MAX_MULT = B.comment("Trần hệ số HP quái từ Kiếp Số").defineInRange("maxMult", 3.0, 1, 50);
    public static final ModConfigSpec.IntValue RADIUS = B.comment("Bán kính tính Kiếp Số trung bình người chơi quanh quái (ô)").defineInRange("radius", 128, 16, 1024);

    public static final ModConfigSpec.DoubleValue HUYET_BASE = B.comment("Xác suất quái Hư thành Huyết Ma khi sinh").defineInRange("huyetMaChance", 0.02, 0, 1);
    public static final ModConfigSpec.DoubleValue HUYET_PER_KIEP = B.comment("Xác suất Huyết Ma cộng thêm mỗi Kiếp Số").defineInRange("huyetMaPerKiep", 0.0006, 0, 1);
    public static final ModConfigSpec.DoubleValue HUYET_MAX = B.comment("Trần xác suất Huyết Ma").defineInRange("huyetMaMaxChance", 0.12, 0, 1);
    public static final ModConfigSpec.DoubleValue HUYET_HP = B.comment("Huyết Ma: HP x").defineInRange("huyetMaHp", 2.5, 1, 50);
    public static final ModConfigSpec.DoubleValue HUYET_DMG = B.comment("Huyết Ma: sát thương x").defineInRange("huyetMaDmg", 1.5, 1, 50);
    public static final ModConfigSpec.DoubleValue HUYET_EXP = B.comment("Huyết Ma: EXP x").defineInRange("huyetMaExp", 3.0, 1, 50);

    public static final ModConfigSpec.DoubleValue HEART_PER_USE = B.comment("Tâm Tinh: máu tối đa vĩnh viễn +x mỗi viên").defineInRange("heartPerUse", 2.0, 0, 100);
    public static final ModConfigSpec.DoubleValue HEART_CAP = B.comment("Trần tổng máu cộng thêm từ Tâm Tinh").defineInRange("heartCap", 40.0, 0, 1000);
    public static final ModConfigSpec.DoubleValue POWER_PER_USE = B.comment("Linh Tinh: hukhong:attack_power vĩnh viễn +x mỗi viên").defineInRange("powerPerUse", 2.0, 0, 100);
    public static final ModConfigSpec.DoubleValue POWER_CAP = B.comment("Trần tổng attack_power cộng thêm từ Linh Tinh").defineInRange("powerCap", 40.0, 0, 1000);
    public static final ModConfigSpec.DoubleValue DROP_HEART = B.comment("Tỉ lệ rơi Tâm Tinh từ quái Hư thường").defineInRange("dropHeart", 0.008, 0, 1);
    public static final ModConfigSpec.DoubleValue DROP_POWER = B.comment("Tỉ lệ rơi Linh Tinh từ quái Hư thường").defineInRange("dropPower", 0.012, 0, 1);
    public static final ModConfigSpec.DoubleValue DROP_HEART_ELITE = B.comment("Tỉ lệ rơi Tâm Tinh từ Huyết Ma").defineInRange("dropHeartElite", 0.30, 0, 1);
    public static final ModConfigSpec.DoubleValue DROP_POWER_ELITE = B.comment("Tỉ lệ rơi Linh Tinh từ Huyết Ma").defineInRange("dropPowerElite", 0.40, 0, 1);

    public static final ModConfigSpec SPEC = B.build();

    static boolean on() { return !SPEC.isLoaded() || ENABLED.get(); }
    static double d(ModConfigSpec.DoubleValue v, double def) { return SPEC.isLoaded() ? v.get() : def; }
}
