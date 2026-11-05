package com.hukhong.phaptu;

import com.hukhong.skill.SkillScheduler;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxSfx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Toàn bộ hiệu ứng nhìn / nghe của Pháp Thổ R (Địa Thứ Lĩnh Vực) và T (Thiên Sơn Áp Đỉnh). Chỉ gửi gói hiệu ứng + hạt + âm thanh;
 * không đổi trạng thái trò chơi (logic ở {@link ThoService}). Mọi hiệu ứng đều do máy chủ phát nên không cần mã client riêng.
 */
final class ThoFx {
    private ThoFx() {}

    static final int COL = PhapElement.THO.rgb, DUST = 0xC49A5C, ROCK = 0x8B7B66, FIRE = 0xFF8A30;
    private static final BlockParticleOption STONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    private static final BlockParticleOption DRIPSTONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.defaultBlockState());
    private static final BlockParticleOption DIRT = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
    private static final BlockParticleOption GRAVEL_FALL = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState());
    private static final BlockParticleOption BLOOD_FALL = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.REDSTONE_BLOCK.defaultBlockState());
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.72f, 0.02f, 0.06f), 1.1f);

    // =============================================================== R: Địa Thứ Lĩnh Vực

    static void fieldStart(ServerLevel lv, ServerPlayer p, Vec3 c, float r) {
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), r * 1.15f, COL, 10, 70);
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), r, DUST, 22);
        Fx.rune(lv, p, c.add(0, 0.08, 0), r * 0.9f, COL, 40, 8);
        Fx.shake(lv, c, 0.7f, 10, 24f);
        lv.sendParticles(STONE, c.x, c.y + 0.5, c.z, 60, r * 0.5, 0.4, r * 0.5, 0.25);
        FxSfx.cast(lv, c, 1.3f, 0.6f, SoundEvents.DEEPSLATE_BREAK, SoundEvents.GRAVEL_BREAK, SoundEvents.ANVIL_LAND);
    }

    /** Làm tươi viền + vết nứt (gọi mỗi ~12 tick, bán kính hiện tại) và rắc bụi đá dọc viền. */
    static void fieldEdge(ServerLevel lv, ServerPlayer p, Vec3 c, float r, RandomSource rnd) {
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), r, DUST, 16);
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), r * 1.05f, COL, 8, 40);
        for (int i = 0; i < 10; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            Vec3 q = c.add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
            lv.sendParticles(STONE, q.x, q.y, q.z, 4, 0.3, 0.25, 0.3, 0.06);
        }
    }

    /** Một nhịp gai trồi khắp vùng: gai đá + mảnh nhũ đá + bụi đất tại các điểm ngẫu nhiên trong vùng. */
    static void fieldPulse(ServerLevel lv, ServerPlayer p, Vec3 c, float r, RandomSource rnd) {
        int n = Math.max(4, Math.min(12, Math.round(r * 1.5f)));
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, rr = r * Math.sqrt(rnd.nextDouble());
            Vec3 q = PhapSkills.ground(lv, c.add(Math.cos(a) * rr, 1.5, Math.sin(a) * rr));
            if (Math.abs(q.y - c.y) > 6) q = new Vec3(q.x, c.y, q.z);
            Fx.elem(lv, p, Fx.E_SPIKES, q, Vec3.ZERO, 0.9f + rnd.nextFloat() * 0.5f, i % 3 == 0 ? DUST : COL, 24);
            lv.sendParticles(DRIPSTONE, q.x, q.y + 0.4, q.z, 8, 0.25, 0.45, 0.25, 0.18);
            lv.sendParticles(STONE, q.x, q.y + 0.2, q.z, 5, 0.3, 0.2, 0.3, 0.1);
            if (i % 2 == 0) lv.sendParticles(DIRT, q.x, q.y + 0.1, q.z, 4, 0.35, 0.1, 0.35, 0.05);
        }
        PhapSkills.sound(lv, c, SoundEvents.POINTED_DRIPSTONE_LAND, 0.9f, 0.6f);
        PhapSkills.sound(lv, c, SoundEvents.GRAVEL_BREAK, 0.8f, 0.55f);
    }

    /** Gai đâm trúng một mục tiêu: gai ngay chân + máu bắn. */
    static void spikeHit(ServerLevel lv, ServerPlayer p, LivingEntity e) {
        Vec3 at = e.position();
        Fx.elem(lv, p, Fx.E_SPIKES, at, Vec3.ZERO, 0.8f, COL, 18);
        double y = e.getY() + e.getBbHeight() * 0.5;
        lv.sendParticles(DRIPSTONE, at.x, at.y + 0.3, at.z, 6, 0.2, 0.3, 0.2, 0.15);
        lv.sendParticles(BLOOD, at.x, y, at.z, 10, 0.25, 0.35, 0.25, 0.04);
    }

    /** Máu nhỏ giọt từ vết chảy máu. */
    static void bleedDrip(ServerLevel lv, LivingEntity e) {
        double y = e.getY() + e.getBbHeight() * 0.6;
        lv.sendParticles(BLOOD, e.getX(), y, e.getZ(), 3, 0.2, 0.3, 0.2, 0.0);
        lv.sendParticles(BLOOD_FALL, e.getX(), y, e.getZ(), 2, 0.2, 0.3, 0.2, 0.0);
    }

    /** Tay đá chộp kéo kẻ bay xuống: 3 dải đất trồi từ mặt đất lên thân rồi kéo ngược xuống + sỏi rơi + tiếng chộp. */
    static void grab(ServerLevel lv, ServerPlayer p, LivingEntity e, double groundY, boolean first) {
        Vec3 body = e.position().add(0, e.getBbHeight() * 0.5, 0);
        double base = e.getRandom().nextDouble() * Math.PI * 2;
        for (int k = 0; k < 3; k++) {
            double a = base + k * 2.094;
            Vec3 g = new Vec3(e.getX() + Math.cos(a) * 1.1, groundY + 0.05, e.getZ() + Math.sin(a) * 1.1);
            Fx.ribbonWave(lv, p, g, body, 0.5, 0.3, 6, k, 0.38f, ROCK);          // tay trồi lên
            Fx.ribbonWave(lv, p, body, g, -0.5, 0.3, 7, 6 + k, 0.5f, DUST);      // kéo xuống
            lv.sendParticles(STONE, g.x, g.y + 0.2, g.z, 6, 0.2, 0.15, 0.2, 0.08);
        }
        lv.sendParticles(GRAVEL_FALL, body.x, body.y, body.z, 14, 0.5, 0.5, 0.5, 0.0);
        lv.sendParticles(STONE, e.getX(), groundY + 0.2, e.getZ(), 12, 0.6, 0.2, 0.6, 0.15);
        if (first) {
            Fx.groundCrack(lv, p, new Vec3(e.getX(), groundY + 0.06, e.getZ()), 2.2f, COL, 6, 36);
            FxSfx.cast(lv, body, 1.0f, 0.7f, SoundEvents.GRAVEL_BREAK, SoundEvents.STONE_BREAK, SoundEvents.DEEPSLATE_BREAK);
        } else PhapSkills.sound(lv, body, SoundEvents.GRAVEL_BREAK, 0.7f, 0.6f);
    }

    static void fieldEnd(ServerLevel lv, ServerPlayer p, Vec3 c, float r) {
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), r, DUST, 14);
        lv.sendParticles(STONE, c.x, c.y + 0.5, c.z, 50, r * 0.45, 0.4, r * 0.45, 0.2);
        lv.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z, 20, r * 0.4, 0.2, r * 0.4, 0.03);
        PhapSkills.sound(lv, c, SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.5f);
    }

    // =============================================================== T: Thiên Sơn Áp Đỉnh

    /** Cột đá trồi: hai dải đất + cột hạt đá từ base lên h block (delay tick). */
    static void pillar(ServerLevel lv, ServerPlayer p, Vec3 base, double h, int delay, float w) {
        Fx.ribbon(lv, p, base.add(0, -0.4, 0), base.add(0, h, 0), 0, 6, delay, w, ROCK);
        Fx.ribbon(lv, p, base.add(0, -0.4, 0), base.add(0, h * 0.8, 0), 0, 7, delay + 1, w * 0.55f, COL);
        SkillScheduler.later(delay + 2, () -> {
            for (int i = 0; i <= h; i += 2) lv.sendParticles(STONE, base.x, base.y + i, base.z, 4, w * 0.6, 0.5, w * 0.6, 0.06);
            lv.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.x, base.y + h, base.z, 1, 0.2, 0.2, 0.2, 0.02);
        });
    }

    /** Tảng thiên thạch rơi từ trời xuống pt trong flight tick: đuôi lửa + khói + tảng đá lớn. */
    static void meteor(ServerLevel lv, ServerPlayer p, Vec3 pt, float size, int flight, double height) {
        Vec3 from = pt.add(2.5, height, -1.5);
        Fx.ribbon(lv, p, from, pt, 0, flight, 0, 0.55f * size / 3f, FIRE);
        Fx.ribbon(lv, p, from.add(0.6, 2, 0), pt, 0, flight, 1, 0.9f * size / 3f, COL);
        Fx.elem(lv, p, Fx.E_BOULDER, pt, Vec3.ZERO, size, COL, flight + 8);
        for (int k = 1; k <= 6; k++) {
            final double f = k / 6.0;
            SkillScheduler.later(Math.max(1, (int) (flight * f)), () -> {
                Vec3 q = from.lerp(pt, f);
                lv.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 6, 0.4, 0.4, 0.4, 0.02);
                lv.sendParticles(ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 4, 0.5, 0.5, 0.5, 0.01);
            });
        }
    }

    static void tStart(ServerLevel lv, ServerPlayer p, Vec3 c, float area) {
        Fx.rune(lv, p, c.add(0, 0.08, 0), area, COL, 50, 10);
        Fx.rune(lv, p, c.add(0, 18, 0), area * 1.3f, COL, 80, 12); // mắt trận trên trời
        Fx.telegraphCircle(lv, p, c.add(0, 0.05, 0), area, 20, COL);
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), area, COL, 8, 60);
        Fx.shake(lv, c, 0.6f, 16, 36f);
        FxSfx.cast(lv, c, 1.4f, 0.5f, SoundEvents.DEEPSLATE_BREAK, SoundEvents.ANVIL_LAND, SoundEvents.AMETHYST_CLUSTER_BREAK);
        PhapSkills.sound(lv, c, SoundEvents.WARDEN_AGITATED, 0.8f, 0.5f);
    }

    static void tTelegraph(ServerLevel lv, ServerPlayer p, Vec3 center, Vec3 pt, float impactR, int fall) {
        Fx.telegraphCircle(lv, p, pt.add(0, 0.05, 0), impactR, fall, COL);
        meteor(lv, p, pt, 4.6f, fall, 30);
        PhapSkills.sound(lv, center, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 0.5f);
        PhapSkills.sound(lv, pt, SoundEvents.FIRECHARGE_USE, 1.0f, 0.5f);
    }

    /** Một điểm rơi chạm đất: vết nứt, vòng sóng xung kích, cột đá trồi quanh và gai. */
    static void tLand(ServerLevel lv, ServerPlayer p, Vec3 pt, float impactR, int wave) {
        Fx.groundCrack(lv, p, pt.add(0, 0.06, 0), impactR + 1.2f, COL, 8, 60);
        Fx.groundRing(lv, p, pt.add(0, 0.06, 0), impactR, DUST, 18);
        Fx.play(lv, FxType.SHOCKWAVE, pt.add(0, 0.3, 0), Vec3.ZERO, impactR * 1.5f, COL, 18);
        Fx.elem(lv, p, Fx.E_SPIKES, pt, Vec3.ZERO, impactR, COL, 34);
        pillar(lv, p, pt, 9 + wave * 1.5, 0, 1.0f);
        for (int k = 0; k < 5; k++) {
            double a = k * 1.2566 + wave * 0.5;
            pillar(lv, p, pt.add(Math.cos(a) * impactR * 0.75, 0, Math.sin(a) * impactR * 0.75), 4.5 + (k % 3) * 2.2, 1 + k, 0.6f);
        }
        lv.sendParticles(STONE, pt.x, pt.y + 0.6, pt.z, 40, impactR * 0.4, 0.6, impactR * 0.4, 0.3);
        lv.sendParticles(ParticleTypes.EXPLOSION, pt.x, pt.y + 0.8, pt.z, 2, 0.8, 0.3, 0.8, 0);
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, pt.x, pt.y + 1, pt.z, 10, impactR * 0.4, 0.5, impactR * 0.4, 0.04);
        PhapSkills.sound(lv, pt, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 0.6f);
        PhapSkills.sound(lv, pt, SoundEvents.ANVIL_LAND, 0.9f, 0.5f);
    }

    static void tWaveShake(ServerLevel lv, Vec3 c, int wave) {
        Fx.shake(lv, c, 0.9f + 0.4f * wave, 14, 40f);
        FxSfx.cast(lv, c, 1.4f, 0.5f, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.ANVIL_LAND, SoundEvents.DEEPSLATE_BREAK);
    }

    static void tWindowOpen(ServerLevel lv, ServerPlayer p, Vec3 c, float blastR) {
        Fx.telegraphCircle(lv, p, c.add(0, 0.05, 0), blastR, 24, COL);
        Fx.rune(lv, p, c.add(0, 0.08, 0), blastR * 0.7f, COL, 120, 12);
        PhapSkills.sound(lv, c, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.5f);
        PhapSkills.sound(lv, c, SoundEvents.WARDEN_HEARTBEAT, 1.2f, 0.7f);
    }

    /** Nạp (giữ T): vòng đất co lại + rung + đá lăn quanh tâm. */
    static void tCharge(ServerLevel lv, ServerPlayer p, Vec3 c, float ringR) {
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), ringR, COL, 6);
        Fx.shake(lv, c, 0.6f, 4, 40f);
        lv.sendParticles(STONE, c.x, c.y + 0.5, c.z, 14, ringR * 0.5, 0.5, ringR * 0.5, 0.05);
        PhapSkills.sound(lv, c, SoundEvents.WARDEN_HEARTBEAT, 1.0f, 0.5f);
    }

    /** ĐỈNH ĐIỂM: thiên thạch khổng lồ, 3 vòng cột đá trồi lan ra, 3 sóng xung kích, mưa mảnh vỡ, rung màn hình và âm thanh nhiều lớp. */
    static void tDetonate(ServerLevel lv, ServerPlayer p, Vec3 c, float r) {
        RandomSource rnd = lv.random;
        // t+0: thiên thạch đập + loé trắng + nứt đất cả bán kính
        meteor(lv, p, c, 9f, 5, 44);
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), r + 2f, COL, 14, 100);
        Fx.groundCrack(lv, p, c.add(0, 0.07, 0), r * 0.6f, DUST, 9, 90);
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), r, 0xFFFFFF, 22);
        Fx.rune(lv, p, c.add(0, 0.08, 0), r * 0.8f, COL, 40, 12);
        Fx.elem(lv, p, Fx.E_SPIKES, c, Vec3.ZERO, r * 0.9f, COL, 50);
        Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.3, 0), Vec3.ZERO, r * 0.6f, 0xFFFFFF, 18);
        Fx.play(lv, FxType.SCREEN_TINT, c, Vec3.ZERO, 0.45f, 0xFFE9B0, 14);
        Fx.shake(lv, c, 3.2f, 34, 64f);
        lv.sendParticles(STONE, c.x, c.y + 1, c.z, 220, r * 0.4, 1.4, r * 0.4, 0.5);
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 3, 2.5, 0.5, 2.5, 0);
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1.5, c.z, 60, r * 0.3, 1.2, r * 0.3, 0.1);
        FxSfx.cast(lv, c, 2.2f, 0.4f, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.ANVIL_LAND, SoundEvents.DEEPSLATE_BREAK);
        PhapSkills.sound(lv, c, SoundEvents.WARDEN_SONIC_BOOM, 1.6f, 0.6f);
        // 3 vòng cột đá lan ra (trong, giữa, ngoài) + 3 sóng xung kích
        double[] ring = {0.35, 0.65, 1.0};
        int[] cnt = {8, 12, 16};
        for (int k = 0; k < 3; k++) {
            final int kk = k;
            SkillScheduler.later(3 + k * 3, () -> {
                double rr = r * ring[kk];
                for (int i = 0; i < cnt[kk]; i++) {
                    double a = Math.PI * 2 * i / cnt[kk] + kk * 0.37;
                    Vec3 q = PhapSkills.ground(lv, c.add(Math.cos(a) * rr, 2, Math.sin(a) * rr));
                    pillar(lv, p, q, 7 + rnd.nextInt(8) + (2 - kk) * 3, 0, 0.8f + (2 - kk) * 0.15f);
                }
                Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.3, 0), Vec3.ZERO, (float) (r * (0.5 + 0.5 * kk)), kk == 1 ? 0xFFFFFF : COL, 20);
                Fx.shake(lv, c, 1.6f, 12, 60f);
                PhapSkills.sound(lv, c, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.55f + 0.1f * kk);
                PhapSkills.sound(lv, c, SoundEvents.DEEPSLATE_BREAK, 1.4f, 0.5f);
            });
        }
        // t+14: mưa mảnh thiên thạch rơi quanh
        SkillScheduler.later(14, () -> {
            for (int i = 0; i < 8; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, rr = r * (0.2 + 0.7 * rnd.nextDouble());
                Vec3 q = PhapSkills.ground(lv, c.add(Math.cos(a) * rr, 2, Math.sin(a) * rr));
                meteor(lv, p, q, 3f, 8, 22);
                SkillScheduler.later(8, () -> {
                    Fx.groundCrack(lv, p, q.add(0, 0.06, 0), 3f, COL, 5, 40);
                    lv.sendParticles(STONE, q.x, q.y + 0.5, q.z, 14, 0.8, 0.5, 0.8, 0.25);
                    PhapSkills.sound(lv, q, SoundEvents.STONE_BREAK, 1.1f, 0.5f);
                });
            }
            Fx.shake(lv, c, 1.2f, 16, 60f);
        });
        // t+26: bụi lắng
        SkillScheduler.later(26, () -> {
            lv.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.5, c.z, 80, r * 0.45, 0.6, r * 0.45, 0.05);
            lv.sendParticles(DIRT, c.x, c.y + 0.5, c.z, 60, r * 0.4, 0.5, r * 0.4, 0.2);
            PhapSkills.sound(lv, c, SoundEvents.GRAVEL_BREAK, 1.4f, 0.5f);
        });
    }
}
