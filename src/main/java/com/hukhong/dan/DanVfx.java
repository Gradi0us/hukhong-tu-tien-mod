package com.hukhong.dan;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** VFX Đan Tu, chỉ server: viên đan xanh-lục, bụi vàng, hồ độc, nón lửa. Không import client.*. */
final class DanVfx {
    private DanVfx() {}

    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.45f, 1f, 0.25f), 1.1f);
    private static final DustParticleOptions DARK_GREEN = new DustParticleOptions(new Vector3f(0.2f, 0.55f, 0.15f), 1.0f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 1.2f);

    /** Viên đan bay theo đường cong ngắn từ tay tới mục tiêu. */
    static void throwArc(ServerLevel lv, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        for (int s = 0; s <= 6; s++) {
            Vec3 q = from.add(d.scale(s / 6.0)).add(0, Math.sin(Math.PI * s / 6.0) * 0.6, 0);
            lv.sendParticles(s % 2 == 0 ? GREEN : GOLD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** Nổ đan: vòng bụi xanh-lục và vàng, hạt slime và khói. */
    static void burst(ServerLevel lv, Vec3 at, double r) {
        int n = 24;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 q = at.add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
            lv.sendParticles(i % 3 == 0 ? GOLD : GREEN, q.x, q.y, q.z, 1, 0, 0.1, 0, 0);
        }
        lv.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.5, at.z, 18, r * 0.5, 0.4, r * 0.5, 0.05);
        lv.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.3, at.z, 6, r * 0.3, 0.2, r * 0.3, 0.02);
    }

    /** Hồ độc: bọt xanh sẫm trên mặt đất trong bán kính, gọi định kỳ mỗi giây. */
    static void pool(ServerLevel lv, Vec3 c, double r) {
        for (int i = 0; i < 16; i++) {
            double a = lv.random.nextDouble() * Math.PI * 2;
            double rr = r * Math.sqrt(lv.random.nextDouble());
            lv.sendParticles(DARK_GREEN, c.x + Math.cos(a) * rr, c.y + 0.1, c.z + Math.sin(a) * rr, 1, 0, 0.05, 0, 0);
        }
        lv.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 0.6, c.z, 4, r * 0.5, 0.3, r * 0.5, 0.01);
    }

    /** Dấu độc gắn lên mục tiêu khi cộng tầng. */
    static void poisonPuff(ServerLevel lv, Vec3 at, int stacks) {
        lv.sendParticles(GREEN, at.x, at.y + 1.0, at.z, 2 + stacks, 0.3, 0.4, 0.3, 0.02);
    }

    /** Nón lửa Đan Hỏa: lửa thường và lửa hồn, rải theo nón từ mắt người chơi. */
    static void flameCone(ServerLevel lv, Vec3 eye, Vec3 look, double range, double cosHalf) {
        double halfAngle = Math.acos(Math.max(-1, Math.min(1, cosHalf)));
        for (int s = 1; s <= 8; s++) {
            double dist = range * s / 8.0;
            for (int k = -2; k <= 2; k++) {
                double yaw = Math.atan2(-look.x, look.z) + k * halfAngle / 2.5;
                Vec3 dir = new Vec3(-Math.sin(yaw), look.y, Math.cos(yaw)).normalize();
                Vec3 q = eye.add(dir.scale(dist));
                lv.sendParticles(s % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, q.x, q.y, q.z, 2, 0.12, 0.12, 0.12, 0.02);
            }
        }
        lv.sendParticles(ParticleTypes.LAVA, eye.x + look.x * 1.5, eye.y, eye.z + look.z * 1.5, 4, 0.3, 0.2, 0.3, 0);
    }

    /** Vòng vàng: T Cửu Chuyển Kim Đan, hồi Hồi Xuân. */
    static void goldRing(ServerLevel lv, Vec3 c, double r) {
        int n = 28;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            lv.sendParticles(GOLD, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
        }
        lv.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1.0, c.z, 14, r * 0.4, 0.8, r * 0.4, 0.2);
    }
}
