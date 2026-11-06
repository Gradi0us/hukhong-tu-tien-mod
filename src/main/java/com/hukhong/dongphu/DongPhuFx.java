package com.hukhong.dongphu;

import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Hiệu ứng Động Phủ: lễ kích hoạt trận (vòng rune bán kính = vùng bảo hộ, cột sáng vừa phải, sóng chấn, chuông),
 *  ranh giới nhấp nháy ở mép vùng, linh khí bốc lên quanh lõi, bầu trời đỏ khi công phá. */
public final class DongPhuFx {
    private DongPhuFx() {}

    static final int JADE = 0x7CFFC4, GOLD = 0xFFD866, VIOLET = 0xB07CFF, RED = 0xFF2030;
    static final DustParticleOptions JADE_DUST = dust(JADE, 1.3f), RED_DUST = dust(RED, 2.2f);

    static DustParticleOptions dust(int rgb, float s) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), s);
    }

    /** Lễ kích hoạt: gọi khi lập trận và mỗi lần nâng tầng. */
    public static void activate(ServerPlayer p, ServerLevel lv, Vec3 c, int tier, float radius) {
        Vec3 ground = c.add(0, 0.05, 0);
        Fx.rune(lv, p, ground, radius, JADE, 100, 8 + tier);            // vòng rune = đúng bán kính bảo hộ
        Fx.groundRing(lv, p, ground, radius, GOLD, 60);
        Fx.play(lv, FxType.LIGHT_PILLAR, c, Vec3.ZERO, 0.5f + 0.08f * tier, JADE, 40); // vừa phải: cột sáng rất chói
        Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.1, 0), Vec3.ZERO, 2.0f + 0.4f * tier, JADE, 14);
        Fx.shake(lv, c, 0.2f + 0.05f * tier, 10, radius * 1.5f);
        lv.sendParticles(ModParticles.LINH_KHI.get(), c.x, c.y + 1, c.z, 40 + 10 * tier, radius * 0.45, 1.2, radius * 0.45, 0.08);
        lv.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 20 + 5 * tier, 0.4, 0.8, 0.4, 0.15);
        lv.playSound(null, BlockPos.containing(c), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.6f, 0.7f);
        lv.playSound(null, BlockPos.containing(c), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.9f);
        lv.playSound(null, BlockPos.containing(c), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.6f, 1.3f);
        pulse(lv, c, radius, 120, JADE);
    }

    // ---- ranh giới: vệt hạt quanh mép vùng, tắt dần
    private record Pulse(ServerLevel lv, Vec3 c, float r, int[] left, int rgb) {}
    private static final List<Pulse> PULSES = new ArrayList<>();

    public static void pulse(ServerLevel lv, Vec3 c, float r, int ticks, int rgb) { PULSES.add(new Pulse(lv, c, r, new int[]{ticks}, rgb)); }

    /** Gọi mỗi 4 tick từ DongPhuEvents. */
    public static void tick() {
        for (Iterator<Pulse> it = PULSES.iterator(); it.hasNext(); ) {
            Pulse p = it.next();
            p.left()[0] -= 4;
            if (p.left()[0] <= 0) { it.remove(); continue; }
            int n = 36;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                double x = p.c().x + Math.cos(a) * p.r(), z = p.c().z + Math.sin(a) * p.r();
                p.lv().sendParticles(dust(p.rgb(), 1.1f), x, p.c().y + 0.6, z, 1, 0, 0.15, 0, 0);
            }
        }
    }

    /** Bầu trời đỏ + hạt đỏ rơi từ trên cao quanh vùng; chỉ dùng khi công phá. */
    public static void redSky(ServerPlayer owner, ServerLevel lv, Vec3 c, float r) {
        Fx.play(lv, FxType.SCREEN_TINT, owner.position(), Vec3.ZERO, 0.5f, RED, 30);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48, d = r * (0.2 + 0.8 * ((i * 37) % 48) / 48.0);
            lv.sendParticles(RED_DUST, c.x + Math.cos(a) * d, c.y + 34, c.z + Math.sin(a) * d, 1, 0.4, 2, 0.4, 0);
        }
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 26, c.z, 60, r * 0.6, 3, r * 0.6, 0.01);
        lv.playSound(null, BlockPos.containing(c), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.9f, 0.6f);
    }
}
