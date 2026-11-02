package com.hukhong.tran;

import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * VFX Trận Tu, chỉ server. Hai lớp:
 * - sendParticles (dust vàng/lam/tím, END_ROD, ENCHANT...): nhìn thấy trong 32 block;
 * - Fx (client dựng decal/cột sáng/vỏ/rung, bền theo life tick): cùng đường đi với Pháp Tu/Đạo Tu.
 * Hàm persistent (cage, slowField, ward...) được gọi ~5 tick/lần; mỗi lần <= ~140 điểm hạt.
 * Vòng quay theo game time nên lặp lại vẫn thấy xoay. Không import client.*.
 */
final class TranVfx {
    private TranVfx() {}

    private static DustParticleOptions dust(float r, float g, float b, float s) {
        return new DustParticleOptions(new Vector3f(r, g, b), s);
    }

    private static final DustParticleOptions GOLD = dust(1f, 0.85f, 0.3f, 1.3f);
    private static final DustParticleOptions GOLD_HOT = dust(1f, 0.97f, 0.72f, 1.1f);
    private static final DustParticleOptions CYAN = dust(0.55f, 0.9f, 1f, 1.2f);
    private static final DustParticleOptions CYAN_PALE = dust(0.85f, 0.98f, 1f, 1f);
    private static final DustParticleOptions PURPLE = dust(0.62f, 0.3f, 1f, 1.3f);
    private static final DustParticleOptions GREY = dust(0.55f, 0.55f, 0.6f, 1.2f);
    private static final DustParticleOptions GREEN = dust(0.55f, 1f, 0.5f, 1.2f);
    private static final DustParticleOptions WHITE = dust(1f, 1f, 1f, 1f);

    // ------------------------------------------------------------ helpers hình học / hạt

    private static double spin(ServerLevel lv, double speed) {
        return lv.getGameTime() * speed;
    }

    private static Vec3 onRing(Vec3 c, double r, double y, double a) {
        return c.add(Math.cos(a) * r, y, Math.sin(a) * r);
    }

    /** Điểm trên mái vòm: phi = góc nâng từ mặt đất (rad), a = góc phương vị. */
    private static Vec3 onDome(Vec3 c, double r, double phi, double a) {
        double h = Math.cos(phi) * r;
        return c.add(Math.cos(a) * h, Math.sin(phi) * r, Math.sin(a) * h);
    }

    private static void dot(ServerLevel lv, DustParticleOptions o, Vec3 q) {
        lv.sendParticles(o, q.x, q.y, q.z, 1, 0, 0, 0, 0);
    }

    /** Vòng tròn ngang n điểm. */
    private static void ring(ServerLevel lv, Vec3 c, double r, double y, int n, double rot, DustParticleOptions o) {
        for (int i = 0; i < n; i++) dot(lv, o, onRing(c, r, y, rot + Math.PI * 2 * i / n));
    }

    /** Đa giác đều sides cạnh, mỗi cạnh steps điểm. */
    private static void poly(ServerLevel lv, Vec3 c, double r, double y, int sides, double rot, int steps, DustParticleOptions o) {
        Vec3[] v = new Vec3[sides];
        for (int k = 0; k < sides; k++) v[k] = onRing(c, r, y, rot + Math.PI * 2 * k / sides);
        for (int k = 0; k < sides; k++) {
            Vec3 a = v[k], b = v[(k + 1) % sides];
            for (int s = 0; s < steps; s++) dot(lv, o, a.lerp(b, (double) s / steps));
        }
    }

    /** Sao sáu cánh: hai tam giác chồng nhau, lệch pha. */
    private static void hexagram(ServerLevel lv, Vec3 c, double r, double y, double rot, int steps, DustParticleOptions o) {
        poly(lv, c, r, y, 3, rot - Math.PI / 2, steps, o);
        poly(lv, c, r, y, 3, rot + Math.PI / 2, steps, o);
    }

