package com.hukhong.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Tùy chọn client (màn Config của NeoForge): R26 giảm hiệu ứng toàn màn hình, particle, mù màu, HUD. */
public final class ClientConfig {
    private ClientConfig() {}

    public enum ParticleLevel { ALL, LOW, OFF }

    public enum FxQuality { LOW, MED, HIGH }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue DISABLE_SCREEN_EFFECTS = B
            .comment("Tắt hiệu ứng toàn màn hình (rung camera, flash) - R26 accessibility")
            .translation("hukhong.configuration.disableScreenEffects").define("disableScreenEffects", false);
    public static final ModConfigSpec.EnumValue<ParticleLevel> PARTICLES = B
            .comment("Lượng particle của mod: ALL / LOW / OFF")
            .translation("hukhong.configuration.particles").defineEnum("particles", ParticleLevel.ALL);
    public static final ModConfigSpec.EnumValue<FxQuality> FX_QUALITY = B
            .comment("Chất lượng hiệu ứng kỹ năng/Lôi Kiếp: LOW / MED / HIGH")
            .translation("hukhong.configuration.fxQuality").defineEnum("fxQuality", FxQuality.HIGH);
    public static final ModConfigSpec.BooleanValue COLORBLIND = B
            .comment("Bảng màu thân thiện mù màu (Okabe-Ito) cho thanh tài nguyên/kinh nghiệm")
            .translation("hukhong.configuration.colorblind").define("colorblind", false);
    public static final ModConfigSpec.BooleanValue SHOW_HUD = B
            .comment("Hiện HUD tu tiên")
            .translation("hukhong.configuration.showHud").define("showHud", true);
    public enum MinimapShape { ROUND, SQUARE }
    public static final ModConfigSpec.BooleanValue SHOW_MINIMAP = B
            .comment("Hiện minimap góc phải trên (phím M đổi cỡ, N mở bản đồ lớn, B ping thành/tông gần nhất)")
            .translation("hukhong.configuration.showMinimap").define("showMinimap", true);
    public static final ModConfigSpec.EnumValue<MinimapShape> MINIMAP_SHAPE = B
            .comment("Hình dạng minimap: ROUND / SQUARE")
            .translation("hukhong.configuration.minimapShape").defineEnum("minimapShape", MinimapShape.ROUND);
    public static final ModConfigSpec.BooleanValue COMPACT_HEALTH = B
            .comment("Thanh máu xếp lớp gọn khi máu tối đa lớn (thay vì hàng tim vanilla)")
            .translation("hukhong.configuration.compactHealth").define("compactHealth", true);
    public static final ModConfigSpec.BooleanValue COMPACT_NOTICES = B
            .comment("Thông báo (title/subtitle/actionbar) hiển thị dạng banner gọn, tự xuống dòng")
            .translation("hukhong.configuration.compactNotices").define("compactNotices", true);
    public static final ModConfigSpec SPEC = B.build();

    public static boolean screenEffectsOff() { return SPEC.isLoaded() && DISABLE_SCREEN_EFFECTS.get(); }
    public static ParticleLevel particles() { return SPEC.isLoaded() ? PARTICLES.get() : ParticleLevel.ALL; }
    public static FxQuality fxQuality() { return SPEC.isLoaded() ? FX_QUALITY.get() : FxQuality.HIGH; }
    public static boolean colorblind() { return SPEC.isLoaded() && COLORBLIND.get(); }
    public static boolean showHud() { return !SPEC.isLoaded() || SHOW_HUD.get(); }
    public static boolean minimapOn() { return !SPEC.isLoaded() || SHOW_MINIMAP.get(); }
    public static MinimapShape minimapShape() { return SPEC.isLoaded() ? MINIMAP_SHAPE.get() : MinimapShape.ROUND; }
    public static boolean compactHealth() { return !SPEC.isLoaded() || COMPACT_HEALTH.get(); }
    public static boolean compactNotices() { return !SPEC.isLoaded() || COMPACT_NOTICES.get(); }
}
