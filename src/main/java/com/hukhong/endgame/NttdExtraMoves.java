package com.hukhong.endgame;

import com.hukhong.vfx.Fx;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Chiêu bổ sung của Nguyên Thủy Tịch Diệt: 3 chiêu dạng 1 (chạy qua pending/windup, dispatch trong stepF1)
 * và 3 chiêu dạng 2 (chạy như own-move của BossBrain, dispatch trong stepOwn). Mọi chiêu có telegraph &gt;= 15 tick.
 * Gọi từ NguyenThuyTichDiet qua các hook nhỏ (f1Swap / f1 / startF2 / stepF2).
 */
final class NttdExtraMoves {
    private NttdExtraMoves() {}

    private static final int GOLD = 0xFFD866, VOIDC = 0xA050FF, PALE = 0xE8D8FF;

    // ---- dạng 1 (mã pending; A_LEN/A_ANIM trong NguyenThuyTichDiet mở rộng theo thứ tự này)
    static final int A_CHAIN = 5, A_STAR = 6, A_SHOCK = 7;

    // ---- dạng 2 (mã own-move; OWN_ANIM trong NguyenThuyTichDiet)
    static final int O_GRAVITY = 21, O_FREEZE = 22, O_ECHO = 23;

    /** Trạng thái riêng của chiêu dạng 2 (tâm hố trọng lực / hướng tia / điểm nhân bản + chiêu trước). */
    static final class State {
        Vec3 c = Vec3.ZERO;
        int last = 0, echoOf = 0;
    }

    // ================================================================ DẠNG 1

    /** Thay một phần chiêu thường bằng chiêu bổ sung (~35%): xa dùng xích/sao, gần dùng sao/chấn. */
    static int f1Swap(net.minecraft.util.RandomSource rnd, double d, int base) {
        if (rnd.nextInt(100) >= 35) return base;
        int[] pool = d > 9 ? new int[]{A_CHAIN, A_STAR} : new int[]{A_STAR, A_SHOCK};
        return pool[rnd.nextInt(pool.length)];
    }

    static void f1(ServerLevel sl, NguyenThuyTichDiet b, int a, int at, Vec3 at0, List<Vec3> marks) {
        switch (a) {
            case A_CHAIN -> chain(sl, b, at, marks);
            case A_STAR -> star(sl, b, at, marks);
            case A_SHOCK -> shock(sl, b, at);
            default -> { }
        }
    }

