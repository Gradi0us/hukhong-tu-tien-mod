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
        if (mv == null && !director().frozen()) brainTick(sl);
        if (!orbs.isEmpty()) SaHatMoves.tickOrbs(sl, this);
        if (!sands.isEmpty()) SaHatMoves.tickSands(sl, this);
    }

    @Override protected void customServerAiStep() {
        skillCd = 1000; // tat he skill cu
        super.customServerAiStep();
        if (mv != null || director().frozen() || director().transitioning()) return;
        if (ownCd > 0) { ownCd--; return; }
        if (com.hukhong.boss.ai.BossBrain.busy(this)) return; // không chen giữa song kiếm / lao trọng lực của brain
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive() || distanceToSqr(t) > 30 * 30) return;
        if (hiddenT > 40 && ultCd >= 0) { hiddenT = 0; startMove((ServerLevel) level(), "sh_trong_luc_cau", t); return; } // trốn sau vật cản -> cầu trọng lực vòng qua
        startMove((ServerLevel) level(), SaHatMoves.pick(random, phase(), ultCd <= 0, distanceTo(t)), t);
    }

    @Override public void die(DamageSource src) {
        super.die(src);
        orbs.clear(); sands.clear();
        if (level() instanceof ServerLevel sl) SaHatMoves.deathScene(sl, this);
    }

    // ================================================================ BossBrain (PLAN-v1 §4: lùi xa bắn, rồi bất ngờ lao)
    /**
     * Giữ 10-16 ô (chiêu tầm xa SaHatMoves chạy theo ownCd); né = lùi trượt trên cát để lại vệt bụi; áp sát = LAO TRỌNG LỰC
     * ~15 ô rồi song kiếm 4 nhát (2 trái 2 phải) hoặc đập chéo. Người chơi trốn sau vật cản > 2 giây -> cầu trọng lực.
     */
    public static final com.hukhong.boss.ai.BossProfile BRAIN = com.hukhong.boss.ai.BossProfile.builder()
            .phase(1, new com.hukhong.boss.ai.BossProfile.PhaseVals(10, 16, 0.16, 10f, 0.3, 40, 60, 90))
            .phase(2, new com.hukhong.boss.ai.BossProfile.PhaseVals(10, 16, 0.18, 12f, 0.35, 35, 45, 70))
            .phase(3, new com.hukhong.boss.ai.BossProfile.PhaseVals(9, 15, 0.2, 14f, 0.4, 30, 35, 55))
            .weight(2.5).accel(6, 4).moveSpeed(0.3)
            .gapClose(new GravityDash(), 9)
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("song_kiem", java.util.List.of(
                    com.hukhong.boss.ai.BossProfile.hit(12, 3, 5, 4, 130, 0.7, 0.8),
                    com.hukhong.boss.ai.BossProfile.hit(9, 3, 5, 4, 130, 0.7, 0.8),
                    com.hukhong.boss.ai.BossProfile.hit(9, 3, 5, 4, 130, 0.7, 0.8),
                    new com.hukhong.boss.ai.BossProfile.Hit(9, 3, 12, 4.2, 140, 0.9, 1.0, 0.4, false, true, false)),
                    0, 5, 3, 1, java.util.List.of(new com.hukhong.boss.ai.BossProfile.Branch(1, com.hukhong.boss.ai.BossProfile.Cond.RETREAT, 3, "dap_cheo")), false))
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("dap_cheo", java.util.List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(16, 4, 18, 4.5, 90, 1.4, 1.5, 0.4, false, true, true)),
                    0, 5, 1.5, 1, java.util.List.of(), false))
            .bigMoveEvery(99)
            .build();

    @Override public com.hukhong.boss.ai.BossProfile brainProfile() { return BRAIN; }
    @Override protected java.util.Set<String> brainAnims() {
        return java.util.Set.of("strafe_l", "strafe_r", "backstep", "sidestep_l", "sidestep_r", "backflip", "dash",
                "combo_song_kiem_1", "combo_song_kiem_2", "combo_song_kiem_3", "combo_song_kiem_4", "combo_dap_cheo_1");
    }

    int hiddenT;
    public int gravityDashes;

    /** Vệt bụi khi lùi trượt + đếm thời gian người chơi khuất tầm nhìn. */
    private void brainTick(ServerLevel sl) {
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(this);
        LivingEntity t = getTarget();
        if (b == null || t == null || !t.isAlive()) { hiddenT = 0; return; }
        hiddenT = hasLineOfSight(t) ? 0 : hiddenT + 1;
        Vec3 v = b.movement().velocity(), away = position().subtract(t.position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 1e-3 && v.dot(away.normalize()) > 0.08 && onGround())
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.FALLING_DUST,
                    net.minecraft.world.level.block.Blocks.SAND.defaultBlockState()), getX(), getY() + 0.1, getZ(), 4, getBbWidth() * 0.4, 0.05, getBbWidth() * 0.4, 0);
    }

    /** Lao trọng lực: 8 tick gồng (vòng tím) rồi phóng ~15 ô thẳng tới mục tiêu, hơi bám hướng. */
    static final class GravityDash implements com.hukhong.boss.ai.GapClose {
        public String anim() { return "dash"; }
        public boolean step(ServerLevel sl, net.minecraft.world.entity.Mob b, com.hukhong.boss.ai.BossMovement mv, LivingEntity t, int i) {
            if (i < 8) {
                mv.face(t.position(), 20f);
                mv.freeze();
                if (i == 0) { ((SaHatVuong) b).gravityDashes++; b.playSound(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 1.4f); }
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, b.getX(), b.getY() + 1, b.getZ(), 6, 0.8, 0.6, 0.8, 0.05);
                return true;
            }
            double d = b.distanceTo(t);
            if (d < 3 || i > 8 + 15) { mv.freeze(); return false; }
            mv.face(t.position(), 6f);
            mv.drive(mv.forward().scale(Math.min(1.1, d * 0.4)), true);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, b.getX(), b.getY() + 0.5, b.getZ(), 4, 0.4, 0.3, 0.4, 0.1);
            return true;
        }
    }
}
