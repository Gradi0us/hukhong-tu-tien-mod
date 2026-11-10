package com.hukhong.boss.ai;

import com.hukhong.arena.BossArena;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Kiểu di chuyển lúc STRAFE (pluggable, đặt bằng {@code BossProfile.Builder.locomotion}). Mặc định (null) brain đi vòng quanh mục tiêu.
 * {@code dir} = chiều đi vòng hiện tại (+1/-1), brain tự đảo khi bị chặn hoặc hết thời gian.
 */
@FunctionalInterface
public interface Locomotion {
    void step(net.minecraft.server.level.ServerLevel sl, Mob boss, BossMovement mv, LivingEntity target, BossProfile.PhaseVals v, int dir);

    /**
     * Như mặc định (giữ khoảng prefMin..prefMax, đi vòng quanh mục tiêu) nhưng xoay nhanh {@code turnRate} khi đi vòng
     * (PhaseVals.turnRate vẫn dùng khi vung đòn) — boss nặng: vung chậm, đi vòng nhanh.
     */
    static Locomotion circleTarget(float turnRate) {
        return (sl, b, mv, t, v, dir) -> {
            mv.face(t.position(), turnRate);
            Vec3 to = new Vec3(t.getX() - b.getX(), 0, t.getZ() - b.getZ());
            double d = to.length();
            Vec3 n = d < 1e-3 ? mv.forward() : to.scale(1 / d);
            Vec3 tan = new Vec3(-n.z, 0, n.x).scale(dir);
            double sp = mv.speed();
            Vec3 want;
            if (d < v.prefMin()) want = n.scale(-sp).add(tan.scale(v.strafeSpeed() * 0.4));
            else if (d > v.prefMax()) want = n.scale(sp).add(tan.scale(v.strafeSpeed() * 0.3));
            else want = tan.scale(v.strafeSpeed()).add(n.scale((d - (v.prefMin() + v.prefMax()) / 2) * 0.06));
            mv.drive(want, false);
        };
    }

    /**
     * Trôi vòng quanh TÂM SÂN (không quanh người chơi) ở bán kính {@code radiusFrac} x bán kính sân, mặt vẫn hướng mục tiêu.
     * Không có BossArena: vòng quanh mục tiêu ở bán kính (prefMin+prefMax)/2. Kết hợp {@link #hover} để lơ lửng.
     */
    static Locomotion orbitCenter(double radiusFrac) {
        return (sl, b, mv, t, v, dir) -> {
            mv.face(t.position(), v.turnRate());
            double[] a = BossArena.bounds(b);
            double cx, cz, R;
            if (a == null) { cx = t.getX(); cz = t.getZ(); R = (v.prefMin() + v.prefMax()) / 2; }
            else { cx = a[0]; cz = a[1]; R = Math.max(3, a[2] * radiusFrac); }
            double rx = b.getX() - cx, rz = b.getZ() - cz, r = Math.hypot(rx, rz);
            Vec3 out = r < 1e-3 ? new Vec3(1, 0, 0) : new Vec3(rx / r, 0, rz / r);
            Vec3 tan = new Vec3(-out.z, 0, out.x).scale(dir);
            double s = v.strafeSpeed();
            mv.drive(tan.scale(s).add(out.scale(Mth.clamp((R - r) * 0.08, -s, s))), false);
        };
    }

    /** Giữ boss lơ lửng {@code height} block trên mặt đất (boss phải setNoGravity(true)). Gọi mỗi tick server. */
    static void hover(Mob b, double height) {
        double want = groundY(b.level(), b.getX(), b.getY(), b.getZ()) + height;
        double vy = Mth.clamp((want - b.getY()) * 0.15, -0.3, 0.3);
        Vec3 d = b.getDeltaMovement();
        b.setDeltaMovement(d.x, vy, d.z);
    }

    /** Mặt đất (đỉnh khối có va chạm) dưới điểm, dò xuống tối đa 24 block; không thấy = y. */
    static double groundY(Level l, double x, double y, double z) {
        BlockPos.MutableBlockPos p = BlockPos.containing(x, y + 0.5, z).mutable();
        for (int i = 0; i < 24; i++, p.move(0, -1, 0)) {
            var shape = l.getBlockState(p).getCollisionShape(l, p);
            if (!shape.isEmpty()) return p.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        return y;
    }
}
