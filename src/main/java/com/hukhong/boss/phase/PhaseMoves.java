package com.hukhong.boss.phase;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseConfig.Pal;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Thực thi chiêu phase dùng chung (tham số hoá bằng {@link Move}): telegraph mặt đất -> gồng -> nổ.
 * Chiêu tối thượng có vòng cảnh báo lớn; PULL_BURST hút người chơi vào tâm khi gồng (phải chạy ra ngoài vòng), DONUT an toàn ở tâm và ngoài rìa.
 */
public final class PhaseMoves {
    private PhaseMoves() {}

    /** Một lần thi triển đang diễn ra. */
    public static final class Run {
        public final Move move;
        public int left;
        public final List<Vec3> spots = new ArrayList<>();
        public Vec3 center, aim;
        /** Nhát/lớp đã ra (chiêu nhiều nhịp). */
        public int stage;
        public LivingEntity target;

        Run(Move move) { this.move = move; this.left = move.windup(); }
    }

    public static ParticleOptions particle(Pal p) {
        return switch (p) {
            case VOID -> ModParticles.HU_KHONG.get();
            case FROST -> ParticleTypes.SNOWFLAKE;
            case TOXIC -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case SAND -> ParticleTypes.POOF;
            case HOLY -> ParticleTypes.END_ROD;
            case BLOOD -> ParticleTypes.CRIMSON_SPORE;
            case MOON -> ParticleTypes.GLOW;
            case WOOD -> ParticleTypes.COMPOSTER;
            case BONE -> ParticleTypes.ASH;
            case SEA -> ParticleTypes.SPLASH;
        };
    }

