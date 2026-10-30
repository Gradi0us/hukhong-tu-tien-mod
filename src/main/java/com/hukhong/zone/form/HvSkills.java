package com.hukhong.zone.form;

import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.entity.HuSkill;
import com.hukhong.entity.ModEntities;
import com.hukhong.entity.PoisonSpit;
import com.hukhong.vfx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Thi trien chieu rieng cac dang Hu Vuong ({@link HvSkill}): telegraph mat dat -> gong -> no.
 * Moi chieu dung mau/hat/FxElem rieng (Pal + elem) de khong na na nhau. Moi thu chay phia server.
 */
public final class HvSkills {
    private HvSkills() {}

    /** Mot lan thi trien dang gong. */
    public static final class Cast {
        public final HvSkill skill;
        public int left;
        public final List<Vec3> spots = new ArrayList<>();
        public Vec3 center = Vec3.ZERO, aim = new Vec3(0, 0, 1);
        public LivingEntity target;

        Cast(HvSkill s) { this.skill = s; this.left = s.windup; }
    }

    /** Vung nguy hiem keo dai (ao doc, loc cat, xoay nuoc, vuc hu vo). */
    public static final class Hazard {
        final Vec3 c;
        final double r;
        int left;
        final HvSkill skill;

        Hazard(Vec3 c, double r, int left, HvSkill skill) { this.c = c; this.r = r; this.left = left; this.skill = skill; }
    }

    // ------------------------------------------------------------------ tien ich
    private static List<Player> players(ServerLevel sl, Vec3 c, double r, double h) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, h, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static List<Player> near(ServerLevel sl, Mob boss, double range, int max) {
        List<Player> l = new ArrayList<>(players(sl, boss.position(), range, 6));
        return l.size() > max ? l.subList(0, max) : l;
    }

    private static Vec3 flat(Vec3 v) { return new Vec3(v.x, 0, v.z); }

    private static void hit(HuVuongForm boss, Player p, HvSkill s, double mulOverride) {
        p.hurt(boss.damageSources().mobAttack(boss), boss.dmg(mulOverride));
        PhaseMoves.applyHit(p, s.hit);
        HuSkill.hu(p, 80);
    }

    private static void hit(HuVuongForm boss, Player p, HvSkill s) { hit(boss, p, s, s.dmg); }

