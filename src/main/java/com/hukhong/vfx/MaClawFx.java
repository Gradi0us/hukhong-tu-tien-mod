package com.hukhong.vfx;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** VFX móng vuốt Ma Tu (phía server phát hạt cho mọi client): 3 vệt cong đỏ thẫm / tím sẫm và đốm hồn-khói tại điểm trúng. */
public final class MaClawFx {
    private MaClawFx() {}

    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.75f, 0.03f, 0.10f), 1.8f);
    private static final DustParticleOptions PLUM = new DustParticleOptions(new Vector3f(0.45f, 0.05f, 0.55f), 1.6f);
    private static final int POINTS = 10;

    /** Ba vệt cong song song quét trước mặt: bán kính {@code reach}, nửa góc {@code half} (rad). Không có tia thẳng. */
    public static void arc(ServerLevel lv, ServerPlayer p, double reach, double half) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.4, 0), look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        // 3 lưỡi liềm huyết khí (renderer SWORD_QI) bay ra, nghiêng lệch nhau như vết vuốt; đổi chiều mỗi đòn.
        double flip = (lv.getGameTime() & 1) == 0 ? 1 : -1;
        Vec3 dir = look.scale(reach);
        for (int s = -1; s <= 1; s++) {
            Vec3 o = eye.add(right.scale(s * 0.25)).add(0, s * 0.18, 0);
            Fx.qi(lv, o, dir, (float) (1.1 + reach * 0.15), s == 0 ? 0x6A0A8A : 0xB0102A, flip * (35 + s * 18), 8);
        }
        lv.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, eye.x + look.x * 1.5, eye.y + look.y * 1.5, eye.z + look.z * 1.5, 6, 0.4, 0.3, 0.4, 0.02);
        for (int s = -1; s <= 1; s++) {
            DustParticleOptions dust = s == 0 ? PLUM : CRIMSON;
            for (int i = 0; i <= POINTS; i++) {
                double a = -half + 2 * half * i / POINTS;
                double r = reach * (0.85 + 0.15 * Math.cos(a));           // cung cong
                Vec3 q = eye.add(flat.scale(Math.cos(a) * r))
                        .add(right.scale(Math.sin(a) * r * 0.9 + s * 0.18))
                        .add(0, s * 0.32 + 0.2 * Math.sin(a), 0);             // ba vệt lệch nhau theo chiều cao, như móng vuốt
                lv.sendParticles(dust, q.x, q.y, q.z, 2, 0.03, 0.03, 0.03, 0);
            }
        }
    }

    /** Đốm hồn + khói nhỏ tại điểm trúng. */
    public static void burst(ServerLevel lv, Vec3 at) {
        lv.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 12, 0.25, 0.3, 0.25, 0.03);
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.02);
    }
}
