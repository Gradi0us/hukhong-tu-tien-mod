package com.hukhong.client.boss;

import com.hukhong.HuKhong;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Tô trời + mưa hạt theo lệnh server (biến đổi đấu trường, cutscene giải phóng). Đẩy vào hệ khí quyển có sẵn
 * ({@code AmbientClient}: tím Hư Hoá / đỏ chiến sự) qua {@link #corr()} và {@link #war()}; bản thân AmbientSky lo vẽ.
 * Giữ {@code hold} tick rồi tàn dần {@code fade} tick; server làm tươi định kỳ khi trận còn diễn ra.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class BossSkyFx {
    private BossSkyFx() {}

    private static float level, warMix, k;
    private static int rain, hold, fade, fadeTotal = 1;

    public static void set(float lv, float war, int rainKind, int holdTicks, int fadeTicks) {
        level = lv; warMix = war; rain = rainKind; hold = Math.max(0, holdTicks); fade = fadeTotal = Math.max(1, fadeTicks);
    }

    /** Mức tím Hư Hoá 0..1. */
    public static float corr() { return level * (1f - warMix) * k; }

    /** Mức đỏ chiến sự 0..1. */
    public static float war() { return level * warMix * k; }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { k = 0; hold = 0; fade = 0; return; }
        if (mc.isPaused()) return;
        if (hold > 0) { hold--; k = 1f; }
        else if (fade > 0) { fade--; k = fade / (float) fadeTotal; }
        else k = 0f;
        if (rain == 0 || k < 0.2f) return;
        ParticleOptions p = switch (rain) {
            case 1 -> ParticleTypes.FALLING_LAVA;
            case 3 -> ParticleTypes.FALLING_SPORE_BLOSSOM;
            default -> ParticleTypes.FALLING_OBSIDIAN_TEAR;
        };
        var rnd = mc.level.random;
        for (int i = 0; i < 8; i++)
            mc.level.addParticle(p, mc.player.getX() + rnd.nextGaussian() * 9, mc.player.getY() + 10 + rnd.nextDouble() * 4,
                    mc.player.getZ() + rnd.nextGaussian() * 9, 0, 0, 0);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { level = 0; k = 0; hold = 0; fade = 0; rain = 0; }
}
