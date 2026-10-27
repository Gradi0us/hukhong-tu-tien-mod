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
    public static final ModConfigSpec.BooleanValue AMBIENT_VFX = B
            .comment("Khí quyển thế giới (bầu trời/sương Hư Hóa, hạt, ánh sáng, tia sáng): bật/tắt")
            .translation("hukhong.configuration.ambientVfx").define("ambientVfx", true);
    public static final ModConfigSpec.BooleanValue AMBIENT_POST = B
            .comment("Shader hậu kỳ chỉnh màu nhẹ (giảm bão hòa, ám tím, vignette) khi vào vùng Hư Hóa/xâm lăng")
            .translation("hukhong.configuration.ambientPost").define("ambientPost", true);
    public static final ModConfigSpec.DoubleValue AMBIENT_INTENSITY = B
            .comment("Cường độ khí quyển 0..1.5 (1 = mặc định)")
            .translation("hukhong.configuration.ambientIntensity").defineInRange("ambientIntensity", 1.0, 0.0, 1.5);
    public static final ModConfigSpec.DoubleValue AMBIENT_VOLUME = B
            .comment("Âm lượng âm thanh khí quyển 0..1")
            .translation("hukhong.configuration.ambientVolume").defineInRange("ambientVolume", 1.0, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue ZONE_TITLES = B
            .comment("Hiện thẻ tên khu vực (Thành/Cảng/Tông Môn) khi bước vào")
            .translation("hukhong.configuration.zoneTitles").define("zoneTitles", true);
    public static final ModConfigSpec.BooleanValue MOB_BARS = B
            .comment("Thanh máu + HP/Max + cấp trên đầu quái gần (hiện khi bị đánh/ngắm)")
            .translation("hukhong.configuration.mobBars").define("mobBars", true);
    public static final ModConfigSpec.BooleanValue MOB_BARS_PLAYERS = B
            .comment("Hiện thanh máu cả trên người chơi khác")
            .translation("hukhong.configuration.mobBarsPlayers").define("mobBarsPlayers", false);
    public static final ModConfigSpec.BooleanValue MOB_BARS_ALWAYS = B
            .comment("Luôn hiện thanh máu của quái đã mất máu (không chỉ khi vừa bị đánh/ngắm)")
            .translation("hukhong.configuration.mobBarsAlways").define("mobBarsAlways", false);
    public static final ModConfigSpec.IntValue MOB_BAR_RANGE = B
            .comment("Tầm hiện thanh máu (block)")
            .translation("hukhong.configuration.mobBarRange").defineInRange("mobBarRange", 24, 4, 64);
    public static final ModConfigSpec.BooleanValue DAMAGE_NUMBERS = B
            .comment("Số sát thương/hồi máu nổi (bạo kích vàng to, kỹ năng theo màu đạo, hồi máu xanh lá)")
            .translation("hukhong.configuration.damageNumbers").define("damageNumbers", true);
    public static final ModConfigSpec.BooleanValue MINIMAP_MOBS = B
            .comment("Minimap: chấm đỏ quái thù địch gần, boss là đầu lâu to")
            .translation("hukhong.configuration.minimapMobs").define("minimapMobs", true);
    public static final ModConfigSpec SPEC = B.build();

    public static boolean mobBars() { return !SPEC.isLoaded() || MOB_BARS.get(); }
    public static boolean mobBarsPlayers() { return SPEC.isLoaded() && MOB_BARS_PLAYERS.get(); }
    public static boolean mobBarsAlways() { return SPEC.isLoaded() && MOB_BARS_ALWAYS.get(); }
    public static int mobBarRange() { return SPEC.isLoaded() ? MOB_BAR_RANGE.get() : 24; }
    public static boolean damageNumbers() { return !SPEC.isLoaded() || DAMAGE_NUMBERS.get(); }
    public static boolean minimapMobs() { return !SPEC.isLoaded() || MINIMAP_MOBS.get(); }

    public static boolean screenEffectsOff() { return SPEC.isLoaded() && DISABLE_SCREEN_EFFECTS.get(); }
    public static ParticleLevel particles() { return SPEC.isLoaded() ? PARTICLES.get() : ParticleLevel.ALL; }
    public static FxQuality fxQuality() { return SPEC.isLoaded() ? FX_QUALITY.get() : FxQuality.HIGH; }
    public static boolean colorblind() { return SPEC.isLoaded() && COLORBLIND.get(); }
    public static boolean showHud() { return !SPEC.isLoaded() || SHOW_HUD.get(); }
    public static boolean minimapOn() { return !SPEC.isLoaded() || SHOW_MINIMAP.get(); }
    public static MinimapShape minimapShape() { return SPEC.isLoaded() ? MINIMAP_SHAPE.get() : MinimapShape.ROUND; }
    public static boolean compactHealth() { return !SPEC.isLoaded() || COMPACT_HEALTH.get(); }
    public static boolean compactNotices() { return !SPEC.isLoaded() || COMPACT_NOTICES.get(); }

    public static boolean ambientVfx() { return !SPEC.isLoaded() || AMBIENT_VFX.get(); }
    public static boolean ambientPost() { return !SPEC.isLoaded() || AMBIENT_POST.get(); }
    public static float ambientIntensity() { return SPEC.isLoaded() ? AMBIENT_INTENSITY.get().floatValue() : 1f; }
    public static float ambientVolume() { return SPEC.isLoaded() ? AMBIENT_VOLUME.get().floatValue() : 1f; }
    public static boolean zoneTitles() { return !SPEC.isLoaded() || ZONE_TITLES.get(); }
}