    /** "Hư Liên Xiềng": xích hư không nối từ thân tới người chơi (18 tick cảnh báo), rồi kéo họ về tâm và nổ tại chỗ (tick 26). */
    private static void chain(ServerLevel sl, NguyenThuyTichDiet b, int at, List<Vec3> marks) {
        Vec3 c = b.position();
        if (at == 1) {
            for (Player p : b.players(sl, c, 14)) {
                marks.add(p.position());
                Fx.telegraphCircle(sl, null, p.position(), 1.2f, 18, VOIDC);
            }
            Fx.rune(sl, null, c.add(0, 0.1, 0), 4f, VOIDC, 40, 6);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 2f, 0.6f);
        }
        if (at >= 2 && at <= 17 && at % 3 == 2) {
            for (Vec3 m : marks) Fx.ribbon(sl, null, c.add(0, 1.8, 0), m.add(0, 0.9, 0), 0, 4, 0, 0.3f, VOIDC);
        }
        if (at >= 18 && at <= 26) {
            for (Player p : b.players(sl, c, 14)) {
                Vec3 to = c.subtract(p.position()).multiply(1, 0, 1);
                if (to.lengthSqr() < 1e-4) continue;
                Vec3 n = to.normalize();
                p.setDeltaMovement(n.x * 0.45, p.getDeltaMovement().y, n.z * 0.45);
                p.hurtMarked = true;
            }
        }
        if (at == 26) {
            for (Player p : b.players(sl, c, 3.5)) b.hit(p, b.atk(0.9), c, true);
            Fx.groundRing(sl, null, c, 3.5f, VOIDC, 14);
            Fx.shake(sl, c, 2f, 10, 20);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 60, 1.5, 1, 1.5, 0.3);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5f, 0.9f);
        }
    }

    /** "Tinh Vẫn": 4 vùng đánh dấu lên người chơi (18 tick cảnh báo), sao rơi xuống lần lượt từng vùng. */
    private static void star(ServerLevel sl, NguyenThuyTichDiet b, int at, List<Vec3> marks) {
        if (at == 1) {
            List<Player> ps = b.players(sl, b.position(), 20);
            for (int i = 0; i < 4 && !ps.isEmpty(); i++) {
                Vec3 m = ps.get(b.getRandom().nextInt(ps.size())).position();
                marks.add(m);
                Fx.telegraphCircle(sl, null, m, 2.2f, 18, GOLD);
            }
        }
        if (at >= 2 && at <= 17 && at % 3 == 2) {
            for (Vec3 m : marks) {
                Vec3 from = m.add(b.getRandom().nextGaussian() * 2.5, 22, b.getRandom().nextGaussian() * 2.5);
                Fx.ribbon(sl, null, from, m, 0, 5, 0, 0.35f, GOLD);
            }
        }
        for (int i = 0; i < marks.size(); i++) {
            if (at != 18 + 4 * i) continue;
            Vec3 m = marks.get(i);
            for (Player p : b.players(sl, m, 2.2)) b.hit(p, b.atk(0.9), m, i == 3);
            Fx.groundRing(sl, null, m, 2.4f, GOLD, 12);
            sl.sendParticles(ParticleTypes.END_ROD, m.x, m.y + 0.5, m.z, 20, 0.6, 0.6, 0.6, 0.2);
            sl.playSound(null, m.x, m.y, m.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        }
    }

    /** "Chấn Hư Liên Hoàn": vòng cảnh báo quanh thân (18 tick), rồi 3 sóng chấn nở ra liên tiếp; nhảy để né. */
    private static void shock(ServerLevel sl, NguyenThuyTichDiet b, int at) {
        if (at != 1) return;
        Vec3 c = b.position();
        Fx.telegraphCircle(sl, null, c, 3f, 18, VOIDC);
        for (int k = 0; k < 3; k++) {
            int base = 18 + k * 8;
            Set<UUID> done = new HashSet<>();
            for (int s = 0; s <= 8; s++) {
                double r = 1.5 + s * 1.6;
                b.later(base + s, () -> {
                    if (((int) r) % 2 == 0) Fx.groundRing(sl, null, c, (float) r, VOIDC, 3);
                    for (Player p : b.players(sl, c, r + 1.2)) {
                        if (done.contains(p.getUUID()) || !p.onGround() || Math.abs(p.getY() - c.y) > 2.5) continue;
                        double d = Math.sqrt(p.position().multiply(1, 0, 1).distanceToSqr(c.multiply(1, 0, 1)));
                        if (Math.abs(d - r) < 1.1) { done.add(p.getUUID()); b.hit(p, b.atk(0.7), c, true); }
                    }
                });
            }
            b.later(base, () -> sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3f, 1.6f));
        }
    }

    // ================================================================ DẠNG 2 (own-move)

    /** Dạng 2: chọn bổ sung theo khoảng cách (hố trọng lực cần gần, tia thời gian xa hơn, nhân bản luôn được). */
    static boolean startF2(NguyenThuyTichDiet b, LivingEntity t) {
        double d = b.distanceTo(t);
        int[] pool = new int[3];
        int n = 0;
        if (d <= 14) pool[n++] = O_GRAVITY;
        if (d <= 18) pool[n++] = O_FREEZE;
        pool[n++] = O_ECHO;
        b.startOwn(pool[b.getRandom().nextInt(n)]);
        return true;
    }

    static boolean stepF2(NguyenThuyTichDiet b, ServerLevel sl, LivingEntity t, int kind, int i) {
        return switch (kind) {
            case O_GRAVITY -> gravity(b, sl, t, i);
            case O_FREEZE -> freeze(b, sl, t, i);
            case O_ECHO -> echo(b, sl, t, i);
            default -> true;
        };
    }

    /** "Hấp Dẫn Vực": hố trọng lực khoá tại vị trí người chơi (18 tick cảnh báo), hút người chơi 32 tick rồi nổ. */
    private static boolean gravity(NguyenThuyTichDiet b, ServerLevel sl, LivingEntity t, int i) {
        State xs = b.xs;
        if (i == 0) {
            xs.c = t != null ? t.position() : b.position();
            Fx.telegraphCircle(sl, null, xs.c, 9f, 18, VOIDC);
            Fx.rune(sl, null, xs.c.add(0, 0.1, 0), 9f, VOIDC, 18, 8);
            sl.playSound(null, xs.c.x, xs.c.y, xs.c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2f, 0.5f);
        }
        Vec3 c = xs.c;
        if (i >= 18 && i < 50) {
            if (i % 2 == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 6, 0.5, 0.5, 0.5, 0.2);
            for (Player p : b.players(sl, c, 9)) {
                Vec3 to = c.subtract(p.position()).multiply(1, 0, 1);
                if (to.lengthSqr() < 1e-4) continue;
                Vec3 n = to.normalize();
                p.setDeltaMovement(p.getDeltaMovement().add(n.x * 0.12, 0, n.z * 0.12));
                p.hurtMarked = true;
            }
        }
        if (i == 50) {
            for (Player p : b.players(sl, c, 3.2)) b.hit(p, b.atk(1.2), c, true);
            Fx.groundRing(sl, null, c, 3f, VOIDC, 14);
            Fx.shake(sl, c, 3f, 12, 30);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
            xs.last = O_GRAVITY;
        }
        return i >= 56;
    }

    /** "Đóng Băng Thời Khắc": tia băng dài 14 ô (18 tick cảnh báo) quét một đường; ai trúng bị làm chậm 4 giây. */
    private static boolean freeze(NguyenThuyTichDiet b, ServerLevel sl, LivingEntity t, int i) {
        State xs = b.xs;
        Vec3 from = b.position().add(0, b.getBbHeight() * 0.6, 0);
        if (i == 0) {
            Vec3 f = t != null ? t.position().subtract(b.position()).multiply(1, 0, 1) : Vec3.ZERO;
            xs.c = f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
            Fx.telegraphLine(sl, null, from, xs.c.scale(14), 1.6f, 18, VOIDC);
        }
        Vec3 dir = xs.c;
        if (i < 18 && i % 2 == 0) {
            for (double s = 1.5; s < 14; s += 1.0)
                sl.sendParticles(ParticleTypes.SNOWFLAKE, from.x + dir.x * s, from.y, from.z + dir.z * s, 1, 0.1, 0.1, 0.1, 0);
        }
        if (i == 18) {
            Fx.ribbon(sl, null, from, from.add(dir.scale(14)), 0, 4, 0, 0.6f, PALE);
            sl.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 3f, 0.5f);
            for (Player p : b.players(sl, from, 15)) {
                Vec3 rel = p.position().subtract(from).multiply(1, 0, 1);
                double along = rel.dot(dir), side = Math.abs(rel.x * dir.z - rel.z * dir.x);
                if (along > 0 && along < 14 && side < 1.6) {
                    b.hit(p, b.atk(0.9), from, false);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                }
            }
        }
        if (i > 18 && i < 40 && i % 3 == 0) {
            for (double s = 1.0; s < 14; s += 1.5)
                sl.sendParticles(ParticleTypes.END_ROD, from.x + dir.x * s, from.y, from.z + dir.z * s, 1, 0.1, 0.1, 0.1, 0);
        }
        if (i == 40) xs.last = O_FREEZE;
        return i >= 40;
    }

    /** "Ảnh Phân Thân": bóng nhân bản hiện ở điểm đối xứng qua thân (18 tick cảnh báo), lặp lại đòn trước tại đó 2 lần. */
    private static boolean echo(NguyenThuyTichDiet b, ServerLevel sl, LivingEntity t, int i) {
        State xs = b.xs;
        if (i == 0) {
            Vec3 self = b.position();
            Vec3 tp = t != null ? t.position() : self.add(0, 0, 4);
            Vec3 m = self.scale(2).subtract(tp);
            xs.c = new Vec3(m.x, self.y, m.z);
            xs.echoOf = xs.last;
            Fx.telegraphCircle(sl, null, xs.c, 3.5f, 20, PALE);
            Fx.rune(sl, null, xs.c.add(0, 0.1, 0), 3.5f, PALE, 20, 6);
            sl.sendParticles(ModParticles.HU_KHONG.get(), xs.c.x, xs.c.y + 1, xs.c.z, 30, 1.2, 1, 1.2, 0.05);
        }
        Vec3 m = xs.c;
        if (i < 20 && i % 2 == 0)
            sl.sendParticles(ParticleTypes.PORTAL, m.x, m.y + 1, m.z, 6, 0.4, 1, 0.4, 0.3);
        if (i == 20 || i == 32) echoStrike(b, sl, m, xs.echoOf);
        if (i >= 34) xs.last = O_ECHO;
        return i >= 34;
    }

    /** Nhân bản lặp đòn trước: sau hố trọng lực thì kéo + nổ, sau tia băng thì làm chậm, còn lại nổ thường. */
    private static void echoStrike(NguyenThuyTichDiet b, ServerLevel sl, Vec3 m, int prev) {
        double r = prev == O_GRAVITY ? 5 : prev == O_FREEZE ? 2.5 : 3.5;
        for (Player p : b.players(sl, m, r)) {
            if (prev == O_GRAVITY) {
                Vec3 n = m.subtract(p.position()).multiply(1, 0, 1);
                if (n.lengthSqr() > 1e-4) {
                    n = n.normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(n.x * 0.6, 0.2, n.z * 0.6));
                    p.hurtMarked = true;
                }
            }
            b.hit(p, b.atk(prev == O_FREEZE ? 0.7 : 1.0), m, prev != O_FREEZE);
            if (prev == O_FREEZE) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }
        Fx.groundRing(sl, null, m, (float) r, PALE, 14);
        sl.sendParticles(ParticleTypes.FLASH, m.x, m.y + 0.5, m.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ModParticles.HU_KHONG.get(), m.x, m.y + 1, m.z, 40, r * 0.3, 1, r * 0.3, 0.05);
    }
}
