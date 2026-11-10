package com.hukhong.boss.ai;

import com.hukhong.arena.BossArena;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Kiểu áp sát (pluggable). Boss tự viết kiểu riêng bằng cách implement và đặt vào {@code BossProfile.Builder.gapClose}.
 * {@link #step} trả false khi đã xong (brain sẽ chọn chuỗi đòn ngay).
 */
public interface GapClose {
    /** Tên animation brain phát khi bắt đầu (dash / leap / ...). */
    String anim();

    /** Gọi mỗi tick, t bắt đầu từ 0. */
    boolean step(ServerLevel sl, Mob boss, BossMovement mv, LivingEntity target, int t);

    /** Chuỗi đòn nối ngay sau khi áp sát (id trong profile của boss); null = brain chọn theo tầm. */
    default String followCombo() { return null; }

    /** Dọn dẹp khi kết thúc / bị cắt ngang (bật lại trọng lực, hiện hình...). */
    default void end(Mob boss) {}

    /** Bọc một kiểu áp sát để luôn nối chuỗi {@code comboId} (vd. lướt rồi đâm). */
    static GapClose then(GapClose g, String comboId) {
        return new GapClose() {
            public String anim() { return g.anim(); }
            public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) { return g.step(sl, b, mv, t, i); }
            public String followCombo() { return comboId; }
            public void end(Mob b) { g.end(b); }
        };
    }

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

    /**
     * Bay lượn rồi lao xuống (~32 tick): bốc lên 8 tick, lượn vòng trên đầu mục tiêu 16 tick, lao chéo xuống cạnh mục tiêu.
     * Tắt trọng lực trong lúc bay ({@link #end} bật lại kể cả khi bị cắt ngang).
     */
    GapClose HOVER = new GapClose() {
        public String anim() { return "hover"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            mv.face(t.position(), 40f);
            if (i == 0) { b.setNoGravity(true); b.playSound(SoundEvents.PHANTOM_FLAP, 1.6f, 0.8f); }
            Vec3 to = t.position().subtract(b.position()).multiply(1, 0, 1);
            double d = to.length();
            Vec3 n = d < 1e-3 ? mv.forward() : to.scale(1 / d);
            if (i < 8) {                                   // bốc lên, trôi về phía mục tiêu
                b.setDeltaMovement(b.getDeltaMovement().x, 0.42, b.getDeltaMovement().z);
                mv.drive(n.scale(Math.min(0.5, d * 0.08)), true);
            } else if (i < 24) {                           // lượn vòng bán kính ~3 quanh đỉnh đầu mục tiêu
                double a = Math.atan2(-to.z, -to.x) + 0.18;
                Vec3 want = t.position().add(Math.cos(a) * 3, 0, Math.sin(a) * 3).subtract(b.position()).multiply(1, 0, 1);
                mv.drive(want.scale(0.35), true);
                b.setDeltaMovement(b.getDeltaMovement().x, Math.sin(i * 0.5) * 0.04, b.getDeltaMovement().z);
                if (i % 3 == 0) sl.sendParticles(ParticleTypes.END_ROD, b.getX(), b.getY() + 0.5, b.getZ(), 2, 0.4, 0.2, 0.4, 0.01);
            } else {                                       // lao xuống
                mv.drive(n.scale(Math.min(0.9, Math.max(0, d - 1.8) * 0.45)), true);
                b.setDeltaMovement(b.getDeltaMovement().x, -1.0, b.getDeltaMovement().z);
                if ((b.onGround() && i > 24) || i >= 34) {
                    end(b);
                    mv.freeze();
                    mv.footstep(sl);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, b.getX(), b.getY() + 0.3, b.getZ(), 3, 0.8, 0.1, 0.8, 0);
                    return false;
                }
            }
            return true;
        }
        public void end(Mob b) { b.setNoGravity(false); b.resetFallDistance(); }
    };

    /** Lướt máu: tan thành vệt máu 6 tick rồi hiện ra sau lưng mục tiêu (không ra ngoài BossArena). */
    GapClose BLINK = new GapClose() {
        public String anim() { return "blink"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            mv.freeze();
            DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.75f, 0.04f, 0.1f), 1.8f);
            sl.sendParticles(blood, b.getX(), b.getY() + 1, b.getZ(), 10, 0.35, 0.7, 0.35, 0);
            if (i == 1) { b.setInvisible(true); b.playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.5f); }
            if (i < 6) return true;
            Vec3 at = behind(b, t);
            Vec3 from = b.position();
            for (int k = 0; k <= 12; k++) {
                Vec3 q = from.lerp(at, k / 12.0);
                sl.sendParticles(blood, q.x, q.y + 1, q.z, 2, 0.1, 0.2, 0.1, 0);
            }
            b.teleportTo(at.x, t.getY(), at.z);
            end(b);
            mv.face(t.position(), 360f);
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
            return false;
        }
        public void end(Mob b) { b.setInvisible(false); }
    };

    /** Điểm sau lưng mục tiêu (ngược hướng nhìn), lùi về phía xa / gần nếu ra ngoài sân. */
    static Vec3 behind(Mob b, LivingEntity t) {
        Vec3 look = t.getLookAngle().multiply(1, 0, 1);
        Vec3 dir = t.position().subtract(b.position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : dir.normalize();
        look = look.lengthSqr() < 1e-4 ? dir.scale(-1) : look.normalize();
        double[] a = BossArena.bounds(b);
        for (Vec3 c : new Vec3[]{t.position().subtract(look.scale(2.2)), t.position().add(dir.scale(2.2)), t.position().subtract(dir.scale(2.2))}) {
            if (a == null || Math.hypot(c.x - a[0], c.z - a[1]) <= a[2] - 2.5) return c;
        }
        return b.position();
    }
}
