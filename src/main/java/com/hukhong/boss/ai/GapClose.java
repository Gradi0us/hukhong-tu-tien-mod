package com.hukhong.boss.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Kiểu áp sát (pluggable). Boss tự viết kiểu riêng bằng cách implement và đặt vào {@code BossProfile.Builder.gapClose}.
 * {@link #step} trả false khi đã xong (brain sẽ chọn chuỗi đòn ngay).
 */
public interface GapClose {
    /** Tên animation brain phát khi bắt đầu (dash / leap / ...). */
    String anim();

    /** Gọi mỗi tick, t bắt đầu từ 0. */
    boolean step(ServerLevel sl, Mob boss, BossMovement mv, LivingEntity target, int t);

    /** Lướt thấp: tăng tốc mạnh thẳng tới mục tiêu, tối đa 14 tick, dừng khi còn ~2.5 block. */
    GapClose DASH = new GapClose() {
        public String anim() { return "dash"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            double d = b.distanceTo(t);
            if (i > 14 || d < 2.8) { mv.freeze(); return false; }
            mv.face(t.position(), 30f);
            mv.drive(mv.forward().scale(Math.min(0.85, d * 0.3)), true);
            if (i % 2 == 0) sl.sendParticles(ParticleTypes.CLOUD, b.getX(), b.getY() + 0.2, b.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
            return true;
        }
    };

    /** Nhảy vọt: bật lên rồi rơi cạnh mục tiêu (~16 tick), rung đất khi đáp. */
    GapClose LEAP = new GapClose() {
        public String anim() { return "leap"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            if (i == 0) {
                Vec3 to = t.position().subtract(b.position());
                double h = Math.max(0, to.horizontalDistance() - 2.5);
                Vec3 dir = new Vec3(to.x, 0, to.z).normalize();
                mv.face(t.position(), 180f);
                mv.drive(dir.scale(h / 14.0), true);
                b.setDeltaMovement(b.getDeltaMovement().x, 0.75, b.getDeltaMovement().z);
                b.playSound(SoundEvents.RAVAGER_ROAR, 1f, 1.4f);
                return true;
            }
            mv.drive(mv.velocity(), true);
            if (i > 3 && b.onGround() || i > 30) {
                mv.freeze();
                mv.footstep(sl);
                sl.sendParticles(ParticleTypes.EXPLOSION, b.getX(), b.getY(), b.getZ(), 1, 0, 0, 0, 0);
                return false;
            }
            return true;
        }
    };

    /** Dịch chuyển ngắn: 6 tick tụ khói rồi hiện ra cách mục tiêu ~2.5 block. */
    GapClose TELEPORT = new GapClose() {
        public String anim() { return "dash"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            mv.freeze();
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, b.getX(), b.getY() + 1, b.getZ(), 6, 0.3, 0.6, 0.3, 0.05);
            if (i < 6) return true;
            Vec3 off = b.position().subtract(t.position()).multiply(1, 0, 1);
            off = off.lengthSqr() < 1e-3 ? new Vec3(1, 0, 0) : off.normalize();
            Vec3 at = t.position().add(off.scale(2.5));
            b.teleportTo(at.x, t.getY(), at.z);
            b.playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.8f);
            mv.face(t.position(), 360f);
            return false;
        }
    };
}
