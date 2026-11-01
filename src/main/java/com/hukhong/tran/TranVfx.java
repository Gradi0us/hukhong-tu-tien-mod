package com.hukhong.tran;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** VFX Trận Tu, chỉ server: sendParticles (vòng phù văn vàng-lam, cột sáng). Không import client.*. */
final class TranVfx {
    private TranVfx() {}

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 1.1f);
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.55f, 0.9f, 1f), 1.0f);

    /** Vòng phù văn: ring vàng/lam xen kẽ, ENCHANT rải đều, và ngôi sao 3 cạnh END_ROD ở giữa. */
    static void rune(ServerLevel lv, Vec3 c, double r) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 q = c.add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
            lv.sendParticles(i % 2 == 0 ? GOLD : CYAN, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            if (i % 4 == 0) lv.sendParticles(ParticleTypes.ENCHANT, q.x, q.y + 0.4, q.z, 2, 0.05, 0.2, 0.05, 0.1);
        }
        Vec3[] tri = new Vec3[3];
        for (int k = 0; k < 3; k++) {
            double a = Math.PI * 2 * k / 3 - Math.PI / 2;
            tri[k] = c.add(Math.cos(a) * r * 0.7, 0.06, Math.sin(a) * r * 0.7);
        }
        for (int k = 0; k < 3; k++) {
            Vec3 from = tri[k], to = tri[(k + 1) % 3];
            for (int s = 0; s <= 6; s++) {
                Vec3 q = from.lerp(to, s / 6.0);
                lv.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Cột sáng dọc 6 block tại điểm chạm. */
    static void pillar(ServerLevel lv, Vec3 at) {
        for (double y = 0; y <= 6; y += 0.5) {
            lv.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 1, 0.05, 0, 0.05, 0);
        }
        lv.sendParticles(GOLD, at.x, at.y + 6, at.z, 8, 0.4, 0.2, 0.4, 0);
        lv.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1, at.z, 14, 0.5, 1.5, 0.5, 0.2);
    }

    /** Tia phù văn từ tay tới mục tiêu (đòn thường). */
    static void bolt(ServerLevel lv, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        for (int s = 0; s <= 8; s++) {
            Vec3 q = from.add(d.scale(s / 8.0));
            lv.sendParticles(s % 2 == 0 ? CYAN : GOLD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }
}
