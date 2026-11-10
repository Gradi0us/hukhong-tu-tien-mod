package com.hukhong.boss.ai;

import com.hukhong.boss.ai.BossProfile.Branch;
import com.hukhong.boss.ai.BossProfile.Combo;
import com.hukhong.boss.ai.BossProfile.Evade;
import com.hukhong.boss.ai.BossProfile.Hit;
import com.hukhong.boss.ai.BossProfile.PhaseVals;
import com.hukhong.boss.phase.PhasedBoss;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * "Boss Combat Brain" (PLAN-v1 §1): máy trạng thái OBSERVE -> STRAFE -> (GAP_CLOSE) -> BASIC_COMBO / EVADE / REACT.
 * Bật cho boss khi {@link PhasedBoss#brainProfile()} khác null; được {@code BossPhaseDirector.tick} gọi mỗi tick phía server.
 * Nhường quyền (yield) khi director đang cutscene / chuyển pha / chiêu phase, hoặc boss đang thi triển chiêu riêng.
 */
public final class BossBrain {
    public enum State { OBSERVE, STRAFE, GAP_CLOSE, BASIC_COMBO, EVADE, REACT }
    public enum Threat { MELEE, PROJECTILE, SKILL }

    private static final Map<Mob, BossBrain> BRAINS = Collections.synchronizedMap(new WeakHashMap<>());

    /** Brain của boss (null nếu boss không bật brain hoặc chưa tick lần nào). */
    public static BossBrain get(Entity e) { return e instanceof Mob m ? BRAINS.get(m) : null; }

    /** Brain đang giữa chuỗi đòn / né / áp sát => director không chen chiêu phase. */
    public static boolean busy(Mob m) { BossBrain b = BRAINS.get(m); return b != null && b.busy(); }

    static List<BossBrain> all() { synchronized (BRAINS) { return new ArrayList<>(BRAINS.values()); } }

    /** Điểm vào từ director. */
    public static void tickFor(ServerLevel sl, Mob boss, PhasedBoss pb) {
        BossBrain b = BRAINS.get(boss);
        if (b == null) {
            BossProfile p = pb.brainProfile();
            if (p == null) return;
            BRAINS.put(boss, b = new BossBrain(boss, pb, p));
        }
        b.tick(sl);
    }

    // ------------------------------------------------------------------ trạng thái
    private final Mob mob;
    private final PhasedBoss pb;
    private final BossProfile prof;
    private final BossMovement mv;
    private State state = State.OBSERVE;
    private int stateT, rest = 20, strafeDir = 1, flipIn = 60, evadeCd, hitStop, combosDone, lastThreatTick = -1;
    private final List<String> history = new ArrayList<>();
    // chuỗi đòn
    private Combo combo;
    private int hitIdx, stage, stageT;       // stage 0 lấy đà, 1 vung, 2 thu đòn
    private double refDist, refAngle;
    private final Set<UUID> hitSet = new HashSet<>();
    // né / áp sát / phản ứng
    private Evade evadeKind;
    private Vec3 evadeDir = Vec3.ZERO;
    private GapClose gap;
    private String pending;
    private final ArrayDeque<Integer> hurtTicks = new ArrayDeque<>();
    private final Map<UUID, Boolean> swinging = new HashMap<>();
    private int lastProj = -1;

    // ---- số liệu cho gametest / debug
    public boolean allowAttacks = true, allowEvade = true;
    public int evadeRolls, evades, strafeFlips, combosCompleted, branchesTaken, hitStops, reacts;
    public String lastBranch;

    BossBrain(Mob mob, PhasedBoss pb, BossProfile prof) {
        this.mob = mob; this.pb = pb; this.prof = prof;
        this.mv = new BossMovement(mob, prof);
    }

    public State state() { return state; }
    public BossProfile profile() { return prof; }
    public BossMovement movement() { return mv; }
    /** 0 lấy đà, 1 vung (khóa hướng), 2 thu đòn; -1 nếu không trong chuỗi. */
    public int hitStage() { return state == State.BASIC_COMBO ? stage : -1; }
    public String comboId() { return combo == null ? null : combo.id(); }
    public boolean busy() { return state == State.BASIC_COMBO || state == State.EVADE || state == State.GAP_CLOSE || state == State.REACT || hitStop > 0; }
    private PhaseVals pv() { return prof.phase(pb.currentPhase()); }

    private boolean yielding() { return pb.director().frozen() || pb.ownMoveActive(); }

    // ------------------------------------------------------------------ tick
    void tick(ServerLevel sl) {
        if (!mob.isAlive()) return;
        if (yielding()) { if (state != State.OBSERVE && state != State.STRAFE) toStrafe(false); mv.freeze(); return; }
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive() || t.isSpectator() || mob.distanceToSqr(t) > 48 * 48) {
            if (state != State.OBSERVE) { set(State.OBSERVE); combo = null; }
            return; // không có mục tiêu: để AI vanilla (đi dạo) chạy
        }
        if (hitStop > 0) { hitStop--; mv.freeze(); mv.apply(sl); return; }
        if (evadeCd > 0) evadeCd--;
        if (rest > 0) rest--;
        scanThreats(sl, t);
        stateT++;
        switch (state) {
            case OBSERVE -> { mv.face(t.position(), pv().turnRate()); mv.stop(); if (stateT > 10) toStrafe(true); }
            case STRAFE -> strafe(sl, t);
            case GAP_CLOSE -> gapClose(sl, t);
            case BASIC_COMBO -> combo(sl, t);
            case EVADE -> evade(sl, t);
            case REACT -> react(sl, t);
        }
        mv.apply(sl);
    }

    private void set(State s) { state = s; stateT = 0; }

    private void toStrafe(boolean anim) {
        set(State.STRAFE);
        combo = null; gap = null; pending = null;
        if (anim) pb.playBrainAnim(strafeDir > 0 ? "strafe_r" : "strafe_l");
    }

    // ------------------------------------------------------------------ STRAFE
    private void strafe(ServerLevel sl, LivingEntity t) {
        PhaseVals v = pv();
        mv.face(t.position(), v.turnRate());
        Vec3 to = flat(t.position().subtract(mob.position()));
        double d = to.length();
        Vec3 n = d < 1e-3 ? mv.forward() : to.scale(1 / d);
        Vec3 tan = new Vec3(-n.z, 0, n.x).scale(strafeDir);
        Vec3 want;
        if (d < v.prefMin()) want = n.scale(-prof.moveSpeed()).add(tan.scale(v.strafeSpeed() * 0.4));       // quá gần: lùi ra
        else if (d > v.prefMax()) want = n.scale(prof.moveSpeed()).add(tan.scale(v.strafeSpeed() * 0.3));   // quá xa: tiến vào
        else want = tan.scale(v.strafeSpeed()).add(n.scale((d - (v.prefMin() + v.prefMax()) / 2) * 0.06));
        mv.drive(want, false);
        if (mv.blocked || --flipIn <= 0) {
            strafeDir = -strafeDir;
            strafeFlips++;
            flipIn = 40 + mob.getRandom().nextInt(61);
            pb.playBrainAnim(strafeDir > 0 ? "strafe_r" : "strafe_l");
        }
        if (!allowAttacks || rest > 0) return;
        if (combosDone >= prof.bigMoveEvery() && pb.brainBigMove(sl, t)) { combosDone = 0; rest = restTicks(); return; }
        Combo c = pickCombo(d);
        if (c != null) startCombo(c, t);
        else if (d >= prof.gapCloseMin() || d > maxRange()) startGap(null);
        else rest = 8;
    }

    private double maxRange() { double m = 0; for (Combo c : prof.combos()) if (!c.branchOnly()) m = Math.max(m, c.rangeMax()); return m; }

    private Combo pickCombo(double d) {
        List<MoveSelector.Option> opts = new ArrayList<>();
        for (Combo c : prof.combos()) if (!c.branchOnly() && pb.currentPhase() <= prof.comboMaxPhase(c.id())) opts.add(new MoveSelector.Option(c.id(), c.rangeMin(), c.rangeMax(), c.weight(), c.minPhase()));
        MoveSelector.Option o = MoveSelector.pick(opts, d, pb.currentPhase(), history, mob.getRandom().nextDouble());
        return o == null ? null : prof.combo(o.id());
    }

    private int restTicks() {
        PhaseVals v = pv();
        int r = v.restMin() + mob.getRandom().nextInt(Math.max(1, v.restMax() - v.restMin() + 1));
        return (int) Math.round(r / Math.max(0.1, prof.difficulty()));
    }

    // ------------------------------------------------------------------ GAP_CLOSE
    private void startGap(String thenCombo) {
        set(State.GAP_CLOSE);
        gap = prof.gapClose();
        pending = thenCombo;
        pb.playBrainAnim(gap.anim());
    }

    private void gapClose(ServerLevel sl, LivingEntity t) {
        if (gap.step(sl, mob, mv, t, stateT - 1) && stateT < 40) return;
        Combo c = pending != null ? prof.combo(pending) : pickCombo(mob.distanceTo(t));
        if (c != null && allowAttacks) startCombo(c, t); else { toStrafe(true); rest = 6; }
    }

    // ------------------------------------------------------------------ BASIC_COMBO
    /** Bắt đầu chuỗi đòn (public cho gametest / boss gọi trực tiếp). */
    public void startCombo(Combo c, LivingEntity t) {
        set(State.BASIC_COMBO);
        combo = c; hitIdx = 0; pending = null;
        MoveSelector.push(history, c.id());
        beginHit(t);
    }

    private void beginHit(LivingEntity t) {
        stage = 0; stageT = 0; hitSet.clear();
        refDist = mob.distanceTo(t);
        refAngle = angleAround(t);
    }

    private double angleAround(LivingEntity t) { return Math.toDegrees(Math.atan2(t.getZ() - mob.getZ(), t.getX() - mob.getX())); }

    private void combo(ServerLevel sl, LivingEntity t) {
        Hit h = combo.hits().get(hitIdx);
        int windup = h.windup() + (h.delayed() ? prof.delayExtra() : 0);
        if (stage == 0) {                          // M3 lấy đà: bám mục tiêu
            if (stageT == 0) pb.playBrainAnim("combo_" + combo.id() + "_" + (hitIdx + 1));
            mv.face(t.position(), pv().turnRate());
            double d = mob.distanceTo(t);
            mv.drive(d > h.reach() * 0.8 ? mv.forward().scale(prof.moveSpeed() * 0.6) : Vec3.ZERO, false);
            if (stageT == (h.delayed() ? 0 : Math.max(0, windup - 6))) glint(sl, h.unblockable());
            if (++stageT >= windup) { stage = 1; stageT = 0; }
        } else if (stage == 1) {                   // M3 vung: khóa hướng, M4 lao theo nhát chém
            if (stageT == 0) pb.onBrainStrike(sl, combo.id(), hitIdx, t);
            mv.drive(mv.forward().scale(h.lunge() / Math.max(1, h.active())), true);
            strike(sl, h);
            if (++stageT >= h.active()) { stage = 2; stageT = 0; }
        } else {                                   // M3 thu đòn: đứng yên (cửa sổ phản công), lùi nhẹ nếu có recoil
            if (h.recoil() > 0) mv.drive(mv.forward().scale(-h.recoil() / Math.max(1, h.recovery())), true); else mv.stop();
            if (++stageT >= h.recovery()) nextHit(t);
        }
    }

    private void nextHit(LivingEntity t) {
        double d = mob.distanceTo(t);
        double moved = Math.abs(Mth.wrapDegrees(angleAround(t) - refAngle));
        for (Branch b : combo.branches()) {
            if (b.afterHit() != hitIdx) continue;
            boolean ok = b.cond() == BossProfile.Cond.RETREAT ? d - refDist > b.threshold() : moved > b.threshold();
            Combo to = ok ? prof.combo(b.toCombo()) : null;
            if (to != null) {
                branchesTaken++;
                lastBranch = combo.id() + "->" + to.id();
                combo = to; hitIdx = 0;
                MoveSelector.push(history, to.id());
                beginHit(t);
                return;
            }
        }
        if (++hitIdx < combo.hits().size()) { beginHit(t); return; }
        combosCompleted++; combosDone++;
        rest = restTicks();
        toStrafe(true);
    }

    private void strike(ServerLevel sl, Hit h) {
        Vec3 fwd = mv.forward(), c = mob.position();
        float dmg = (float) (pb.moveDamage(h.dmgMult()) * prof.difficulty());
        double reach = h.reach() + mob.getBbWidth() * 0.5;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(reach, 1.5, reach))) {
            if (e == mob || !e.isAlive() || e.isSpectator() || e.getClass() == mob.getClass() || hitSet.contains(e.getUUID())) continue;
            if (!(e instanceof Player) && e != mob.getTarget()) continue;
            Vec3 to = flat(e.position().subtract(c));
            double dist = to.length() - e.getBbWidth() * 0.5;
            if (dist > reach) continue;
            if (to.lengthSqr() > 0.25 && Math.toDegrees(Math.acos(Mth.clamp(to.normalize().dot(fwd), -1, 1))) > h.arcDeg() / 2) continue;
            hitSet.add(e.getUUID());
            if (h.unblockable() && e instanceof Player p && p.isBlocking()) p.disableShield();
            if (e.hurt(mob.damageSources().mobAttack(mob), dmg) && h.heavy()) hitStop(sl, e);
        }
        if (stageT == 0) mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, (float) Mth.clamp(1.2 - 0.15 * prof.weight(), 0.5, 1.3));
    }

    /** M6: boss + nạn nhân khựng 2-4 tick, rung camera nhẹ. */
    private void hitStop(ServerLevel sl, LivingEntity victim) {
        int n = Mth.clamp((int) Math.round(1.5 + prof.weight()), 2, 4);
        hitStop = n; hitStops++;
        mv.freeze();
        victim.setDeltaMovement(Vec3.ZERO);
        victim.hurtMarked = true;
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, n, 9, false, false, false));
        Fx.shake(sl, victim.position(), (float) (0.6 + 0.4 * prof.weight()), 6, 10f);
    }

    /** M9: ánh lóe trên tay/vũ khí — trắng: đòn thường, đỏ: không thể đỡ. */
    private void glint(ServerLevel sl, boolean red) {
        Vec3 f = mv.forward(), r = new Vec3(-f.z, 0, f.x);
        Vec3 p = mob.position().add(f.scale(mob.getBbWidth() * 0.5 + 0.3)).add(r.scale(0.45)).add(0, mob.getBbHeight() * 0.72, 0);
        if (red) {
            sl.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.08f, 0.08f), 1.6f), p.x, p.y, p.z, 10, 0.12, 0.12, 0.12, 0);
            sl.playSound(null, p.x, p.y, p.z, SoundEvents.BELL_BLOCK, mob.getSoundSource(), 0.8f, 1.6f);
        } else {
            sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 4, 0.05, 0.05, 0.05, 0.02);
            sl.playSound(null, p.x, p.y, p.z, SoundEvents.AMETHYST_BLOCK_CHIME, mob.getSoundSource(), 1f, 1.8f);
        }
        sl.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }

    // ------------------------------------------------------------------ EVADE
    /** Báo mối đe dọa (vung vũ khí / đạn / phép). Trả true nếu boss né. */
    public boolean onThreat(Threat k) {
        if (!allowEvade || mob.tickCount == lastThreatTick || yielding() || mob.getTarget() == null) return false;
        boolean can = state == State.STRAFE || state == State.OBSERVE || (state == State.BASIC_COMBO && stage == 2);
        if (!can || evadeCd > 0 || pv().evadeChance() <= 0) return false;
        lastThreatTick = mob.tickCount;
        evadeRolls++;
        if (!MoveSelector.evadeRoll(pv().evadeChance(), evadeCd, mob.getRandom().nextDouble())) { evadeCd = 4; return false; }
        Evade[] own = prof.evadeKinds(pb.currentPhase());
        Evade kind = own != null && own.length > 0 ? (k == Threat.PROJECTILE && java.util.Arrays.asList(own).contains(Evade.SIDESTEP) ? Evade.SIDESTEP : own[mob.getRandom().nextInt(own.length)])
                : k == Threat.PROJECTILE ? Evade.SIDESTEP
                : pb.currentPhase() >= 2 && mob.getRandom().nextFloat() < 0.3f ? Evade.BACKFLIP
                : mob.getRandom().nextBoolean() ? Evade.BACKSTEP : Evade.SIDESTEP;
        startEvade(kind);
        return true;
    }

    private void startEvade(Evade kind) {
        LivingEntity t = mob.getTarget();
        Vec3 away = t == null ? mv.forward().scale(-1) : flat(mob.position().subtract(t.position()));
        away = away.lengthSqr() < 1e-4 ? mv.forward().scale(-1) : away.normalize();
        boolean left = mob.getRandom().nextBoolean();
        evadeKind = kind;
        evadeDir = switch (kind) {
            case BACKSTEP -> away.scale(0.55);
            case BACKFLIP -> away.scale(0.6);
            case SIDESTEP -> new Vec3(-away.z, 0, away.x).scale(left ? 0.6 : -0.6).add(away.scale(0.15));
        };
        if (kind == Evade.BACKFLIP) mob.setDeltaMovement(mob.getDeltaMovement().x, 0.45, mob.getDeltaMovement().z);
        evades++;
        combo = null;
        set(State.EVADE);
        if (t != null && mob.level() instanceof ServerLevel sl) pb.onBrainEvade(sl, kind, t);
        pb.playBrainAnim(switch (kind) { case BACKSTEP -> "backstep"; case BACKFLIP -> "backflip"; case SIDESTEP -> left ? "sidestep_l" : "sidestep_r"; });
        mob.playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8f, 0.7f);
    }

    private void evade(ServerLevel sl, LivingEntity t) {
        int len = evadeKind == Evade.BACKFLIP ? 10 : 6;
        mv.face(t.position(), pv().turnRate());
        mv.drive(stateT <= len - 2 ? evadeDir : Vec3.ZERO, true);
        if (stateT == 1) sl.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.1, mob.getZ(), 4, 0.3, 0.05, 0.3, 0.02);
        if (stateT < len) return;
        evadeCd = pv().evadeCd();
        Combo c = pending == null ? null : prof.combo(pending);
        if (c == null && pending != null && allowAttacks && pb.brainReact(sl, t, pending)) { toStrafe(false); return; }
        if (c != null && allowAttacks) { pending = null; startCombo(c, t); }
        else { toStrafe(true); rest = Math.min(rest, 10); }
    }

    // ------------------------------------------------------------------ REACT (trừng phạt hồi máu / phản đòn)
    /** Người chơi hồi máu/uống thuốc. */
    public void onHeal(Player p) {
        if (!prof.punishHeal() || p != mob.getTarget() || yielding() || state == State.REACT || state == State.BASIC_COMBO && stage == 1) return;
        reacts++;
        combo = null;
        pending = prof.punishCombo();
        set(State.REACT);
    }

    /** Boss bị đánh trúng — đếm cho phản đòn (N nhát trong T tick -> lướt ngang rồi chém). */
    public void onHurt(Entity attacker) {
        if (prof.counterHits() <= 0 || attacker == null || attacker != mob.getTarget() || yielding()) return;
        int now = mob.tickCount;
        hurtTicks.addLast(now);
        while (!hurtTicks.isEmpty() && now - hurtTicks.peekFirst() > prof.counterWindow()) hurtTicks.pollFirst();
        if (hurtTicks.size() >= prof.counterHits() && state != State.REACT && state != State.EVADE && !(state == State.BASIC_COMBO && stage == 1)) {
            hurtTicks.clear();
            reacts++;
            if (prof.counterEvade() == null) { combo = null; pending = prof.counterCombo(); set(State.REACT); return; }
            startEvade(prof.counterEvade());
            pending = prof.counterCombo();
            if (mob.level() instanceof ServerLevel sl) glint(sl, true);
        }
    }

    private void react(ServerLevel sl, LivingEntity t) {
        mv.face(t.position(), pv().turnRate() * 1.5f);
        mv.stop();
        if (stateT == 1) { glint(sl, true); mob.playSound(SoundEvents.RAVAGER_ROAR, 0.8f, 1.5f); }
        if (stateT < 6) return;
        Combo c = pending == null ? null : prof.combo(pending);
        if (c == null && pending != null && allowAttacks && pb.brainReact(sl, t, pending)) { toStrafe(false); return; }
        if (c == null) c = pickCombo(mob.distanceTo(t));
        if (c == null || !allowAttacks) { toStrafe(true); return; }
        if (mob.distanceTo(t) <= c.rangeMax()) { pending = null; startCombo(c, t); }
        else startGap(c.id());
    }

    // ------------------------------------------------------------------ dò mối đe dọa phía server
    private void scanThreats(ServerLevel sl, LivingEntity t) {
        for (Player p : sl.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(5.5), pl -> !pl.isSpectator())) {
            Boolean was = swinging.put(p.getUUID(), p.swinging);
            if (p.swinging && (was == null || !was)) onThreat(Threat.MELEE);
        }
        if (mob.tickCount % 2 != 0) return;
        Vec3 c = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
        for (Projectile pr : sl.getEntitiesOfClass(Projectile.class, mob.getBoundingBox().inflate(8))) {
            if (pr.getOwner() == mob || pr.getId() == lastProj) continue;
            Vec3 v = pr.getDeltaMovement();
            if (v.lengthSqr() < 0.09) continue;
            Vec3 dir = v.normalize(), rel = c.subtract(pr.position());
            double along = rel.dot(dir);
            if (along <= 0) continue;
            if (rel.subtract(dir.scale(along)).length() < 1.2 + mob.getBbWidth() * 0.5) { lastProj = pr.getId(); onThreat(Threat.PROJECTILE); }
        }
    }

    /** Phép/kỹ năng người chơi niệm gần boss (hệ thống kỹ năng gọi để boss có thể né). */
    public static void notifySkillCast(ServerLevel sl, Vec3 at, double radius) {
        for (BossBrain b : all()) if (b.mob.level() == sl && b.mob.isAlive() && b.mob.position().distanceToSqr(at) <= radius * radius) b.onThreat(Threat.SKILL);
    }

    private static Vec3 flat(Vec3 v) { return new Vec3(v.x, 0, v.z); }
    Mob mob() { return mob; }
}
