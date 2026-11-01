package com.hukhong.dan;

import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * VFX Đan Tu, chỉ server. Hai lớp: sendParticles (hạt dust/slime/khói, count >= 1, có offset) để nhìn thấy trong 32 block,
 * và Fx (decal hồ độc / vòng vàng bền theo life tick, cùng đường đi với Pháp Tu). Không import client.*.
 */
final class DanVfx {
    private DanVfx() {}

    private static final int POISON_RGB = 0x6FD13E, GOLD_RGB = 0xFFD866;
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.45f, 1f, 0.25f), 1.2f);
    private static final DustParticleOptions POISON = new DustParticleOptions(new Vector3f(0.35f, 0.9f, 0.2f), 1.3f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 1.3f);

    /** Viên đan bay theo đường cong ngắn từ tay (hoặc từ trên cao khi rơi) tới mục tiêu. */
    static void throwArc(ServerLevel lv, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        for (int s = 0; s <= 10; s++) {
            Vec3 q = from.add(d.scale(s / 10.0)).add(0, Math.sin(Math.PI * s / 10.0) * 0.6, 0);
            lv.sendParticles(s % 2 == 0 ? GREEN : GOLD, q.x, q.y, q.z, 2, 0.04, 0.04, 0.04, 0.01);
        }
    }

    /** Nổ đan: 2 vòng bụi xanh-lục/vàng, hạt slime và khói; kèm vòng Fx mờ dần 1 giây. */
    static void burst(ServerLevel lv, ServerPlayer owner, Vec3 at, double r) {
        int n = 36;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 q = at.add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
            lv.sendParticles(i % 3 == 0 ? GOLD : GREEN, q.x, q.y, q.z, 1, 0, 0.15, 0, 0.02);
            Vec3 inner = at.add(Math.cos(a) * r * 0.5, 0.4, Math.sin(a) * r * 0.5);
            lv.sendParticles(POISON, inner.x, inner.y, inner.z, 1, 0, 0.1, 0, 0.01);
        }
        lv.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.5, at.z, 24, r * 0.5, 0.4, r * 0.5, 0.08);
        lv.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.3, at.z, 10, r * 0.3, 0.2, r * 0.3, 0.03);
        if (owner != null) Fx.groundRing(lv, owner, at.add(0, 0.1, 0), (float) r, POISON_RGB, 20);
    }

    /** Hồ độc: 24 bọt xanh trên mặt đất trong bán kính, gọi định kỳ mỗi nhịp (hạt, không tốn decal). */
    static void pool(ServerLevel lv, Vec3 c, double r) {
        for (int i = 0; i < 24; i++) {
            double a = lv.random.nextDouble() * Math.PI * 2;
            double rr = r * Math.sqrt(lv.random.nextDouble());
            lv.sendParticles(POISON, c.x + Math.cos(a) * rr, c.y + 0.1, c.z + Math.sin(a) * rr, 1, 0.05, 0.1, 0.05, 0.02);
        }
        lv.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 0.6, c.z, 6, r * 0.5, 0.3, r * 0.5, 0.02);
    }

    /** Hồ độc bền theo thời gian hồ: vũng Fx xanh loang trên đất (client vẽ suốt {@code life} tick). */
    static void poolDecal(ServerLevel lv, ServerPlayer owner, Vec3 c, double r, int life) {
        Fx.pool(lv, owner, c.add(0, 0.05, 0), (float) r, POISON_RGB, life);
    }

    /** Dấu độc gắn lên mục tiêu khi cộng tầng. */
    static void poisonPuff(ServerLevel lv, Vec3 at, int stacks) {
        lv.sendParticles(GREEN, at.x, at.y + 1.0, at.z, 3 + stacks, 0.3, 0.4, 0.3, 0.03);
    }

    /** Nón lửa Đan Hỏa: lửa thường và lửa hồn, rải theo nón từ mắt người chơi (dày gấp đôi bản cũ). */
    static void flameCone(ServerLevel lv, Vec3 eye, Vec3 look, double range, double cosHalf) {
        double halfAngle = Math.acos(Math.max(-1, Math.min(1, cosHalf)));
        for (int s = 1; s <= 10; s++) {
            double dist = range * s / 10.0;
            for (int k = -3; k <= 3; k++) {
                double yaw = Math.atan2(-look.x, look.z) + k * halfAngle / 3.0;
                Vec3 dir = new Vec3(-Math.sin(yaw), look.y, Math.cos(yaw)).normalize();
                Vec3 q = eye.add(dir.scale(dist));
                lv.sendParticles(s % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, q.x, q.y, q.z, 3, 0.15, 0.15, 0.15, 0.03);
            }
        }
        lv.sendParticles(ParticleTypes.LAVA, eye.x + look.x * 1.5, eye.y, eye.z + look.z * 1.5, 6, 0.3, 0.2, 0.3, 0.02);
    }

    /** Vòng vàng: T Cửu Chuyển Kim Đan, hồi Hồi Xuân. 36 điểm dust + ENCHANT + vòng Fx bền 1 giây. */
    static void goldRing(ServerLevel lv, ServerPlayer owner, Vec3 c, double r) {
        int n = 36;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            lv.sendParticles(GOLD, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
        }
        lv.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1.0, c.z, 24, r * 0.4, 0.8, r * 0.4, 0.25);
        if (owner != null) Fx.groundRing(lv, owner, c.add(0, 0.1, 0), (float) r, GOLD_RGB, 20);
    }

    /** Dấu đất khi viên đan chạm/đặt ở điểm ngắm (Q trước khi rơi): vòng lục mờ nhẹ. */
    static void aimRing(ServerLevel lv, ServerPlayer owner, Vec3 c, double r) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            lv.sendParticles(POISON, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
        }
        if (owner != null) Fx.groundRing(lv, owner, c.add(0, 0.08, 0), (float) r, POISON_RGB, 16);
    }
}
