package com.hukhong.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Tùy chọn client (màn Config của NeoForge): R26 giảm hiệu ứng toàn màn hình, particle, mù màu, HUD. */
public final class ClientConfig {
    private ClientConfig() {}

    public enum ParticleLevel { ALL, LOW, OFF }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue DISABLE_SCREEN_EFFECTS = B
            .comment("Tắt hiệu ứng toàn màn hình (rung camera, flash) - R26 accessibility")
            .translation("hukhong.configuration.disableScreenEffects").define("disableScreenEffects", false);
    public static final ModConfigSpec.EnumValue<ParticleLevel> PARTICLES = B
            .comment("Lượng particle của mod: ALL / LOW / OFF")
            .translation("hukhong.configuration.particles").defineEnum("particles", ParticleLevel.ALL);
    public static final ModConfigSpec.BooleanValue COLORBLIND = B
            .comment("Bảng màu thân thiện mù màu (Okabe-Ito) cho thanh tài nguyên/kinh nghiệm")
            .translation("hukhong.configuration.colorblind").define("colorblind", false);
    public static final ModConfigSpec.BooleanValue SHOW_HUD = B
            .comment("Hiện HUD tu tiên")
            .translation("hukhong.configuration.showHud").define("showHud", true);
    public static final ModConfigSpec SPEC = B.build();

    public static boolean screenEffectsOff() { return SPEC.isLoaded() && DISABLE_SCREEN_EFFECTS.get(); }
    public static ParticleLevel particles() { return SPEC.isLoaded() ? PARTICLES.get() : ParticleLevel.ALL; }
    public static boolean colorblind() { return SPEC.isLoaded() && COLORBLIND.get(); }
    public static boolean showHud() { return !SPEC.isLoaded() || SHOW_HUD.get(); }
}
