package com.hukhong.dodge;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Cấu hình (COMMON) Thể Lực / Lách Né. File: hukhong-dodge.toml. */
public final class DodgeConfig {
    private DodgeConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLED = B.comment("Bật Lách Né (phím mặc định Left Alt)").define("dodgeEnabled", true);
    public static final ModConfigSpec.BooleanValue SPRINT_DRAIN = B.comment("Chạy nhanh tiêu Thể Lực").define("sprintDrain", true);
    public static final ModConfigSpec.BooleanValue PERFECT_DODGE = B.comment("Bật Né Hoàn Hảo (buff Thân Pháp)").define("perfectDodge", true);
    public static final ModConfigSpec SPEC = B.build();

    public static boolean enabled() { return !SPEC.isLoaded() || ENABLED.get(); }
    public static boolean sprintDrain() { return !SPEC.isLoaded() || SPRINT_DRAIN.get(); }
    public static boolean perfectDodge() { return !SPEC.isLoaded() || PERFECT_DODGE.get(); }
}
