package com.hukhong.vfx;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** VFX móng vuốt Ma Tu (phía server phát hạt cho mọi client): 3 vệt cong đỏ thẫm / tím sẫm và đốm hồn-khói tại điểm trúng. */
public final class MaClawFx {
    private MaClawFx() {}

    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.75f, 0.03f, 0.10f), 1.8f);
    private static final DustParticleOptions PLUM = new DustParticleOptions(new Vector3f(0.45f, 0.05f, 0.55f), 1.6f);
    private static final int POINTS = 10;

    private static final int BLOOD_RGB = 0x8A0A1A;
    private static final DustParticleOptions BRIGHT_RED = new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.15f), 1.6f);
    private static final DustParticleOptions DARK_RED = new DustParticleOptions(new Vector3f(0.54f, 0.04f, 0.10f), 1.4f);

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

    /** Q hit 1: xiềng máu từ người tới mục tiêu vừa kéo, vòng đỏ thẫm dưới chân, dấu choáng trên đầu, tiếng nặng. Gọi SAU khi kéo. */
    public static void qPull(ServerLevel lv, ServerPlayer p, LivingEntity target) {
        Vec3 from = p.position().add(0, p.getBbHeight() * 0.6, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Fx.ribbonWave(lv, p, from, to, 0.3, 0.4, 8, 0, 0.12f, BLOOD_RGB);
        Fx.groundRing(lv, p, p.position().add(0, 0.05, 0), 2.6f, BLOOD_RGB, 14);
        // dấu choáng: vòng nhỏ quanh đầu mục tiêu + dấu chấm than
        Vec3 head = target.position().add(0, target.getBbHeight() + 0.3, 0);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            lv.sendParticles(DARK_RED, head.x + Math.cos(a) * 0.35, head.y, head.z + Math.sin(a) * 0.35, 1, 0, 0, 0, 0);
        }
        lv.sendParticles(ParticleTypes.ANGRY_VILLAGER, head.x, head.y + 0.2, head.z, 1, 0, 0, 0, 0);
        lv.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 0.6f);
    }

    /**
     * Q hit 2/3: lưỡi liềm đỏ thẫm chém qua mục tiêu. fromLeft = từ trái (roll +60°), không thì từ phải (roll -60°).
     * crit = lớn hơn, sáng hơn, kèm bùng nổ bạo kích (hạt CRIT, DAMAGE_INDICATOR, flash đỏ, tiếng chém).
     */
    public static void qSlash(ServerLevel lv, ServerPlayer p, LivingEntity target, boolean fromLeft, boolean crit) {
        double roll = fromLeft ? 60 : -60;
        qSlashAt(lv, p, target.position().add(0, target.getBbHeight() * 0.5, 0), fromLeft, crit);
    }

    /** Q đòn 1 tại điểm trước mặt: vòng sóng máu dưới chân + xích máu phóng ra (hiện cả khi không trúng ai). */
    public static void qPullAt(ServerLevel lv, ServerPlayer p, Vec3 at) {
        Fx.groundRing(lv, p, p.position().add(0, 0.05, 0), 3.0f, BLOOD_RGB, 16);
        Vec3 from = p.position().add(0, p.getBbHeight() * 0.6, 0);
        for (int s = -1; s <= 1; s++) {
            Vec3 side = new Vec3(-(at.z - from.z), 0, at.x - from.x).normalize().scale(s * 1.2);
            Fx.ribbonWave(lv, p, at.add(side).add(at.subtract(from).scale(1.5)), from, 0.4, 0.5, 8, 0, 0.14f, BLOOD_RGB);
        }
        lv.sendParticles(DARK_RED, at.x, at.y, at.z, 30, 1.2, 0.5, 1.2, 0);
        lv.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 10, 1.0, 0.4, 1.0, 0.02);
        lv.playSound(null, p.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    /** Q đòn 2/3 tại điểm c: lưỡi liềm huyết khí lớn chém từ trái/phải. */
    public static void qSlashAt(ServerLevel lv, ServerPlayer p, Vec3 c, boolean fromLeft, boolean crit) {
        double roll = fromLeft ? 60 : -60;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        Vec3 side = right.scale(fromLeft ? -1 : 1);
        Vec3 origin = c.add(side.scale(crit ? 2.6 : 2.0)).add(0, 0.4, 0);
        Fx.qi(lv, origin, c.subtract(origin).scale(2), crit ? 3.4f : 2.6f, BLOOD_RGB, roll, crit ? 12 : 10);
        Fx.qi(lv, origin.add(0, 0.3, 0), c.subtract(origin).scale(2), crit ? 2.6f : 1.9f, crit ? 0xFF2A3A : 0x6A0A8A, roll * 0.8, crit ? 10 : 8);
        // vệt bụi đỏ thẫm vẽ cung cắt qua mục tiêu
        int n = crit ? 14 : 10;
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n - 0.5;
            Vec3 q = c.add(flat.scale(t * 2.2)).add(side.scale(Math.sin((i / (double) n) * Math.PI) * (crit ? 1.2 : 0.8)));
            lv.sendParticles(crit ? BRIGHT_RED : DARK_RED, q.x, q.y, q.z, 2, 0.03, 0.03, 0.03, 0);
        }
        if (crit) {
            lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 16, 0.3, 0.3, 0.3, 0.2);
            lv.sendParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y, c.z, 4, 0.2, 0.2, 0.2, 0.1);
            lv.sendParticles(BRIGHT_RED, c.x, c.y, c.z, 12, 0.4, 0.4, 0.4, 0);
            lv.playSound(null, net.minecraft.core.BlockPos.containing(c), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}
