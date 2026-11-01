package com.hukhong.tran;

import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * VFX Trận Tu, chỉ server. Hai lớp:
 * - sendParticles (hạt dust/enchant/end_rod, count >= 1, có offset để hạt bay tản): nhìn thấy trong 32 block;
 * - Fx (gói riêng, client tự dựng decal/cột sáng/tia, bền theo life tick): cùng đường đi với Pháp Tu/Đạo Tu.
 * Không import client.*.
 */
final class TranVfx {
    private TranVfx() {}

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 1.3f);
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.55f, 0.9f, 1f), 1.2f);

    /** Vòng phù văn bằng hạt: 48 điểm ring vàng/lam xen kẽ, ENCHANT bay lên, sao 3 cạnh END_ROD ở giữa. Vẽ lại mỗi vài tick. */
    static void rune(ServerLevel lv, Vec3 c, double r) {
        int n = 48;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 q = c.add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
            lv.sendParticles(i % 2 == 0 ? GOLD : CYAN, q.x, q.y, q.z, 1, 0.02, 0.04, 0.02, 0.01);
            if (i % 3 == 0) lv.sendParticles(ParticleTypes.ENCHANT, q.x, q.y + 0.4, q.z, 3, 0.05, 0.3, 0.05, 0.15);
        }
        Vec3[] tri = new Vec3[3];
        for (int k = 0; k < 3; k++) {
            double a = Math.PI * 2 * k / 3 - Math.PI / 2;
            tri[k] = c.add(Math.cos(a) * r * 0.7, 0.06, Math.sin(a) * r * 0.7);
        }
        for (int k = 0; k < 3; k++) {
            Vec3 from = tri[k], to = tri[(k + 1) % 3];
            for (int s = 0; s <= 8; s++) {
                Vec3 q = from.lerp(to, s / 8.0);
                lv.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0.02, 0, 0.01);
            }
        }
    }

    /**
     * Vòng phù văn bền theo tuổi trận: decal Fx (client vẽ suốt {@code life} tick) + vòng nền sáng lam.
     * Gọi một lần khi đặt trận; hạt {@link #rune} được vẽ lại định kỳ trong TranRuntime.
     */
    static void zoneDecal(ServerLevel lv, ServerPlayer owner, Vec3 c, double r, int life) {
        Fx.rune(lv, owner, c.add(0, 0.05, 0), (float) r, Fx.GOLD, life, 3);
        Fx.groundRing(lv, owner, c.add(0, 0.06, 0), (float) r, Fx.CYAN, Math.min(life, 24));
    }

    /** Nổ sáng khi đặt trận / đánh cận chiến: 40 điểm dust vòng ngoài + 30 ENCHANT tản quanh tâm. */
    static void burst(ServerLevel lv, Vec3 at, double r) {
        int n = 40;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 q = at.add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
            lv.sendParticles(i % 2 == 0 ? GOLD : CYAN, q.x, q.y, q.z, 1, 0, 0.2, 0, 0.02);
        }
        lv.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1.0, at.z, 30, r * 0.5, 0.8, r * 0.5, 0.25);
    }

    /** Cột sáng dọc 6 block tại điểm chạm: hạt END_ROD + cột Fx (client dựng). */
    static void pillar(ServerLevel lv, Vec3 at) {
        for (double y = 0; y <= 6; y += 0.5) {
            lv.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 1, 0.05, 0, 0.05, 0.01);
        }
        lv.sendParticles(GOLD, at.x, at.y + 6, at.z, 10, 0.4, 0.2, 0.4, 0.05);
        lv.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1, at.z, 18, 0.5, 1.5, 0.5, 0.2);
        Fx.play(lv, FxType.LIGHT_PILLAR, at, Vec3.ZERO, 0.9f, Fx.GOLD, 24);
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
}