    private static void ring(ServerLevel sl, Vec3 c, double rad, ParticleOptions pt) {
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            sl.sendParticles(pt, c.x + Math.cos(a) * rad, c.y + 0.3, c.z + Math.sin(a) * rad, 1, 0.1, 0.2, 0.1, 0.0);
        }
    }

    // ------------------------------------------------------------------ bat dau: telegraph
    public static Cast begin(ServerLevel sl, HuVuongForm boss, HvSkill s, LivingEntity target) {
        Cast c = new Cast(s);
        c.target = target;
        c.center = boss.position();
        Vec3 d = target != null ? flat(target.position().subtract(boss.position())) : flat(boss.getViewVector(1f));
        c.aim = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
        if (target != null) boss.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        int rgb = s.pal.rgb, w = s.windup;
        Vec3 bp = boss.position();
        switch (s.kind) {
            case CONE -> Fx.telegraphCone(sl, null, bp, c.aim, (float) s.r, 55f, w);
            case SWALLOW -> Fx.telegraphCone(sl, null, bp, c.aim, (float) s.r, 40f, w);
            case SPIT -> Fx.telegraphCone(sl, null, bp, c.aim, 14f, 35f, w);
            case DRAIN, ROAR, RING, JUMP_RING, GAS, DRAG, WHIRL -> Fx.telegraphCircle(sl, null, bp, (float) s.r, w, rgb);
            case DASH -> Fx.telegraphLine(sl, null, bp, c.aim.scale(s.r), 3.2f, w, rgb);
            case WAVE -> Fx.telegraphLine(sl, null, bp, c.aim.scale(s.r), 6f, w, rgb);
            case ROOT, THORNS, METEOR, PILLARS, LIGHTNING, POOLS -> {
                int extra = s.kind == HvSkill.Kind.THORNS || s.kind == HvSkill.Kind.PILLARS ? 3 : 1;
                for (Player p : near(sl, boss, 30, 5)) c.spots.add(p.position());
                if (target != null) for (int i = 0; i < extra; i++)
                    c.spots.add(target.position().add(sl.random.nextGaussian() * 4, 0, sl.random.nextGaussian() * 4));
                for (Vec3 sp : c.spots) Fx.telegraphCircle(sl, null, sp, (float) s.r, w, rgb);
            }
            case BEAMS -> {
                for (Player p : near(sl, boss, 30, 3)) c.spots.add(p.position());
                if (c.spots.isEmpty() && target != null) c.spots.add(target.position());
                for (Vec3 sp : c.spots) {
                    Vec3 dir = flat(sp.subtract(bp));
                    if (dir.lengthSqr() > 1e-4) Fx.telegraphLine(sl, null, bp, dir.normalize().scale(s.r), 2.4f, w, rgb);
                }
            }
            case BURROW -> {
                c.spots.add(target != null ? target.position() : bp);
                Fx.telegraphCircle(sl, null, c.spots.get(0), (float) s.r, w, rgb);
            }
            case TORNADO, TELEPORT -> {
                c.spots.add(target != null ? target.position() : bp);
                Fx.telegraphCircle(sl, null, c.spots.get(0), (float) s.r, w, rgb);
            }
            case SUMMON, HEAL_TREE, MIRROR, SHIELD -> Fx.rune(sl, null, bp.add(0, 0.1, 0), (float) Math.max(3, s.r * 0.5), rgb, w, 8);
        }
        Component name = Component.translatable(s.langKey());
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(boss) < 48 * 48) p.displayClientMessage(name, true);
        boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.6f, 0.7f);
        return c;
    }

    // ------------------------------------------------------------------ moi tick gong
    public static void tick(ServerLevel sl, HuVuongForm boss, Cast c) {
        HvSkill s = c.skill;
        boss.getNavigation().stop();
        ParticleOptions pt = PhaseMoves.particle(s.pal);
        double y = boss.getY() + boss.getBbHeight() * 0.5;
        switch (s.kind) {
            case DRAG -> {
                if (c.left % 2 == 0) for (Player p : players(sl, c.center, s.r, 4)) {
                    Vec3 to = flat(c.center.subtract(p.position()));
                    double d = to.length();
                    if (d < 2) continue;
                    Vec3 pull = to.normalize().scale(0.16);
                    p.setDeltaMovement(p.getDeltaMovement().add(pull.x, 0, pull.z));
                    p.hurtMarked = true;
                }
            }
            case GAS -> {
                double rr = s.r * (1 - c.left / (double) s.windup);
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.center.x, c.center.y + 1, c.center.z, 14, rr * 0.5, 0.8, rr * 0.5, 0.01);
                sl.sendParticles(ParticleTypes.SQUID_INK, c.center.x, c.center.y + 1.5, c.center.z, 3, rr * 0.4, 0.8, rr * 0.4, 0.0);
            }
            case BURROW -> {
                sl.sendParticles(ParticleTypes.POOF, boss.getX(), boss.getY() + 0.2, boss.getZ(), 10, boss.getBbWidth() * 0.5, 0.1, boss.getBbWidth() * 0.5, 0.05);
                if (c.left == s.windup - 12) boss.setBurrowing(true);
                if (c.left < s.windup - 12 && c.left % 3 == 0) sl.sendParticles(pt, c.spots.get(0).x, c.spots.get(0).y + 0.2, c.spots.get(0).z, 6, s.r * 0.4, 0.1, s.r * 0.4, 0.05);
            }
            case JUMP_RING, ROAR, RING -> { if (c.left % 4 == 0) ring(sl, c.center, s.r * (1 - c.left / (double) s.windup), pt); }
            default -> { }
        }
        if (c.left % 5 == 0) sl.sendParticles(pt, boss.getX(), y, boss.getZ(), 8, 0.8, 0.8, 0.8, 0.03);
    }

    // ------------------------------------------------------------------ no
    public static void fire(ServerLevel sl, HuVuongForm boss, Cast c) {
        HvSkill s = c.skill;
        int rgb = s.pal.rgb;
        ParticleOptions pt = PhaseMoves.particle(s.pal);
        Vec3 bp = boss.position();
        switch (s.kind) {
            case CONE -> {
                for (Player p : players(sl, bp, s.r, 3)) if (inCone(bp, c.aim, p.position(), s.r, 55)) hit(boss, p, s);
                Fx.elem(sl, null, s.elem, bp.add(0, 1, 0), c.aim, (float) s.r, rgb, 0);
                for (double t = 1; t < s.r; t += 1.2) {
                    Vec3 q = bp.add(c.aim.scale(t));
                    sl.sendParticles(pt, q.x, q.y + 1.2, q.z, 3, 0.7, 0.5, 0.7, 0.02);
                }
                boss.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f);
            }
            case DRAIN -> {
                double healed = 0;
                for (Player p : players(sl, bp, s.r, 4)) {
                    float before = p.getHealth();
                    hit(boss, p, s);
                    healed += Math.max(0, before - p.getHealth());
                    Fx.ribbon(sl, null, p.position().add(0, 1, 0), bp.add(0, boss.getBbHeight() * 0.6, 0), 0.4, 16, 0, 0.35f, rgb);
                }
                boss.heal((float) (healed * 2.0 + boss.getMaxHealth() * 0.005));
                Fx.elem(sl, null, s.elem, bp.add(0, 1, 0), Vec3.ZERO, (float) (s.r * 0.5), rgb, 0);
                boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 1.6f);
            }
            case SUMMON -> {
                int have = sl.getEntitiesOfClass(Monster.class, boss.getBoundingBox().inflate(24), e -> e != boss && e instanceof com.hukhong.entity.HuMob).size();
                for (int i = 0; i < 3 && have + i < 8; i++) {
                    EntityType<? extends Mob> type = sl.random.nextBoolean() ? ModEntities.HU_THI.get() : ModEntities.HU_COT.get();
                    Mob m = type.create(sl);
                    if (m == null) continue;
                    double a = sl.random.nextDouble() * Math.PI * 2;
                    m.moveTo(boss.getX() + Math.cos(a) * 3.5, boss.getY(), boss.getZ() + Math.sin(a) * 3.5, sl.random.nextFloat() * 360f, 0f);
                    m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    m.setTarget(boss.getTarget());
                    sl.addFreshEntity(m);
                }
                Fx.elem(sl, null, Fx.E_HALOS, bp.add(0, 0.6, 0), Vec3.ZERO, 3f, rgb, 0);
                sl.sendParticles(pt, bp.x, bp.y + 1, bp.z, 40, 2, 1, 2, 0.05);
                boss.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5f, 0.7f);
            }
            case DASH -> { boss.startDash(c.aim, s); boss.playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f); }
            case RING -> {
                for (Player p : players(sl, bp, s.r, 3)) {
                    hit(boss, p, s);
                    Vec3 away = flat(p.position().subtract(bp)).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(away.x * 1.1, 0.35, away.z * 1.1));
                    p.hurtMarked = true;
                }
                Fx.groundRing(sl, null, bp, (float) s.r, rgb, 24);
                Fx.elem(sl, null, s.elem, bp, Vec3.ZERO, (float) s.r, rgb, 0);
                ring(sl, bp, s.r, pt);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.8f);
            }
            case ROAR -> {
                for (Player p : players(sl, bp, s.r, 4)) {
                    hit(boss, p, s);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6));  // choang ~1.5s
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 2));
                }
                Fx.shake(sl, bp, 3f, 24, 40f);
                Fx.elem(sl, null, s.elem, bp.add(0, boss.getBbHeight() * 0.7, 0), Vec3.ZERO, (float) (s.r * 0.6), rgb, 0);
                ring(sl, bp, s.r, pt);
                boss.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.6f);
            }
            case ROOT -> {
                for (Vec3 sp : c.spots) {
                    for (Player p : players(sl, sp, s.r, 3)) {
                        hit(boss, p, s);
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                        p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1));
                    }
                    Fx.elem(sl, null, s.elem, sp, Vec3.ZERO, (float) s.r, rgb, 0);
                    sl.sendParticles(pt, sp.x, sp.y + 0.5, sp.z, 20, 0.6, 0.6, 0.6, 0.03);
                }
                boss.playSound(SoundEvents.ROOTED_DIRT_BREAK, 2f, 0.6f);
            }
            case THORNS, PILLARS, METEOR, LIGHTNING -> {
                for (Vec3 sp : c.spots) {
                    for (Player p : players(sl, sp, s.r, 3)) hit(boss, p, s);
                    Fx.elem(sl, null, s.elem, sp, Vec3.ZERO, (float) s.r * 0.6f, rgb, 0);
                    Fx.groundRing(sl, null, sp, (float) s.r, rgb, 14);
                    sl.sendParticles(pt, sp.x, sp.y + 1, sp.z, 20, 0.5, 1.2, 0.5, 0.05);
                    if (s.kind == HvSkill.Kind.LIGHTNING) {
                        net.minecraft.world.entity.LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
                        if (b != null) { b.moveTo(sp.x, sp.y, sp.z); b.setVisualOnly(true); sl.addFreshEntity(b); }
                    }
                }
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, s.kind == HvSkill.Kind.METEOR ? 1.3f : 0.9f);
            }
            case HEAL_TREE -> {
                int trees = countLogs(sl, boss.blockPosition(), (int) s.r);
                float frac = 0.03f + 0.01f * Math.min(7, trees);
                boss.heal(boss.getMaxHealth() * frac);
                Fx.elem(sl, null, s.elem, bp.add(0, 1, 0), Vec3.ZERO, 4f, rgb, 0);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, bp.x, bp.y + 2, bp.z, 40, 2, 2, 2, 0.1);
                boss.playSound(SoundEvents.AZALEA_LEAVES_BREAK, 2f, 0.6f);
            }
            case MIRROR -> {
                boss.setMirror(120);
                Fx.shell(sl, null, boss, 3f, rgb, 120);
                boss.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
            }
            case SHIELD -> {
                boss.setAbsorptionAmount(boss.getMaxHealth() * 0.04f);
                boss.heal(boss.getMaxHealth() * 0.02f);
                Fx.shell(sl, null, boss, 3.2f, rgb, 100);
                boss.playSound(SoundEvents.BEACON_ACTIVATE, 2f, 1.1f);
            }
            case BURROW -> {
                Vec3 at = c.target != null && c.target.isAlive() ? c.target.position() : c.spots.get(0);
                boss.setBurrowing(false);
                boss.teleportTo(at.x, at.y, at.z);
                for (Player p : players(sl, at, s.r, 3)) hit(boss, p, s);
                Fx.elem(sl, null, s.elem, at, Vec3.ZERO, (float) s.r * 0.7f, rgb, 0);
                Fx.groundCrack(sl, null, at, (float) s.r, rgb, 8, 30);
                Fx.shake(sl, at, 2.5f, 20, 32f);
                sl.sendParticles(pt, at.x, at.y + 0.5, at.z, 50, s.r * 0.4, 0.5, s.r * 0.4, 0.1);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
            }
            case TORNADO -> {
                boss.addHazard(new Hazard(c.spots.get(0), s.r, 80, s));
                Fx.elem(sl, null, s.elem, c.spots.get(0), c.aim, (float) s.r, rgb, 80);
                boss.playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 2f, 0.6f);
            }
            case WHIRL -> {
                boss.addHazard(new Hazard(bp, s.r, 90, s));
                Fx.pool(sl, null, bp, (float) s.r, rgb, 90);
                boss.playSound(SoundEvents.ELDER_GUARDIAN_AMBIENT, 2f, 0.6f);
            }
            case POOLS -> {
                for (Vec3 sp : c.spots) {
                    boss.addHazard(new Hazard(sp, s.r, 100, s));
                    Fx.pool(sl, null, sp, (float) s.r, rgb, 100);
                }
                boss.playSound(SoundEvents.SLIME_SQUISH, 2f, 0.5f);
            }
            case SWALLOW -> {
                for (Player p : players(sl, bp, s.r, 3)) {
                    if (!inCone(bp, c.aim, p.position(), s.r, 40)) continue;
                    hit(boss, p, s);
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    Vec3 spit = bp.add(c.aim.scale(-7));
                    p.teleportTo(spit.x, bp.y, spit.z);   // nuot roi nhả ra sau lung
                    p.setDeltaMovement(c.aim.scale(-0.8).add(0, 0.5, 0));
                    p.hurtMarked = true;
                }
                Fx.elem(sl, null, s.elem, bp.add(0, boss.getBbHeight() * 0.7, 0), c.aim, (float) s.r, rgb, 0);
                boss.playSound(SoundEvents.GENERIC_EAT, 2.5f, 0.5f);
            }
            case SPIT -> {
                double base = boss.dmg(s.dmg);
                Vec3 from = bp.add(0, boss.getBbHeight() * 0.7, 0).add(c.aim.scale(1.5));
                double a0 = Math.atan2(c.aim.z, c.aim.x);
                for (int i = 0; i < 7; i++) {
                    double a = a0 + Math.toRadians((i - 3) * 11);
                    PoisonSpit.launchDir(boss, from, new Vec3(Math.cos(a), 0.18, Math.sin(a)), (float) base, 2.6, 1.1f).withPoison(120, 1);
                }
                boss.playSound(SoundEvents.SLIME_SQUISH, 2f, 0.5f);
            }
            case GAS -> {
                for (Player p : players(sl, bp, s.r, 4)) {
                    hit(boss, p, s);
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    Vec3 away = flat(p.position().subtract(bp)).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(away.x * 0.9, 0.4, away.z * 0.9));
                    p.hurtMarked = true;
                }
                Fx.elem(sl, null, s.elem, bp.add(0, 1, 0), Vec3.ZERO, (float) s.r, rgb, 0);
                Fx.groundCrack(sl, null, bp, (float) s.r * 0.6f, rgb, 8, 30);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, bp.x, bp.y + 1, bp.z, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, bp.x, bp.y + 1, bp.z, 80, s.r * 0.4, 1, s.r * 0.4, 0.05);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
            }
            case WAVE -> {
                Vec3 b = bp.add(c.aim.scale(s.r));
                for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(bp, b).inflate(4, 4, 4), q -> !q.isSpectator() && q.isAlive())) {
                    Vec3 rel = flat(p.position().subtract(bp));
                    double t = rel.dot(c.aim), perp = rel.subtract(c.aim.scale(t)).length();
                    if (t < -1 || t > s.r + 1 || perp > 3.2) continue;
                    hit(boss, p, s);
                    p.setDeltaMovement(p.getDeltaMovement().add(c.aim.x * 1.4, 0.45, c.aim.z * 1.4));
                    p.hurtMarked = true;
                }
                Fx.elem(sl, null, s.elem, bp.add(0, 0.2, 0), c.aim, (float) s.r, rgb, 0);
                for (double t = 1; t < s.r; t += 1.0) {
                    Vec3 q = bp.add(c.aim.scale(t));
                    sl.sendParticles(ParticleTypes.SPLASH, q.x, q.y + 0.8, q.z, 6, 1.5, 0.5, 1.5, 0.1);
                }
                boss.playSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.5f, 0.5f);
            }
            case DRAG -> {
                for (Player p : players(sl, bp, s.r, 4)) {
                    hit(boss, p, s);
                    Vec3 to = flat(bp.subtract(p.position())).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(to.x * 1.0, -0.2, to.z * 1.0));
                    p.hurtMarked = true;
                }
                Fx.elem(sl, null, s.elem, bp, Vec3.ZERO, (float) s.r * 0.6f, rgb, 0);
                Fx.shake(sl, bp, 2f, 20, 32f);
                ring(sl, bp, s.r, pt);
                boss.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2f, 0.7f);
            }
            case JUMP_RING -> {
                for (Player p : players(sl, bp, s.r, 3)) if (p.onGround()) hit(boss, p, s);  // nhay len la ne
                Fx.groundRing(sl, null, bp, (float) s.r, rgb, 24);
                Fx.elem(sl, null, s.elem, bp.add(0, 0.5, 0), Vec3.ZERO, (float) s.r * 0.6f, rgb, 0);
                ring(sl, bp, s.r, pt);
                boss.playSound(SoundEvents.BELL_RESONATE, 3f, 0.6f);
            }
            case BEAMS -> {
                Vec3 from = bp.add(0, boss.getBbHeight() * 0.75, 0);
                for (Vec3 sp : c.spots) {
                    Vec3 dir = flat(sp.subtract(bp));
                    if (dir.lengthSqr() < 1e-4) continue;
                    dir = dir.normalize();
                    for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(bp, bp.add(dir.scale(s.r))).inflate(3, 4, 3), q -> !q.isSpectator() && q.isAlive())) {
                        Vec3 rel = flat(p.position().subtract(bp));
                        double t = rel.dot(dir), perp = rel.subtract(dir.scale(t)).length();
                        if (t < -1 || t > s.r + 1 || perp > 1.8) continue;
                        hit(boss, p, s);
                    }
                    Fx.ribbon(sl, null, from, from.add(dir.scale(s.r)), 0.0, 6, 0, 0.7f, rgb);
                    Fx.elem(sl, null, s.elem, from, dir, 10f, rgb, 0);
                }
                boss.playSound(SoundEvents.BEACON_POWER_SELECT, 2f, 1.4f);
            }
            case TELEPORT -> {
                Vec3 at = c.target != null && c.target.isAlive() ? c.target.position() : c.spots.get(0);
                Vec3 behind = at.subtract(flat(c.target != null ? c.target.getViewVector(1f) : c.aim).normalize().scale(3));
                Fx.elem(sl, null, Fx.E_HALOS, bp.add(0, 1, 0), Vec3.ZERO, 3f, rgb, 0);
                boss.teleportTo(behind.x, at.y, behind.z);
                for (Player p : players(sl, boss.position(), s.r, 3)) hit(boss, p, s);
                Fx.elem(sl, null, s.elem, boss.position(), Vec3.ZERO, (float) s.r * 0.6f, rgb, 0);
                Fx.groundRing(sl, null, boss.position(), (float) s.r, rgb, 18);
                sl.sendParticles(pt, boss.getX(), boss.getY() + 1, boss.getZ(), 40, 1, 1, 1, 0.1);
                boss.playSound(SoundEvents.ENDERMAN_TELEPORT, 2.5f, 0.5f);
            }
        }
    }

    // ------------------------------------------------------------------ vung nguy hiem (moi tick)
    /** Tick mot vung; tra true khi con song. */
    public static boolean tickHazard(ServerLevel sl, HuVuongForm boss, Hazard h) {
        if (--h.left <= 0) return false;
        HvSkill s = h.skill;
        ParticleOptions pt = PhaseMoves.particle(s.pal);
        if (h.left % 3 == 0) {
            double a = (h.left * 0.5) % (Math.PI * 2);
            for (int i = 0; i < 6; i++) {
                double an = a + i * Math.PI / 3, rr = h.r * (0.4 + 0.6 * ((h.left + i * 7) % 10) / 10.0);
                sl.sendParticles(pt, h.c.x + Math.cos(an) * rr, h.c.y + 0.4 + (h.left % 9) * 0.25, h.c.z + Math.sin(an) * rr, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
        boolean pull = s.kind == HvSkill.Kind.TORNADO || s.kind == HvSkill.Kind.WHIRL || s == HvSkill.HU_VO_VUC;
        if (pull && h.left % 2 == 0) for (Player p : players(sl, h.c, h.r, 4)) {
            Vec3 to = flat(h.c.subtract(p.position()));
            if (to.lengthSqr() < 1) continue;
            Vec3 v = to.normalize().scale(s.kind == HvSkill.Kind.WHIRL ? 0.2 : 0.12);
            p.setDeltaMovement(p.getDeltaMovement().add(v.x, s.kind == HvSkill.Kind.TORNADO ? 0.05 : 0, v.z));
            p.hurtMarked = true;
        }
        if (h.left % 10 == 0) for (Player p : players(sl, h.c, h.r, 3)) hit(boss, p, s);
        return true;
    }

    // ------------------------------------------------------------------ tien ich hinh hoc
    static boolean inCone(Vec3 from, Vec3 aim, Vec3 pos, double range, double halfDeg) {
        Vec3 rel = flat(pos.subtract(from));
        double d = rel.length();
        if (d > range) return false;
        if (d < 1.5) return true;
        return rel.normalize().dot(aim) >= Math.cos(Math.toRadians(halfDeg));
    }

    static int countLogs(ServerLevel sl, BlockPos c, int r) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x += 2)
            for (int z = -r; z <= r; z += 2)
                for (int y = -2; y <= 8; y += 2) {
                    p.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    if (sl.hasChunkAt(p) && sl.getBlockState(p).is(BlockTags.LOGS)) n++;
                }
        return n;
    }
}
