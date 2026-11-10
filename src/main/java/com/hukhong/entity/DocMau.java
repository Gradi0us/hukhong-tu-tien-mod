package com.hukhong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Doc Mau: boss the gioi dam lay / rung ram. Bo ky nang: Dam Bao Tu (vung khi doc), Day Leo Quat (keo nguoi choi vao),
 * Do Trung (goi bay Hu Trung); pha 2: Phun Trao Doc (bung doc dien rong + mua nuoc bot).
 */
public class DocMau extends WorldBoss {
    private static final int CLOUD = 0, LASH = 1, BROOD = 2, ERUPT = 3;

    public DocMau(EntityType<? extends Monster> t, Level l) { super(t, l, BossEvent.BossBarColor.GREEN); }

    @Override protected String id() { return "doc_mau"; }
    @Override protected String track() { return "bicanh_boss"; }
    @Override protected int skillCount(int phase) { return phase >= 2 ? 4 : 3; }
    @Override protected int windupOf(int s) { return switch (s) { case CLOUD -> 26; case LASH -> 24; case BROOD -> 30; default -> 40; }; }
    @Override protected String animOf(int s) { return switch (s) { case CLOUD -> "skill_a"; case LASH -> "skill_c"; default -> "skill_b"; }; }
    @Override protected int cooldown(int phase) { return phase >= 2 ? 72 : 100; }
    @Override protected double radiusOf(int s) { return switch (s) { case LASH -> 18; case ERUPT -> 11; default -> 4; }; }

    // ---------------- bo chieu rieng pha 2/3 (DocMauMoves) - director giao qua startOwnMove
    String mv;
    int mt;
    LivingEntity mTarget;
    Vec3 aimDir = new Vec3(0, 0, 1), from, ultCenter;
    final java.util.Set<java.util.UUID> hitOnce = new java.util.HashSet<>();
    final java.util.List<Vec3> spots = new java.util.ArrayList<>(), safe = new java.util.ArrayList<>();
    final java.util.List<java.util.UUID> brood = new java.util.ArrayList<>(), offered = new java.util.ArrayList<>();
    final java.util.List<DocMauMoves.Pool> pools = new java.util.ArrayList<>();
    final java.util.List<DocMauMoves.Moth> moths = new java.util.ArrayList<>();

    public String currentMove() { return mv; }
    public int moveTick() { return mt; }
    public int broodCount() { return brood.size(); }
    public int mothCount() { return moths.size(); }

    public boolean startMove(ServerLevel sl, String id, LivingEntity target) {
        if (!DocMauMoves.handles(id) || mv != null || !isAlive()) return false;
        mv = id; mt = 0; mTarget = target; from = null; ultCenter = null;
        hitOnce.clear(); spots.clear();
        pending = -1; windup = 0;
        getNavigation().stop();
        DocMauMoves.begin(sl, this);
        return true;
    }

    void endMove() {
        if (mv == null) return;
        mv = null; mTarget = null;
        setNoGravity(false);
    }

    @Override public boolean startOwnMove(ServerLevel sl, com.hukhong.boss.phase.BossPhaseConfig.Move m, LivingEntity target) {
        return DocMauMoves.handles(m.id()) && startMove(sl, m.id(), target);
    }
    @Override public boolean idleForMove() { return super.idleForMove() && mv == null; }
    @Override public void cancelSkill() { super.cancelSkill(); endMove(); }
    @Override public java.util.List<com.hukhong.boss.phase.CamPath.Key> introCamera(int T) { return DocMauMoves.introCam(T); }
    @Override public java.util.List<com.hukhong.boss.phase.BossPhaseConfig.SubLine> introLines(int T) { return DocMauMoves.introSubs(T); }
    @Override public void applyPhase(ServerLevel sl, int ph) {
        super.applyPhase(sl, ph);
        if (director() != null && director().transitioning()) DocMauMoves.transformFx(sl, this, ph);
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        if (mv != null) {
            skillCd = Math.max(skillCd, 20);
            if (director().transitioning()) endMove();
            else if (!DocMauMoves.step(sl, this, mt++)) { endMove(); skillCd = 50; }
        }
        if (mv == null && !director().frozen()) brainTick(sl);
        if (!pools.isEmpty()) DocMauMoves.tickPools(sl, this);
        if (!moths.isEmpty()) DocMauMoves.tickMoths(sl, this);
    }

