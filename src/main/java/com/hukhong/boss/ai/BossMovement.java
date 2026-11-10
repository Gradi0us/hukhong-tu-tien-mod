package com.hukhong.boss.ai;

import com.hukhong.arena.BossArena;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Chuyển động mượt cho boss (PLAN-v1 M1 tăng/giảm tốc, M2 giới hạn xoay, M4 dịch chuyển theo đòn, M8 bước chân).
 * Tự giữ vận tốc ngang + hướng mặt, mỗi tick ghi đè deltaMovement/rotation của mob; giữ boss trong BossArena và tự lách khi kẹt tường.
 */
public final class BossMovement {
    private final Mob mob;
    private final BossProfile prof;
    private Vec3 vel = Vec3.ZERO;
    private float yaw;
    private boolean init;
    private int stuck, detour, detourSign = 1;
    private double stride;
    /** Tick vừa rồi bị chặn (tường hoặc mép sân) — brain dùng để đổi chiều đi vòng. */
    boolean blocked;

    BossMovement(Mob mob, BossProfile prof) { this.mob = mob; this.prof = prof; }

    public float yaw() { ensure(); return yaw; }
    public Vec3 velocity() { return vel; }
    /** Tốc độ đi tối đa của profile (block/tick). */
    public double speed() { return prof.moveSpeed(); }
    public Vec3 forward() { ensure(); double r = Math.toRadians(yaw); return new Vec3(-Math.sin(r), 0, Math.cos(r)); }

    private void ensure() { if (!init) { init = true; yaw = mob.getYRot(); } }

    /** M2: xoay về phía điểm với tốc độ tối đa {@code rate} độ/tick. */
    public void face(Vec3 at, float rate) {
        ensure();
        double dx = at.x - mob.getX(), dz = at.z - mob.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        yaw += Mth.clamp(Mth.wrapDegrees(want - yaw), -rate, rate);
        yaw = Mth.wrapDegrees(yaw);
    }

    /** M1: tiến dần tới vận tốc mong muốn (tăng tốc accelTicks, phanh brakeTicks). instant = M4 root motion (không nội suy). */
    public void drive(Vec3 desired, boolean instant) {
        ensure();
        desired = new Vec3(desired.x, 0, desired.z);
        desired = steer(desired);
        if (instant) { vel = desired; return; }
        Vec3 diff = desired.subtract(vel);
        double max = prof.moveSpeed() * 1.5;
        double step = desired.lengthSqr() >= vel.lengthSqr() ? max / prof.accelTicks() : max / prof.brakeTicks();
        if (diff.length() > step) diff = diff.normalize().scale(step);
        vel = vel.add(diff);
    }

    /** Đứng yên (phanh). */
    public void stop() { drive(Vec3.ZERO, false); }

    /** Đứng khựng ngay (hit-stop). */
    public void freeze() { vel = Vec3.ZERO; }

    private Vec3 steer(Vec3 d) {
        blocked = false;
        if (d.lengthSqr() < 1e-6) { stuck = 0; return d; }
        // tường: thử nhảy bậc, kẹt lâu thì lách nghiêng 75° một lúc
        if (mob.horizontalCollision) {
            if (++stuck == 2 && mob.onGround()) mob.getJumpControl().jump();
            if (stuck > 4) { detour = 12; detourSign = mob.getRandom().nextBoolean() ? 1 : -1; stuck = 0; blocked = true; }
        } else stuck = 0;
        if (detour > 0) { detour--; d = rotY(d, 75 * detourSign); }
        // sân: không bước ra ngoài r-2
        double[] b = BossArena.bounds(mob);
        if (b != null) {
            double rx = mob.getX() - b[0], rz = mob.getZ() - b[1], dist = Math.hypot(rx, rz), lim = b[2] - 2.5;
            if (dist > 1e-3) {
                Vec3 out = new Vec3(rx / dist, 0, rz / dist);
                Vec3 next = new Vec3(rx, 0, rz).add(d.scale(4));
                if (next.horizontalDistance() > lim) {
                    double o = d.dot(out);
                    if (o > 0) { d = d.subtract(out.scale(o)); blocked = true; }
                    if (dist > lim) d = d.subtract(out.scale(0.12));
                }
            }
        }
        return d;
    }

    static Vec3 rotY(Vec3 v, double deg) {
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    /** Ghi vận tốc + hướng mặt vào mob; bụi/âm bước chân theo trọng lượng (M8). Gọi một lần mỗi tick sau khi brain quyết định. */
    void apply(ServerLevel sl) {
        ensure();
        mob.getNavigation().stop();
        mob.setZza(0); mob.setXxa(0);
        mob.setDeltaMovement(vel.x, mob.getDeltaMovement().y, vel.z);
        mob.setYRot(yaw); mob.yBodyRot = yaw; mob.setYHeadRot(yaw);
        double sp = vel.horizontalDistance();
        if (sp > 0.04 && mob.onGround()) {
            stride += sp;
            double strideLen = 1.4 + 0.4 * prof.weight();
            if (stride >= strideLen) { stride = 0; footstep(sl); }
        }
    }

    void footstep(ServerLevel sl) {
        BlockPos below = mob.blockPosition().below();
        BlockState st = sl.getBlockState(below);
        if (st.isAir()) return;
        double w = prof.weight();
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), mob.getX(), mob.getY() + 0.05, mob.getZ(),
                (int) Math.round(3 + 4 * w), mob.getBbWidth() * 0.4, 0.02, mob.getBbWidth() * 0.4, 0.05);
        sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), st.getSoundType(sl, below, mob).getStepSound(), mob.getSoundSource(),
                (float) Math.min(2.0, 0.25 + 0.35 * w), (float) Mth.clamp(1.15 - 0.2 * w, 0.5, 1.3));
    }
}