    // ------------------------------------------------------------------ bắt đầu: telegraph
    public static Run begin(ServerLevel sl, Mob boss, Move m, LivingEntity target) {
        Run r = new Run(m);
        r.target = target;
        r.center = boss.position();
        Vec3 d = target != null ? target.position().subtract(boss.position()).multiply(1, 0, 1) : boss.getViewVector(1f).multiply(1, 0, 1);
        r.aim = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
        if (target != null) boss.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        int rgb = m.pal().rgb;
        switch (m.shape()) {
            case PILLARS -> {
                for (Player p : nearbyPlayers(sl, boss, 30, 5)) r.spots.add(p.position());
                if (target != null) for (int i = 0; i < 2; i++) r.spots.add(target.position().add(sl.random.nextGaussian() * 4, 0, sl.random.nextGaussian() * 4));
                for (Vec3 s : r.spots) Fx.telegraphCircle(sl, null, s, (float) m.radius(), m.windup(), rgb);
            }
            case METEOR -> {
                for (Player p : nearbyPlayers(sl, boss, 30, 4)) {
                    r.spots.add(p.position());
                    for (int i = 0; i < 2; i++) {
                        double a = sl.random.nextDouble() * Math.PI * 2, rr = 2 + sl.random.nextDouble() * 5;
                        r.spots.add(p.position().add(Math.cos(a) * rr, 0, Math.sin(a) * rr));
                    }
                }
                if (r.spots.isEmpty() && target != null) r.spots.add(target.position());
                for (Vec3 s : r.spots) Fx.telegraphCircle(sl, null, s, (float) m.radius(), m.windup(), rgb);
            }
            case LINE -> Fx.telegraphLine(sl, null, boss.position(), r.aim.scale(m.radius()), 3.4f, m.windup(), rgb);
            case SHOCK -> Fx.telegraphCircle(sl, null, boss.position(), (float) m.radius(), m.windup(), rgb);
            case PULL_BURST -> {
                Fx.telegraphCircle(sl, null, boss.position(), (float) m.radius(), m.windup(), 0xFF2030);
                Fx.rune(sl, null, boss.position().add(0, 0.1, 0), (float) m.radius() * 0.6f, rgb, m.windup(), 8);
            }
            case DONUT -> {
                Fx.telegraphCircle(sl, null, boss.position(), (float) m.radius(), m.windup(), 0xFF2030);
                Fx.telegraphCircle(sl, null, boss.position(), (float) (m.radius() * 0.4), m.windup(), 0x40FF80);
            }
            case COMBO -> Fx.telegraphCone(sl, null, boss.position(), r.aim, (float) m.radius(), 65, m.windup());
            case LEAP -> {
                Vec3 s = target != null ? target.position() : boss.position().add(r.aim.scale(8));
                r.spots.add(s);
                Fx.telegraphCircle(sl, null, s, (float) m.radius(), m.windup(), 0xFF2030);
                Fx.rune(sl, null, s.add(0, 0.1, 0), (float) m.radius() * 0.7f, rgb, m.windup(), 6);
            }
            case SPIN -> Fx.telegraphCircle(sl, null, boss.position(), (float) m.radius(), m.windup(), rgb);
            case NOVA -> {
                for (int i = 1; i <= 3; i++) Fx.telegraphCircle(sl, null, boss.position(), (float) (m.radius() * i / 3.0), m.windup() + (i - 1) * NOVA_GAP, rgb);
            }
            case SKY_SWORD -> {
                Vec3 s = target != null ? target.position() : boss.position().add(r.aim.scale(6));
                r.spots.add(s);
                Fx.telegraphCircle(sl, null, s, (float) m.radius(), m.windup(), 0xFF2030);
                Fx.giantSword(sl, null, s, (float) m.radius() * 0.5f, rgb, m.windup(), m.windup() + 30);
            }
        }
        Component name = Component.translatable("hukhong.boss.move." + m.id());
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(boss) < 48 * 48) p.displayClientMessage(m.ultimate() ? name.copy().withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD) : name, true);
        boss.playSound(m.ultimate() ? SoundEvents.WARDEN_SONIC_CHARGE : SoundEvents.WARDEN_ATTACK_IMPACT, m.ultimate() ? 3f : 1.6f, 0.7f);
        return r;
    }

    private static List<Player> nearbyPlayers(ServerLevel sl, Mob boss, double range, int max) {
        List<Player> out = new ArrayList<>(sl.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(range), p -> !p.isSpectator() && p.isAlive()));
        return out.size() > max ? out.subList(0, max) : out;
    }

    // ------------------------------------------------------------------ mỗi tick gồng
    public static void tick(ServerLevel sl, Mob boss, Run r) {
        Move m = r.move;
        boss.getNavigation().stop();
        if (m.shape() == BossPhaseConfig.Shape.PULL_BURST && r.left % 2 == 0) {
            double rad = m.radius();
            for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(r.center, r.center).inflate(rad, 4, rad))) {
                if (p.isSpectator() || p.isCreative()) continue;
                Vec3 to = r.center.subtract(p.position()).multiply(1, 0, 1);
                double dist = to.length();
                if (dist > rad || dist < 1.5) continue;
                Vec3 pull = to.normalize().scale(0.13);
                p.setDeltaMovement(p.getDeltaMovement().add(pull.x, 0, pull.z));
                p.hurtMarked = true;
            }
        }
        if (m.ultimate() && r.left % 4 == 0) {
            double a0 = (r.left * 0.37) % (Math.PI * 2);
            for (int i = 0; i < 12; i++) {
                double a = a0 + i * Math.PI * 2 / 12, rr = m.radius() * (m.shape() == BossPhaseConfig.Shape.PULL_BURST ? (r.left % 40) / 40.0 : 1.0);
                sl.sendParticles(particle(m.pal()), r.center.x + Math.cos(a) * rr, r.center.y + 0.3, r.center.z + Math.sin(a) * rr, 1, 0.05, 0.1, 0.05, 0.0);
            }
        }
        if (m.shape() == BossPhaseConfig.Shape.LEAP && r.stage == 0 && !r.spots.isEmpty()) {
            int el = m.windup() - r.left;
            if (el == m.windup() / 2) {  // bật lên cao
                boss.setDeltaMovement(0, 1.4, 0);
                boss.hurtMarked = true;
                sl.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY(), boss.getZ(), 30, 1.2, 0.2, 1.2, 0.08);
                boss.playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
            } else if (el > m.windup() / 2 && r.left <= 4) {  // lơ lửng trên điểm rơi
                Vec3 s = r.spots.get(0);
                boss.teleportTo(s.x, s.y + 6, s.z);
                boss.setDeltaMovement(0, -2.5, 0);
            }
        }
        if (r.left % 6 == 0) sl.sendParticles(particle(m.pal()), boss.getX(), boss.getY() + boss.getBbHeight() * 0.6, boss.getZ(), 8, 0.8, 0.8, 0.8, 0.03);
    }

    /** Khoảng tick giữa các nhịp của chiêu nhiều nhịp. */
    public static final int COMBO_GAP = 12, SPIN_GAP = 14, NOVA_GAP = 12;

    /** Ra đòn. Trả về số tick tới nhịp kế tiếp (chiêu nhiều nhịp), hoặc 0 nếu chiêu đã xong. */
    public static int fireStage(ServerLevel sl, Mob boss, PhasedBoss pb, Run r) {
        Move m = r.move;
        ParticleOptions pt = particle(m.pal());
        int rgb = m.pal().rgb;
        int st = r.stage++;
        switch (m.shape()) {
            case COMBO -> {
                if (r.target != null && r.target.isAlive()) {
                    Vec3 d = r.target.position().subtract(boss.position()).multiply(1, 0, 1);
                    if (d.lengthSqr() > 1e-3) r.aim = d.normalize();
                    boss.setDeltaMovement(r.aim.x * 0.9, 0.1, r.aim.z * 0.9);  // lướt tới mỗi nhát
                    boss.hurtMarked = true;
                }
                double rad = m.radius() * (st == 2 ? 1.35 : 1.0);
                cone(sl, boss, pb, boss.position(), r.aim, rad, st == 2 ? 80 : 65, m, st == 2 ? 1.4 : 0.8);
                arc(sl, boss, r.aim, rad, st == 1 ? -35 : st == 2 ? 80 : 35, rgb);
                boss.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f + st * 0.15f);
                if (st < 2) {
                    Fx.telegraphCone(sl, null, boss.position(), r.aim, (float) (m.radius() * (st == 1 ? 1.35 : 1.0)), st == 1 ? 80 : 65, COMBO_GAP);
                    return st == 0 ? COMBO_GAP + 8 : COMBO_GAP;  // nhịp 2 trễ hơn để bắt người lăn sớm
                }
                return 0;
            }
            case LEAP -> {
                Vec3 s = r.spots.isEmpty() ? boss.position() : r.spots.get(0);
                boss.teleportTo(s.x, s.y, s.z);
                hurtIn(sl, boss, pb, s, m.radius(), m);
                Fx.groundCrack(sl, null, s, (float) m.radius(), rgb, 10, 40);
                Fx.groundRing(sl, null, s, (float) m.radius() * 1.3f, rgb, 20);
                Fx.shake(sl, s, 3f, 20, 36f);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
                sl.sendParticles(pt, s.x, s.y + 1, s.z, 40, m.radius() * 0.4, 0.8, m.radius() * 0.4, 0.08);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
                return 0;
            }
            case SPIN -> {
                double rad = m.radius();
                hurtIn(sl, boss, pb, boss.position(), rad, m);
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2 + st;
                    arc(sl, boss, new Vec3(Math.cos(a), 0, Math.sin(a)), rad, 0, rgb);
                }
                ring(sl, boss.position(), rad, pt);
                boss.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.6f + st * 0.2f);
                if (st < 2) {
                    if (r.target != null && r.target.isAlive()) {
                        Vec3 d = r.target.position().subtract(boss.position()).multiply(1, 0, 1);
                        if (d.lengthSqr() > 4) { Vec3 v = d.normalize().scale(1.3); boss.setDeltaMovement(v.x, 0.25, v.z); boss.hurtMarked = true; }
                    }
                    Fx.telegraphCircle(sl, null, boss.position(), (float) rad, SPIN_GAP, rgb);
                    return SPIN_GAP;
                }
                return 0;
            }
            case NOVA -> {
                double outer = m.radius() * (st + 1) / 3.0, inner = Math.max(0, outer - m.radius() / 3.0);
                for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(r.center, r.center).inflate(outer, 3, outer))) {
                    if (p.isSpectator() || !p.isAlive() || !p.onGround()) continue;
                    double d2 = p.position().multiply(1, 0, 1).distanceToSqr(r.center.multiply(1, 0, 1));
                    if (d2 > outer * outer || d2 < inner * inner) continue;
                    hit(sl, boss, pb, p, m);
                }
                Fx.groundRing(sl, null, r.center, (float) outer, rgb, 14);
                ring(sl, r.center, outer, pt);
                Fx.shake(sl, r.center, 1.5f, 10, 30f);
                boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.7f + st * 0.2f);
                return st < 2 ? NOVA_GAP : 0;
            }
            case SKY_SWORD -> {
                Vec3 s = r.spots.isEmpty() ? boss.position() : r.spots.get(0);
                hurtIn(sl, boss, pb, s, m.radius(), m);
                Fx.groundCrack(sl, null, s, (float) m.radius() * 1.2f, rgb, 14, 60);
                Fx.rift(sl, null, s, r.aim.scale(m.radius() * 1.5), 2.5f, rgb, 50);
                Fx.shake(sl, s, 4f, 30, 60f);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 1, s.z, 2, 1, 0.2, 1, 0);
                sl.sendParticles(pt, s.x, s.y + 2, s.z, 80, m.radius() * 0.5, 2, m.radius() * 0.5, 0.15);
                boss.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.6f);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
                return 0;
            }
            default -> { fire(sl, boss, pb, r); return 0; }
        }
    }

    /** Vệt kiếm khí lưỡi liềm quét từ thân boss theo hướng dir. */
    private static void arc(ServerLevel sl, Mob boss, Vec3 dir, double rad, double roll, int rgb) {
        Fx.qi(sl, boss.position().add(0, boss.getBbHeight() * 0.45, 0), dir.scale(rad * 0.8), (float) (rad * 0.6), rgb, roll, 10);
    }

    /** Trúng người chơi trong hình quạt trước mặt. */
    private static void cone(ServerLevel sl, Mob boss, PhasedBoss pb, Vec3 c, Vec3 aim, double rad, double halfDeg, Move m, double mul) {
        double cos = Math.cos(Math.toRadians(halfDeg));
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(rad, 3, rad))) {
            if (p.isSpectator() || !p.isAlive()) continue;
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double d = rel.length();
            if (d > rad || (d > 1.2 && rel.normalize().dot(aim) < cos)) continue;
            p.hurt(boss.damageSources().mobAttack(boss), pb.moveDamage(m.dmg() * mul));
            applyHit(p, m.hit());
        }
    }

    // ------------------------------------------------------------------ nổ
    public static void fire(ServerLevel sl, Mob boss, PhasedBoss pb, Run r) {
        Move m = r.move;
        ParticleOptions pt = particle(m.pal());
        int rgb = m.pal().rgb;
        switch (m.shape()) {
            case PILLARS -> {
                for (Vec3 s : r.spots) {
                    hurtIn(sl, boss, pb, s, m.radius(), m);
                    Fx.elem(sl, null, Fx.E_SPIKES, s, Vec3.ZERO, (float) m.radius() * 0.5f, rgb, 0);
                    sl.sendParticles(pt, s.x, s.y + 1, s.z, 24, 0.5, 1.4, 0.5, 0.05);
                }
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.9f);
            }
            case METEOR -> {
                for (Vec3 s : r.spots) {
                    hurtIn(sl, boss, pb, s, m.radius(), m);
                    Fx.groundRing(sl, null, s, (float) m.radius(), rgb, 18);
                    sl.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.5, s.z, 2, 0.4, 0.2, 0.4, 0);
                    sl.sendParticles(pt, s.x, s.y + 1.5, s.z, 20, 0.6, 1.2, 0.6, 0.05);
                }
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 1.1f);
            }
            case LINE -> {
                double len = m.radius();
                Vec3 a = r.center, b = a.add(r.aim.scale(len));
                for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(a, b).inflate(3, 4, 3))) {
                    if (p.isSpectator() || !p.isAlive()) continue;
                    Vec3 rel = p.position().subtract(a).multiply(1, 0, 1);
                    double t = rel.dot(r.aim);
                    if (t < -1 || t > len + 1) continue;
                    double perp = rel.subtract(r.aim.scale(t)).length();
                    if (perp > 2.0 || Math.abs(p.getY() - a.y) > 4) continue;
                    hit(sl, boss, pb, p, m);
                }
                for (double t = 1; t < len; t += 1.0) {
                    Vec3 q = a.add(r.aim.scale(t));
                    sl.sendParticles(pt, q.x, q.y + 1.0, q.z, 4, 0.6, 0.6, 0.6, 0.02);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, q.x, q.y + 1.2, q.z, 1, 0.3, 0.3, 0.3, 0);
                }
                boss.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.6f);
            }
            case SHOCK -> {
                double rad = m.radius();
                for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(r.center, r.center).inflate(rad, 3, rad))) {
                    if (p.isSpectator() || !p.isAlive() || p.position().distanceToSqr(r.center) > rad * rad) continue;
                    if (!p.onGround()) continue;  // nhảy lên là né được
                    hit(sl, boss, pb, p, m);
                }
                Fx.groundRing(sl, null, r.center, (float) rad, rgb, 24);
                ring(sl, r.center, rad, pt);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.7f);
            }
            case PULL_BURST -> {
                double rad = m.radius();
                for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(r.center, r.center).inflate(rad, 4, rad))) {
                    if (p.isSpectator() || !p.isAlive() || p.position().distanceToSqr(r.center) > rad * rad) continue;
                    hit(sl, boss, pb, p, m);
                }
                Fx.groundCrack(sl, null, r.center, (float) rad * 0.6f, rgb, 8, 40);
                Fx.shake(sl, r.center, 3f, 30, 48f);
                ring(sl, r.center, rad, pt);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, r.center.x, r.center.y + 1, r.center.z, 1, 0, 0, 0, 0);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
            }
            case DONUT -> {
                double rad = m.radius(), in = rad * 0.4;
                for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(r.center, r.center).inflate(rad, 4, rad))) {
                    if (p.isSpectator() || !p.isAlive()) continue;
                    double d2 = p.position().multiply(1, 0, 1).distanceToSqr(r.center.multiply(1, 0, 1));
                    if (d2 > rad * rad || d2 < in * in) continue;
                    hit(sl, boss, pb, p, m);
                }
                Fx.groundCrack(sl, null, r.center, (float) rad * 0.7f, rgb, 12, 40);
                Fx.shake(sl, r.center, 2.5f, 30, 48f);
                ring(sl, r.center, rad, pt);
                ring(sl, r.center, in, pt);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.6f);
            }
        }
    }

    private static void ring(ServerLevel sl, Vec3 c, double rad, ParticleOptions pt) {
        for (int i = 0; i < 56; i++) {
            double a = i * Math.PI * 2 / 56;
            sl.sendParticles(pt, c.x + Math.cos(a) * rad, c.y + 0.3, c.z + Math.sin(a) * rad, 1, 0.1, 0.2, 0.1, 0.0);
        }
    }

    private static void hurtIn(ServerLevel sl, Mob boss, PhasedBoss pb, Vec3 c, double rad, Move m) {
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(rad, 3, rad))) {
            if (p.isSpectator() || !p.isAlive() || p.position().multiply(1, 0, 1).distanceToSqr(c.multiply(1, 0, 1)) > rad * rad) continue;
            hit(sl, boss, pb, p, m);
        }
    }

    private static void hit(ServerLevel sl, Mob boss, PhasedBoss pb, Player p, Move m) {
        p.hurt(boss.damageSources().mobAttack(boss), pb.moveDamage(m.dmg()));
        applyHit(p, m.hit());
    }

    public static void applyHit(Player p, Hit h) {
        switch (h) {
            case KNOCK_UP -> { p.setDeltaMovement(p.getDeltaMovement().add(0, 0.85, 0)); p.hurtMarked = true; }
            case SLOW -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            case BLIND -> p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            case WEAK -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            case POISON -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            case FREEZE -> { p.setTicksFrozen(p.getTicksRequiredToFreeze() + 100); p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)); }
            case NAUSEA -> p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            case NONE -> { }
        }
    }
}