    private static Vec3 inDisc(ServerLevel lv, Vec3 c, double r) {
        double a = lv.getRandom().nextDouble() * Math.PI * 2;
        double d = Math.sqrt(lv.getRandom().nextDouble()) * r;
        return c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
    }

    private static void sound(ServerLevel lv, Vec3 c, SoundEvent e, float vol, float pitch) {
        lv.playSound(null, c.x, c.y, c.z, e, SoundSource.PLAYERS, vol, pitch);
    }

    private static void sound(ServerLevel lv, Vec3 c, Holder<SoundEvent> e, float vol, float pitch) {
        lv.playSound(null, c.x, c.y, c.z, e, SoundSource.PLAYERS, vol, pitch);
    }

    /** Âm thanh nền thưa cho hàm persistent: ~1/16 lần gọi. */
    private static boolean ambient(ServerLevel lv) {
        return lv.getRandom().nextInt(16) == 0;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // ------------------------------------------------------------ hàm cũ (TranSkills/TranRuntime đang gọi)

    /** Pháp trận: vòng vàng/lam xen kẽ xoay đều + sao sáu cánh lam + đốm enchant bay lên ở 6 điểm. */
    static void rune(ServerLevel lv, Vec3 c, double r) {
        double rot = spin(lv, 0.05);
        for (int i = 0; i < 36; i++) {
            Vec3 q = onRing(c, r, 0.05, rot + Math.PI * 2 * i / 36);
            lv.sendParticles(i % 2 == 0 ? GOLD : CYAN, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            if (i % 6 == 0) lv.sendParticles(ParticleTypes.ENCHANT, q.x, q.y + 0.4, q.z, 2, 0.05, 0.3, 0.05, 0.15);
        }
        ring(lv, c, r * 0.9, 0.05, 24, -rot * 1.5, GOLD_HOT);
        hexagram(lv, c, r * 0.7, 0.06, rot, 6, CYAN_PALE);
    }

    /**
     * Vòng phù văn bền theo tuổi trận: decal Fx (client vẽ suốt {@code life} tick) + vòng nền sáng lam.
     * Gọi một lần khi đặt trận; hạt {@link #rune} được vẽ lại định kỳ trong TranRuntime.
     */
    static void zoneDecal(ServerLevel lv, ServerPlayer owner, Vec3 c, double r, int life) {
        Fx.rune(lv, owner, c.add(0, 0.05, 0), (float) r, Fx.GOLD, life, 3);
        Fx.groundRing(lv, owner, c.add(0, 0.06, 0), (float) r, Fx.CYAN, Math.min(life, 24));
    }

    /** Nổ sáng khi đặt trận / đánh cận chiến: vòng dust vàng/lam nở + ENCHANT tản quanh tâm. */
    static void burst(ServerLevel lv, Vec3 at, double r) {
        int n = 40;
        for (int i = 0; i < n; i++) {
            Vec3 q = onRing(at, r, 0.1, Math.PI * 2 * i / n);
            lv.sendParticles(i % 2 == 0 ? GOLD_HOT : CYAN, q.x, q.y, q.z, 1, 0, 0.2, 0, 0.02);
        }
        lv.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1.0, at.z, 30, r * 0.5, 0.8, r * 0.5, 0.25);
    }

    /** Cột sáng vàng cao 12 block tại điểm chạm: Fx LIGHT_PILLAR lớn + hạt END_ROD xoắn + vòng sốc đất + tiếng sấm. */
    static void pillar(ServerLevel lv, Vec3 at) {
        Fx.play(lv, FxType.LIGHT_PILLAR, at, Vec3.ZERO, 1.6f, Fx.GOLD, 30);
        for (double y = 0; y <= 12; y += 0.6) {
            lv.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 1, 0.08, 0, 0.08, 0.01);
        }
        ring(lv, at, 2.5, 0.1, 32, spin(lv, 0.2), GOLD_HOT);
        lv.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.5, 0.2, 0.5, 0);
        lv.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1, at.z, 40, 0.6, 2, 0.6, 0.3);
        sound(lv, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
        sound(lv, at, SoundEvents.BEACON_POWER_SELECT, 1f, 1.2f);
    }

    /**
     * Hiệu ứng khi vận Sát Trận (gọi mỗi tick): pháp trận decal sáng nở dần theo r, vòng phù văn xoay, tia linh lực từ tay
     * người chơi xuống tâm, hạt tụ vào người, âm thanh cao dần; đầy lực thì chớp sáng.
     */
    static void chargeTick(ServerLevel lv, ServerPlayer p, Vec3 c, double r, double f, int held) {
        Vec3 g = c.add(0, 0.06, 0);
        if (held % 2 == 0) {
            Fx.rune(lv, p, g, (float) r, Fx.GOLD, 5, 3 + (int) Math.round(f * 5));
            Fx.groundRing(lv, p, g.add(0, 0.01, 0), (float) r, Fx.CYAN, 5);
        }
        if (held % 4 == 0) {
            rune(lv, g.add(0, 0.25, 0), r);
            ring(lv, c, r, 0.4 + 0.2 * Math.sin(held * 0.3), 48, spin(lv, 0.15), GOLD_HOT);
            Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.6)).subtract(0, 0.4, 0);
            Fx.ribbon(lv, p, hand, c.add(0, 0.5, 0), 0, 4, 0, 0.05f + 0.1f * (float) f, Fx.GOLD);
            lv.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.8, 0.8, 0.8, 0.6);
            for (int k = 0; k < 6; k++) {                       // cột hạt mảnh dọc theo mép trận
                double a = spin(lv, 0.1) + Math.PI * 2 * k / 6;
                Vec3 q = onRing(c, r, 0, a);
                lv.sendParticles(ParticleTypes.END_ROD, q.x, q.y + 0.2 + f * 2, q.z, 1, 0, 0.3, 0, 0.01);
            }
        }
        if (held % 10 == 0) sound(lv, p.position(), SoundEvents.BEACON_AMBIENT, 0.8f, 0.6f + (float) f);
        if (held == TranMath.CHARGE_TICKS) {
            lv.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
            sound(lv, c, SoundEvents.BEACON_POWER_SELECT, 1.2f, 1.6f);
        }
    }

    /** Cột sáng khổng lồ: trụ sáng lớn + vành sóng xung kích + rung màn hình. */
    static void giantPillar(ServerLevel lv, Vec3 at, double r) {
        giantPillar(lv, null, at, r);
    }

    /**
     * Cột sáng khổng lồ rộng bằng pháp trận, dọi thẳng từ trời xuống: dải sáng dày ở tâm + vòng dải sáng ở mép trận +
     * sao sáng phủ kín đĩa (FxType.LIGHT_PILLAR với scale = đường kính), hạt dọc thân cột trong 10 tick, sóng xung kích, rung màn hình.
     */
    static void giantPillar(ServerLevel lv, ServerPlayer owner, Vec3 at, double r) {
        double top = 32;
        if (owner != null) {
            Fx.ribbon(lv, owner, at.add(0, top, 0), at, 0, 5, 0, (float) Math.max(0.6, r * 0.45), Fx.WHITE);
            Fx.ribbon(lv, owner, at.add(0, top, 0), at, 0, 5, 0, (float) Math.max(0.9, r * 0.75), Fx.GOLD);
            int n = Math.max(8, (int) Math.round(r * 3));
            for (int k = 0; k < n; k++) {
                double a = Math.PI * 2 * k / n;
                Vec3 q = at.add(Math.cos(a) * r * 0.85, 0, Math.sin(a) * r * 0.85);
                Fx.ribbon(lv, owner, q.add(0, top, 0), q, 0, 5, k % 3, 0.35f, k % 2 == 0 ? Fx.GOLD : Fx.WHITE);
            }
            Fx.groundRing(lv, owner, at.add(0, 0.06, 0), (float) r, Fx.GOLD, 30);
            Fx.groundCrack(lv, owner, at.add(0, 0.05, 0), (float) r, Fx.GOLD, 8, 40);
        }
        Fx.play(lv, FxType.LIGHT_PILLAR, at, Vec3.ZERO, (float) (r * 2), Fx.GOLD, 40);
        Fx.play(lv, FxType.SHOCKWAVE, at.add(0, 0.2, 0), Vec3.ZERO, (float) (r * 1.6), Fx.GOLD, 20);
        for (int t = 0; t < 10; t++) {
            final int tt = t;
            com.hukhong.skill.SkillScheduler.later(t, () -> {
                for (int i = 0; i < 40; i++) {
                    double a = lv.random.nextDouble() * Math.PI * 2, rr = r * Math.sqrt(lv.random.nextDouble());
                    double y = lv.random.nextDouble() * top;
                    lv.sendParticles(ParticleTypes.END_ROD, at.x + Math.cos(a) * rr, at.y + y, at.z + Math.sin(a) * rr, 1, 0, -0.6, 0, 0.3);
                }
                ring(lv, at, r * (0.3 + 0.07 * tt), 0.15, 40, spin(lv, 0.3), GOLD_HOT);
            });
        }
        for (double rr = 1; rr <= r; rr += 1) ring(lv, at, rr, 0.15, 40, spin(lv, 0.3), GOLD_HOT);
        lv.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 2, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 2, at.z, 150, r * 0.5, 4, r * 0.5, 0.5);
        Fx.shake(lv, at, 0.8f, 12, 24f);
        sound(lv, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.7f);
        sound(lv, at, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.6f);
    }

    /** Tia phù văn từ tay tới mục tiêu (đòn thường): hạt dọc tia + tia Fx BEAM bền 6 tick. */
    static void bolt(ServerLevel lv, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        for (int s = 0; s <= 10; s++) {
            Vec3 q = from.add(d.scale(s / 10.0));
            lv.sendParticles(s % 2 == 0 ? CYAN : GOLD, q.x, q.y, q.z, 2, 0.04, 0.04, 0.04, 0.01);
        }
        Fx.play(lv, FxType.BEAM, from, d, (float) d.length(), Fx.CYAN, 6);
    }

    // ------------------------------------------------------------ hàm công khai mới

    /** Lồng vòm: 3 vòng phù văn ngang + 6 kinh tuyến xoay; màu vàng -> đỏ theo crackRatio (0..1); nứt tia lửa khi > 0.5. */
    static void cage(ServerLevel lv, Vec3 c, double r, float crackRatio) {
        float k = clamp01(crackRatio);
        DustParticleOptions col = dust(1f, 0.85f - 0.65f * k, 0.3f - 0.15f * k, 1.2f);
        double rot = spin(lv, 0.04);
        for (int i = 0; i < 3; i++) {
            double lat = Math.toRadians(15 + 25 * i);
            ring(lv, c, r * Math.cos(lat), r * Math.sin(lat) + 0.1, 24, rot + i * 0.2, col);
        }
        for (int m = 0; m < 6; m++) {
            double a = rot + Math.PI * 2 * m / 6;
            for (int s = 0; s < 6; s++) {
                dot(lv, col, onDome(c.add(0, 0.1, 0), r, Math.toRadians(s * 16), a));
            }
        }
        ring(lv, c, r, 0.1, 24, -rot, GOLD);
        if (k > 0.5f) {
            for (int i = 0; i < 4; i++) {
                Vec3 p = onDome(c.add(0, 0.1, 0), r, lv.getRandom().nextDouble() * Math.PI * 0.45, lv.getRandom().nextDouble() * Math.PI * 2);
                lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.2);
            }
        }
    }

    /** Vỡ lồng: tia CRIT/ELECTRIC_SPARK bung trên vòm, vết nứt đất đỏ, tiếng kính vỡ. */
    static void cageCrack(ServerLevel lv, Vec3 c) {
        Vec3 mid = c.add(0, 1.5, 0);
        for (int i = 0; i < 16; i++) {
            Vec3 p = onDome(mid, 3.2, lv.getRandom().nextDouble() * Math.PI * 0.5, lv.getRandom().nextDouble() * Math.PI * 2);
            lv.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.4);
        }
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 20, 1.5, 1.5, 1.5, 0.3);
        Fx.groundCrack(lv, null, c.add(0, 0.05, 0), 2.5f, Fx.RED, 6, 20);
        sound(lv, c, SoundEvents.GLASS_BREAK, 1.2f, 0.8f);
        sound(lv, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 0.7f);
    }

    /** Lồng vỡ hoàn toàn (hết lượt đập): vòng vàng bung ra theo bán kính r, mảnh kính sáng, tiếng vỡ lớn. */
    static void cageBreak(ServerLevel lv, Vec3 c, double r) {
        cageCrack(lv, c);
        for (int w = 1; w <= 2; w++) {
            ring(lv, c, r * w / 2.0, 0.3, 24, w * 0.2, w == 1 ? GOLD_HOT : GOLD);
        }
        for (int i = 0; i < 24; i++) {
            Vec3 p = inDisc(lv, c, r);
            lv.sendParticles(ParticleTypes.END_ROD, p.x, c.y + 0.5 + lv.getRandom().nextDouble() * r * 0.6, p.z, 1, 0.05, 0.3, 0.05, 0.12);
        }
        lv.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        Fx.shake(lv, c, 3f, 14, (float) r * 2);
        sound(lv, c, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.4f);
    }

    /** Vùng làm chậm: vòng lam, 2 tay xoáy ngược chiều, dây xích tỏa từ tâm, tuyết lấp lánh. */
    static void slowField(ServerLevel lv, Vec3 c, double r) {
        double t = spin(lv, 0.08);
        ring(lv, c, r, 0.06, 40, -t * 0.5, CYAN);
        for (int arm = 0; arm < 2; arm++) {
            for (int i = 0; i < 16; i++) {
                double f = i / 16.0;
                double a = t + arm * Math.PI + f * Math.PI * 3;
                dot(lv, arm == 0 ? CYAN : CYAN_PALE, onRing(c, r * f, 0.1, a));
            }
        }
        for (int s = 0; s < 6; s++) {
            double a = t * 0.5 + Math.PI * 2 * s / 6;
            for (int i = 1; i <= 4; i++) {
                dot(lv, CYAN_PALE, onRing(c, r * 0.9 * i / 4.0, 0.2, a));
            }
        }
        for (int i = 0; i < 6; i++) {
            Vec3 p = inDisc(lv, c, r);
            lv.sendParticles(ParticleTypes.SNOWFLAKE, p.x, c.y + 0.5, p.z, 1, 0, 0.2, 0, 0.01);
        }
        if (ambient(lv)) sound(lv, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f, 0.6f);
    }

    /** Trấn áp: con dấu tím-vàng nặng ép xuống (cột ép đi từ cao 3 block xuống đất, lặp theo game time), vòng tím-vàng trên đất. */
    static void suppress(ServerLevel lv, Vec3 c, double r) {
        double t = spin(lv, 0.02);
        ring(lv, c, r, 0.08, 40, t, GOLD);
        ring(lv, c, r, 0.08, 20, t + Math.PI / 20, PURPLE);
        poly(lv, c, r * 0.6, 0.1, 6, t, 4, PURPLE);
        double top = 3.0 - ((lv.getGameTime() % 10) / 10.0) * 2.7;
        ring(lv, c, r * 0.5, top, 12, -t, PURPLE);
        for (int i = 0; i <= 7; i++) {
            dot(lv, GOLD_HOT, c.add(0, 0.3 + (top - 0.3) * i / 7.0, 0));
        }
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.2, c.z, 4, r * 0.4, 0.1, r * 0.4, 0.02);
        if (ambient(lv)) sound(lv, c, SoundEvents.ANVIL_LAND, 0.6f, 0.5f);
    }

    /** Mưa kiếm phù văn: 8 thanh kiếm lam-trắng rơi xuyên vùng r (lưỡi + chuôi), vòng đánh dấu trên đất. */
    static void bladeRain(ServerLevel lv, Vec3 c, double r) {
        for (int i = 0; i < 8; i++) {
            Vec3 p = inDisc(lv, c, r);
            double top = 6 + lv.getRandom().nextDouble() * 4;
            for (int s = 0; s < 6; s++) {
                dot(lv, CYAN_PALE, p.add(0, top - 2.5 * s / 5.0, 0));
            }
            for (int g = -1; g <= 1; g++) {
                dot(lv, GOLD_HOT, p.add(g * 0.4, top + 0.3, 0));
            }
        }
        ring(lv, c, r, 0.06, 24, spin(lv, 0.05), GOLD);
    }

    /** Tích lực hủy diệt: 3 vòng co dần vào tâm (tím -> vàng khi gần xong), tia hút vào giữa. progress 0..1. */
    static void doomCharge(ServerLevel lv, Vec3 c, double r, float progress) {
        float p = clamp01(progress);
        double rad = Math.max(0.4, r * (1 - p));
        DustParticleOptions core = p < 0.8f ? PURPLE : GOLD_HOT;
        double t = spin(lv, 0.15);
        for (int k = 0; k < 3; k++) {
            double rr = rad * (1 + 0.35 * k);
            double dir = k % 2 == 0 ? 1 : -1;
            ring(lv, c, rr, 0.3 + k * 0.5, 20, t * dir + k, k == 0 ? core : GOLD);
        }
        for (int i = 0; i < 8; i++) {
            double a = t + Math.PI * 2 * i / 8;
            dot(lv, core, onRing(c, rad * 0.5, 1.0, a));
        }
        lv.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1, c.z, 6, rad * 0.2, 0.5, rad * 0.2, 0.3);
    }

    /** Bùng nổ hủy diệt: nổ lớn, 3 vòng sốc, sao rune lớn, bụi hạt tản, rung màn hình, tiếng nổ + gầm rồng. */
    static void doomBlast(ServerLevel lv, Vec3 c, double r) {
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
        for (int w = 1; w <= 3; w++) {
            ring(lv, c, r * w / 3.0, 0.2, 24, w * 0.3, w == 3 ? PURPLE : GOLD_HOT);
        }
        hexagram(lv, c, r * 0.6, 0.25, 0, 8, CYAN_PALE);
        for (int i = 0; i < 40; i++) {
            Vec3 p = inDisc(lv, c, r);
            DustParticleOptions o = i % 3 == 0 ? GOLD_HOT : (i % 3 == 1 ? PURPLE : CYAN);
            lv.sendParticles(o, p.x, c.y + lv.getRandom().nextDouble() * 2, p.z, 1, 0, 0, 0, 0);
        }
        lv.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1, c.z, 120, r * 0.5, 1.5, r * 0.5, 0.5);
        Fx.rune(lv, null, c.add(0, 0.05, 0), (float) r, Fx.GOLD, 40, 6);
        Fx.groundRing(lv, null, c.add(0, 0.06, 0), (float) r, Fx.PURPLE, 30);
        Fx.shake(lv, c, 6f, 20, (float) r * 3);
        sound(lv, c, SoundEvents.GENERIC_EXPLODE, 1.4f, 0.8f);
        sound(lv, c, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 1.3f);
    }

    /** Hộ vực lục giác lam: khung lục giác đứng cao 2.4 block (cạnh dọc + viền trên) xoay chậm. */
    static void ward(ServerLevel lv, Vec3 c, double r) {
        double t = spin(lv, 0.03);
        poly(lv, c, r, 0.1, 6, t, 4, CYAN);
        poly(lv, c, r, 2.4, 6, t, 4, CYAN_PALE);
        for (int v = 0; v < 6; v++) {
            Vec3 base = onRing(c, r, 0.1, t + Math.PI * 2 * v / 6);
            for (int h = 1; h <= 2; h++) dot(lv, CYAN, base.add(0, h * 1.1, 0));
        }
        if (lv.getRandom().nextInt(3) == 0) {
            Vec3 p = onRing(c, r, 1.2, lv.getRandom().nextDouble() * Math.PI * 2);
            lv.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 2, 0.05, 0.3, 0.05, 0.1);
        }
    }

    /** Hồi phục: vòng xanh lá + vòng vàng nhỏ, mote vàng-lục bay lên, vũng sáng nhẹ dưới đất. */
    static void healField(ServerLevel lv, Vec3 c, double r) {
        ring(lv, c, r, 0.08, 30, spin(lv, 0.04), GREEN);
        ring(lv, c, r * 0.6, 0.08, 18, -spin(lv, 0.06), GOLD_HOT);
        for (int i = 0; i < 16; i++) {
            Vec3 p = inDisc(lv, c, r * 0.9);
            lv.sendParticles(i % 2 == 0 ? ModParticles.FX_GLOW.get() : GOLD, p.x, c.y + 0.2, p.z, 0, 0, 0.12, 0, 1);
        }
        Fx.pool(lv, null, c.add(0, 0.05, 0), (float) r, 0x7FFF66, 20);
    }

    /** Vỏ vàng bao quanh thực thể (Fx SHELL) + 3 vòng vàng ở các độ cao khác nhau. Gọi lại mỗi vài tick để giữ. */
    static void shield(ServerLevel lv, LivingEntity e) {
        Fx.shell(lv, null, e, 1.1f, Fx.GOLD, 12);
        double h = e.getBbHeight();
        double rad = Math.max(0.5, e.getBbWidth() * 0.9);
        double t = spin(lv, 0.1);
        double[] levels = {0.15, 0.5, 0.85};
        for (int i = 0; i < levels.length; i++) {
            ring(lv, e.position(), rad, h * levels[i], 10, t + i * 2, GOLD);
        }
    }

    /** Vào thế đứng: 0 trắng (cơ bản), 1 lam (khống chế), 2 đỏ (sát thương), 3 vàng (phòng ngự): burst rune quanh người + tiếng. */
    static void stanceEnter(ServerLevel lv, ServerPlayer p, int stance) {
        int s = Math.max(0, Math.min(3, stance));
        DustParticleOptions col = switch (s) {
            case 1 -> CYAN;
            case 2 -> dust(1f, 0.25f, 0.2f, 1.3f);
            case 3 -> GOLD;
            default -> WHITE;
        };
        int rgb = switch (s) {
            case 1 -> Fx.CYAN;
            case 2 -> Fx.RED;
            case 3 -> Fx.GOLD;
            default -> Fx.WHITE;
        };
        Vec3 c = p.position();
        Fx.rune(lv, p, c.add(0, 0.05, 0), 1.6f, rgb, 24, 6);
        ring(lv, c, 1.6, 0.1, 32, spin(lv, 0.3), col);
        ring(lv, c, 0.9, 1.0, 16, -spin(lv, 0.4), col);
        lv.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1, c.z, 24, 0.5, 0.8, 0.5, 0.4);
        lv.sendParticles(ModParticles.FX_STAR.get(), c.x, c.y + 1, c.z, 6, 0.4, 0.6, 0.4, 0.1);
        sound(lv, c, SoundEvents.BEACON_POWER_SELECT, 0.9f, 0.9f + s * 0.1f);
        sound(lv, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.6f + s * 0.15f);
    }

    /** Kiệt sức: khói xám phả ra trước mặt + tiếng thở nặng. */
    static void exhaust(ServerLevel lv, ServerPlayer p) {
        Vec3 mouth = p.position().add(p.getLookAngle().scale(0.5)).add(0, 1.5, 0);
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 8, 0.2, 0.15, 0.2, 0.02);
        lv.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, 6, 0.25, 0.15, 0.25, 0.02);
        for (int i = 0; i < 6; i++) {
            Vec3 q = mouth.add((lv.getRandom().nextDouble() - 0.5) * 0.5, (lv.getRandom().nextDouble() - 0.5) * 0.3, (lv.getRandom().nextDouble() - 0.5) * 0.5);
            dot(lv, GREY, q);
        }
        sound(lv, p.position(), SoundEvents.PLAYER_BREATH, 1f, 0.7f);
    }
}