    @Override public void die(net.minecraft.world.damagesource.DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            endMove(); pools.clear(); moths.clear();
            DocMauMoves.despawnBrood(sl, this);
            DocMauMoves.deathScene(sl, this);
        }
    }

    @Override public void remove(RemovalReason r) {
        if (level() instanceof ServerLevel sl) DocMauMoves.despawnBrood(sl, this);
        super.remove(r);
    }

    @Override protected void fire(ServerLevel sl, int skill, LivingEntity t) {
        switch (skill) {
            case CLOUD -> {
                Vec3 at = t != null ? t.position() : pendingAt;
                AreaEffectCloud c = new AreaEffectCloud(sl, at.x, at.y, at.z);
                c.setOwner(this);
                c.setRadius(4.0f);
                c.setDuration(160);
                c.setRadiusPerTick(-0.015f);
                c.setParticle(ParticleTypes.ITEM_SLIME);
                c.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                c.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                sl.addFreshEntity(c);
                playSound(SoundEvents.SLIME_SQUISH, 2f, 0.5f);
            }
            case LASH -> {
                hitPlayers(sl, position(), 18, 1.0, p -> {
                    Vec3 in = position().subtract(p.position()).multiply(1, 0, 1).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(in.x * 1.3, 0.3, in.z * 1.3));
                    p.hurtMarked = true;
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    Vec3 a = position().add(0, getBbHeight() * 0.6, 0), b = p.position().add(0, 1, 0);
                    for (int i = 0; i <= 14; i++) {
                        Vec3 q = a.lerp(b, i / 14.0);
                        sl.sendParticles(ParticleTypes.COMPOSTER, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                });
            }
            case BROOD -> {
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    DocMauMoves.spawnBrood(sl, this, position().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5), t);
                }
                playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.6f);
            }
            default -> {
                hitPlayers(sl, position(), 11, 1.4, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 2)));
                ringFx(sl, position(), 11, ParticleTypes.ITEM_SLIME);
                for (int i = 0; i < 4; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    PoisonSpit.launchDir(this, position().add(0, getBbHeight() * 0.8, 0), new Vec3(Math.cos(a), 0.5, Math.sin(a)), skillDmg(0.8), 2.5, 1.0f);
                }
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.9f);
            }
        }
    }

    // ================================================================ BossBrain (PLAN-v1 §4: đứng sau bầy con)
    /** Giữ 8-12 ô, nép sau bầy con; bị áp sát -> phun độc rồi lùi; bầy con chết hết -> hung hăng cận chiến (cào 2 nhát + phun gần). */
    public static final com.hukhong.boss.ai.BossProfile BRAIN = com.hukhong.boss.ai.BossProfile.builder()
            .phase(1, new com.hukhong.boss.ai.BossProfile.PhaseVals(8, 12, 0.12, 10f, 0.3, 40, 40, 70))
            .phase(2, new com.hukhong.boss.ai.BossProfile.PhaseVals(8, 12, 0.14, 12f, 0.35, 35, 35, 60))
            .weight(1.5).accel(5, 4).moveSpeed(0.24)
            .gapClose(com.hukhong.boss.ai.GapClose.DASH, 6)
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("cao", java.util.List.of(
                    com.hukhong.boss.ai.BossProfile.hit(9, 3, 6, 3, 120, 0.8, 0.6),
                    com.hukhong.boss.ai.BossProfile.hit(9, 3, 6, 3, 120, 0.8, 0.6),
                    new com.hukhong.boss.ai.BossProfile.Hit(12, 4, 14, 4, 70, 1.0, 0, 0.6, false, true, false)),
                    0, 4.5, 3, 1, java.util.List.of(), false))
            .bigMoveEvery(99)
            .build();
    /** Hết bầy con: áp sát, đi vòng gần, không né. */
    static final com.hukhong.boss.ai.BossProfile.PhaseVals AGGRO = new com.hukhong.boss.ai.BossProfile.PhaseVals(1.5, 3, 0.2, 16f, 0, 999, 14, 26);

    boolean hadBrood, aggro;
    int seenEvades;
    public int retreatSpits;
    public boolean aggressive() { return aggro; }

    @Override public com.hukhong.boss.ai.BossProfile brainProfile() { return BRAIN; }
    @Override public com.hukhong.boss.ai.BossProfile.PhaseVals brainVals(com.hukhong.boss.ai.BossProfile p) { return aggro ? AGGRO : p.phase(currentPhase()); }
    @Override protected java.util.Set<String> brainAnims() {
        return java.util.Set.of("strafe_l", "strafe_r", "backstep", "sidestep_l", "sidestep_r", "dash", "combo_cao_1", "combo_cao_2", "combo_cao_3");
    }

    private void brainTick(ServerLevel sl) {
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(this);
        LivingEntity t = getTarget();
        if (b == null || t == null || !t.isAlive()) return;
        java.util.List<HuTrung> live = DocMauMoves.liveBrood(sl, this);
        if (!live.isEmpty()) hadBrood = true;
        boolean was = aggro;
        aggro = hadBrood && live.isEmpty();
        if (aggro && !was) { playSound(SoundEvents.RAVAGER_ROAR, 2f, 1.3f); triggerAnim("skill", "phase"); }
        double d = distanceTo(t);
        b.allowAttacks = aggro || d < 5; // còn bầy: chỉ cào khi bị áp sát, không lao vào
        if (b.evades > seenEvades) { // né = phun độc rồi lùi
            seenEvades = b.evades;
            Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1);
            if (dir.lengthSqr() > 1e-3) {
                dir = dir.normalize();
                PoisonSpit.launchDir(this, position().add(0, getBbHeight() * 0.8, 0), new Vec3(dir.x, 0.1, dir.z), skillDmg(0.6), 2.0, 1.1f);
                playBrainAnim("backstep");
                retreatSpits++;
            }
        }
        if (!aggro && !live.isEmpty() && b.state() == com.hukhong.boss.ai.BossBrain.State.STRAFE) { // nép sau bầy
            Vec3 c = Vec3.ZERO;
            for (HuTrung h : live) c = c.add(h.position());
            c = c.scale(1.0 / live.size());
            Vec3 back = c.subtract(t.position()).multiply(1, 0, 1);
            if (back.lengthSqr() > 1e-3) {
                Vec3 spot = c.add(back.normalize().scale(3)), go = spot.subtract(position()).multiply(1, 0, 1);
                if (go.lengthSqr() > 1) setDeltaMovement(getDeltaMovement().add(go.normalize().scale(0.05)));
            }
        }
    }
}
