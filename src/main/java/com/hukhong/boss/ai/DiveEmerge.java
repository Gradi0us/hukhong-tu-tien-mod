package com.hukhong.boss.ai;

import com.hukhong.arena.BossArena;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Lặn vào đất / Hư Không rồi trồi lên chỗ khác (kiểu Elden Beast). Dùng làm {@link GapClose} (NEAR_TARGET) hoặc boss tự gọi
 * {@link #step} trong chiêu riêng (ACROSS_ARENA). Trong lúc lặn boss tàng hình; trồi lên gọi {@link Blast} (sóng nổ, đếm...).
 * Chu trình: sink tick chìm (vũng sáng) -> hide tick ẩn, vũng sáng ở điểm đến -> trồi lên + blast -> rise tick đứng khựng.
 */
public final class DiveEmerge implements GapClose {
    public enum Where { NEAR_TARGET, ACROSS_ARENA }

    @FunctionalInterface
    public interface Blast { void at(ServerLevel sl, Mob boss, Vec3 pos); }

    private final Where where;
    private final int sink, hide, rise;
    private final String anim;
    private final Blast blast;

    public DiveEmerge(Where where, int sinkTicks, int hideTicks, int riseTicks, String anim, Blast blast) {
        this.where = where; this.sink = sinkTicks; this.hide = hideTicks; this.rise = riseTicks; this.anim = anim; this.blast = blast;
    }

    public String anim() { return anim; }
    public int length() { return sink + hide + rise; }

    public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
        mv.freeze();
        Vec3 c = b.position();
        if (i < sink) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.2, c.z, 12, b.getBbWidth() * 0.6, 0.1, b.getBbWidth() * 0.6, 0.05);
            if (i == 0) b.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.4f);
            return true;
        }
        if (i == sink) {
            Vec3 to = destination(b, t, where);
            b.setInvisible(true);
            b.teleportTo(to.x, b.getY() + Locomotion.groundY(b.level(), to.x, b.getY(), to.z) - Locomotion.groundY(b.level(), c.x, b.getY(), c.z), to.z);
            if (t != null) mv.face(t.position(), 360f);
        }
        if (i < sink + hide) {
            Vec3 g = new Vec3(b.getX(), Locomotion.groundY(b.level(), b.getX(), b.getY(), b.getZ()) + 0.1, b.getZ());
            sl.sendParticles(ParticleTypes.END_ROD, g.x, g.y, g.z, 6, b.getBbWidth() * 0.5, 0.05, b.getBbWidth() * 0.5, 0.01);
            return true;
        }
        if (i == sink + hide) {
            b.setInvisible(false);
            if (t != null) mv.face(t.position(), 360f);
            if (blast != null) blast.at(sl, b, b.position());
        }
        return i < length();
    }

    /** Điểm trồi lên: cạnh mục tiêu (phía tâm sân, cách ~4 block) hoặc đối xứng qua tâm sân; luôn trong sân. */
    public static Vec3 destination(Mob b, LivingEntity t, Where w) {
        double[] a = BossArena.bounds(b);
        Vec3 c = a == null ? (t != null ? t.position() : b.position()) : new Vec3(a[0], b.getY(), a[1]);
        double lim = a == null ? 1e9 : Math.max(2, a[2] - 4);
        Vec3 out;
        if (w == Where.NEAR_TARGET && t != null) {
            Vec3 in = new Vec3(c.x - t.getX(), 0, c.z - t.getZ());
            in = in.lengthSqr() < 1e-3 ? new Vec3(b.getX() - t.getX(), 0, b.getZ() - t.getZ()) : in;
            in = in.lengthSqr() < 1e-3 ? new Vec3(1, 0, 0) : in.normalize();
            out = new Vec3(t.getX(), 0, t.getZ()).add(in.scale(4 + b.getBbWidth() * 0.5));
        } else {
            Vec3 rel = new Vec3(b.getX() - c.x, 0, b.getZ() - c.z);
            if (rel.lengthSqr() < 1) rel = t != null ? new Vec3(c.x - t.getX(), 0, c.z - t.getZ()) : new Vec3(1, 0, 0);
            if (rel.lengthSqr() < 1e-3) rel = new Vec3(1, 0, 0);
            out = new Vec3(c.x, 0, c.z).subtract(rel.normalize().scale(Math.min(lim, Math.max(6, rel.length()))));
        }
        Vec3 rel = new Vec3(out.x - c.x, 0, out.z - c.z);
        if (rel.length() > lim) out = new Vec3(c.x, 0, c.z).add(rel.normalize().scale(lim));
        return new Vec3(out.x, b.getY(), out.z);
    }
}
