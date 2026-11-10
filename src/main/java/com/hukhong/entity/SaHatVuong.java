package com.hukhong.entity;

import com.hukhong.boss.phase.BossPhaseConfig;
import com.hukhong.boss.phase.CamPath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sa Hat Vuong: yeu vuong sa mac, bo chieu RIENG kieu Starscourge Radahn (song dai kiem, trong luc, mua ten cat,
 * thien thach giang). Moi chieu o {@link SaHatMoves}; he WorldBoss.fire cu bi tat (skillCd giu cao).
 */
public class SaHatVuong extends WorldBoss {
    // ---- trang thai chieu rieng (SaHatMoves doc/ghi)
    String mv;
    int mt, ownCd = 60, ultCd = 200;
    LivingEntity mTarget;
    Vec3 aim = new Vec3(0, 0, 1), from = Vec3.ZERO, to = Vec3.ZERO, spot = Vec3.ZERO;
    final List<Vec3> spots = new ArrayList<>();
    final Set<UUID> hitOnce = new HashSet<>();
    final List<SaHatMoves.Orb> orbs = new ArrayList<>();
    final List<SaHatMoves.Sand> sands = new ArrayList<>();

    public SaHatVuong(EntityType<? extends Monster> t, Level l) { super(t, l, BossEvent.BossBarColor.YELLOW); }

    @Override protected String id() { return "sa_hat_vuong"; }
    @Override protected String track() { return "chg"; }
    // he skill WorldBoss cu khong dung nua (giu de thoa abstract)
    @Override protected int skillCount(int phase) { return 0; }
    @Override protected int windupOf(int s) { return 20; }
    @Override protected String animOf(int s) { return "skill_a"; }
    @Override protected int cooldown(int phase) { return 100; }
    @Override protected double radiusOf(int s) { return 3; }
    @Override protected void fire(ServerLevel sl, int skill, LivingEntity t) { }

    // ------------------------------------------------------------ API chieu rieng
    public String currentMove() { return mv; }
    public int moveTick() { return mt; }
    /** Don chieu rieng trung: sat thuong + Hu hoa. */
    public boolean strike(LivingEntity e, float dmg) {
        boolean ok = e.hurt(damageSources().mobAttack(this), dmg);
        if (ok) HuSkill.hu(e, 100);
        return ok;
    }
    public int orbCount() { return orbs.size(); }
    public int sandCount() { return sands.size(); }

    public boolean startMove(ServerLevel sl, String id, LivingEntity target) {
        if (!SaHatMoves.handles(id) || mv != null || !isAlive()) return false;
        mv = id; mt = 0; mTarget = target;
        hitOnce.clear(); spots.clear();
        SaHatMoves.begin(sl, this);
        return true;
    }

    void endMove() {
        if (mv != null) setNoGravity(false);
        if (isInvisible()) setInvisible(false);
        mv = null; mt = 0;
        ownCd = (int) Math.round(com.hukhong.events.BossBuff.cd(phase() >= 3 ? 40 : 60));
    }

    @Override public boolean idleForMove() { return super.idleForMove() && mv == null; }
    @Override public void cancelSkill() { super.cancelSkill(); endMove(); }
    @Override public boolean startOwnMove(ServerLevel sl, BossPhaseConfig.Move m, LivingEntity target) {
        if (!SaHatMoves.handles(m.id())) return false;
        String id = m.id();
        if (id.equals(SaHatMoves.ULT) && ultCd > 0) id = "sh_trong_luc_cau"; // toi thuong dang hoi: thay bang cau trong luc
        return startMove(sl, id, target);
    }
    @Override public List<CamPath.Key> introCamera(int T) { return SaHatMoves.introCam(T); }
    @Override public List<BossPhaseConfig.SubLine> introLines(int T) { return SaHatMoves.introSubs(T); }
    @Override public void applyPhase(ServerLevel sl, int ph) { super.applyPhase(sl, ph); SaHatMoves.transformFx(sl, this, ph); }
    @Override public boolean isImmobile() { return super.isImmobile() || mv != null; }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        if (ultCd > 0) ultCd--;
        if (mv != null && !director().frozen()) {
            getNavigation().stop();
            if (!SaHatMoves.step(sl, this, mt++)) endMove();
        }
        if (!orbs.isEmpty()) SaHatMoves.tickOrbs(sl, this);
        if (!sands.isEmpty()) SaHatMoves.tickSands(sl, this);
    }

    @Override protected void customServerAiStep() {
        skillCd = 1000; // tat he skill cu
        super.customServerAiStep();
        if (mv != null || director().frozen() || director().transitioning()) return;
        if (ownCd > 0) { ownCd--; return; }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive() || distanceToSqr(t) > 30 * 30) return;
        startMove((ServerLevel) level(), SaHatMoves.pick(random, phase(), ultCd <= 0, distanceTo(t)), t);
    }

    @Override public void die(DamageSource src) {
        super.die(src);
        orbs.clear(); sands.clear();
        if (level() instanceof ServerLevel sl) SaHatMoves.deathScene(sl, this);
    }
}
