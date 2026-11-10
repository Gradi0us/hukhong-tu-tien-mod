package com.hukhong.chg;

import com.hukhong.boss.ai.BossBrain;
import com.hukhong.boss.ai.BossMovement;
import com.hukhong.boss.ai.BossProfile;
import com.hukhong.boss.ai.BossProfile.Branch;
import com.hukhong.boss.ai.BossProfile.Combo;
import com.hukhong.boss.ai.BossProfile.Cond;
import com.hukhong.boss.ai.BossProfile.Evade;
import com.hukhong.boss.ai.BossProfile.Hit;
import com.hukhong.boss.ai.BossProfile.PhaseVals;
import com.hukhong.boss.ai.GapClose;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

/**
 * BossBrain cho 4 Chấp Hành Giả (PLAN-v1 §4, bước B4): hồ sơ theo idx + kiểu áp sát riêng + hiệu ứng phụ của đòn thường.
 * Chiêu riêng (ChgOneMoves / ChgMaliketh / Chg3Moves / Chg4Moves) vẫn chạy theo hồi chiêu giữa các chuỗi đòn; brain nhường quyền khi chiêu chạy.
 */
public final class ChgBrain {
    private ChgBrain() {}

    private static Hit h(int w, int a, int r, double reach, double arc, double dmg, double lunge) { return BossProfile.hit(w, a, r, reach, arc, dmg, lunge); }
    private static Hit heavy(int w, int a, int r, double reach, double arc, double dmg, double lunge, boolean delayed) {
        return new Hit(w, a, r, reach, arc, dmg, lunge, 0, delayed, true, false);
    }
    private static Hit delayed(int w, int a, int r, double reach, double arc, double dmg, double lunge) {
        return new Hit(w, a, r, reach, arc, dmg, lunge, 0, true, false, false);
    }

    // ------------------------------------------------------------------ CHG I — Thạch Hống (Godfrey): nặng, chậm, lì, không né
    static final BossProfile CHG1 = BossProfile.builder()
            .phase(1, new PhaseVals(2.5, 4.5, 0.03, 8f, 0, 999, 30, 46))
            .phase(2, new PhaseVals(2.5, 4.5, 0.04, 9f, 0, 999, 24, 38))
            .phase(3, new PhaseVals(2.5, 4.5, 0.05, 10f, 0, 999, 20, 32))
            .weight(3.0).accel(8, 6).moveSpeed(0.2)
            .gapClose(GapClose.LEAP, 9).delayExtra(12)
            .combo(new Combo("riu", List.of(h(14, 4, 8, 3.6, 140, 0.8, 0.6), h(10, 4, 8, 3.6, 140, 0.8, 0.6),
                    heavy(12, 4, 18, 3.8, 60, 1.3, 1.2, true)), 0, 4.2, 3, 1, List.of(), false))
            .combo(new Combo("quet", List.of(h(16, 5, 8, 3.8, 180, 0.9, 0.4), heavy(10, 3, 16, 2.8, 90, 0.9, 1.6, false)),
                    0, 4.0, 2, 1, List.of(), false))
            .combo(new Combo("vo", List.of(new Hit(16, 5, 6, 3.5, 70, 0.6, 2.6, 0, false, false, true), heavy(8, 3, 16, 2.8, 90, 1.1, 0.4, false)),
                    1.5, 5.5, 2.5, 2, List.of(), false))
            .combo(new Combo("dam", List.of(new Hit(10, 3, 16, 3.4, 360, 0.7, 0, 0, false, true, true)), 0, 4, 1, 1, List.of(), true))
            .counter(3, 50, "dam").counterEvade(null)
            .punishHeal("nem_da")
            .bigMoveEvery(1)
            .build();

    // ------------------------------------------------------------------ CHG II — Bạch Từ Âm (Maliketh): pha 1 thú, pha 2 kiếm sĩ
    static final GapClose MAL_GAP = new GapClose() {
        public String anim() { return "leap"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            if (b instanceof ChapHanhGia c && c.phase() >= 2) {   // pha 2: lướt bóng thẳng tới
                if (i == 0) sl.sendParticles(ParticleTypes.LARGE_SMOKE, b.getX(), b.getY() + 1, b.getZ(), 12, 0.4, 0.8, 0.4, 0.02);
                return GapClose.DASH.step(sl, b, mv, t, i);
            }
            return GapClose.LEAP.step(sl, b, mv, t, i);       // pha 1: vồ từ trên không
        }
    };

    static final BossProfile CHG2 = BossProfile.builder()
            .phase(1, new PhaseVals(3, 8, 0.32, 24f, 0.35, 40, 18, 30))
            .phase(2, new PhaseVals(5, 8, 0.2, 18f, 0.3, 45, 20, 34))
            .phase(3, new PhaseVals(5, 8, 0.22, 20f, 0.35, 40, 16, 28))
            .weight(1.2).accel(3, 3).moveSpeed(0.34)
            .gapClose(MAL_GAP, 9).delayExtra(8)
            .evades(1, Evade.BACKFLIP).evades(2, Evade.SIDESTEP)
            // pha 1: cào 4 nhát giật cục (nhịp lệch, nhát cuối trễ)
            .combo(new Combo("cao", List.of(h(6, 2, 3, 2.8, 100, 0.4, 0.8), h(4, 2, 8, 2.8, 100, 0.4, 0.6),
                    h(9, 2, 2, 2.8, 100, 0.45, 0.9), delayed(5, 3, 14, 3.0, 120, 0.6, 1.2)), 0, 3.6, 3, 1, List.of(), false))
            .combo(new Combo("chom", List.of(h(10, 4, 6, 3.0, 80, 0.7, 3.5), h(6, 3, 14, 2.8, 120, 0.6, 0.5)), 3, 7, 2, 1, List.of(), false))
            .comboUntil("cao", 1).comboUntil("chom", 1)
            // pha 2: hắc kiếm 2 nhát + chém xoay; lùi xa sau nhát 1 -> đâm lướt
            .combo(new Combo("hac", List.of(h(12, 4, 6, 3.6, 120, 0.8, 0.8), h(8, 4, 8, 3.6, 120, 0.8, 0.8),
                    heavy(14, 6, 18, 3.8, 360, 1.1, 0.3, true)), 0, 4.5, 3, 2,
                    List.of(new Branch(0, Cond.RETREAT, 2.5, "dam")), false))
            .combo(new Combo("dam", List.of(heavy(10, 4, 14, 4.5, 40, 1.0, 3.0, false)), 4, 8, 1.5, 2, List.of(), false))
            .counter(3, 40, "ha_khong").counterEvade(Evade.BACKFLIP)
            .bigMoveEvery(1)
            .build();

    // ------------------------------------------------------------------ CHG III — Lăng Uyên (Morgott): chậm rãi, xảo quyệt
    static final GapClose LONG_STEP = new GapClose() {   // bước dài 1 nhịp
        public String anim() { return "dash"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            double d = b.distanceTo(t);
            if (i < 3) { mv.face(t.position(), 40f); mv.freeze(); return true; }
            if (i > 11 || d < 3.0) { mv.freeze(); mv.face(t.position(), 40f); return false; }
            mv.drive(mv.forward().scale(Math.min(0.8, d * 0.25)), true);
            if (i == 3) b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f);
            return true;
        }
    };

    static final BossProfile CHG3 = BossProfile.builder()
            .phase(1, new PhaseVals(4, 7, 0.1, 12f, 0.3, 50, 24, 38))
            .phase(2, new PhaseVals(4, 7, 0.12, 14f, 0.35, 45, 20, 32))
            .phase(3, new PhaseVals(4, 7, 0.13, 15f, 0.4, 40, 16, 28))
            .weight(1.5).accel(5, 4).moveSpeed(0.26)
            .gapClose(LONG_STEP, 8).delayExtra(14)
            .evades(1, Evade.BACKSTEP)
            // gậy 3 nhát, trễ 2 lần; triệu kiếm ở nhát giữa
            .combo(new Combo("gay", List.of(delayed(12, 4, 6, 3.4, 110, 0.7, 0.8), h(8, 4, 6, 3.4, 110, 0.7, 0.6),
                    heavy(14, 5, 18, 3.6, 70, 1.2, 1.0, true)), 0, 4.2, 3, 1,
                    List.of(new Branch(0, Cond.RETREAT, 2.5, "dam")), false))
            .combo(new Combo("dam", List.of(h(10, 4, 8, 4.2, 40, 0.8, 1.6), heavy(12, 5, 16, 3.4, 140, 1.0, 0.6, false)), 3, 6, 2, 1, List.of(), false))
            .combo(new Combo("quet", List.of(h(9, 4, 6, 3.0, 160, 0.6, 0.4), heavy(12, 4, 16, 3.0, 60, 1.0, 0.6, false)), 0, 3.2, 1.5, 1, List.of(), false))
            .bigMoveEvery(1)
            .build();

    // ------------------------------------------------------------------ CHG IV — Cơ Vô Tận (Mohg): uy nghi, tầm dài
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.75f, 0.05f, 0.08f), 1.4f);

    static final GapClose THRUST = new GapClose() {    // đâm lao tới
        public String anim() { return "dash"; }
        public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int i) {
            double d = b.distanceTo(t);
            if (i > 16 || d < 5.0) { mv.freeze(); return false; }
            mv.face(t.position(), 20f);
            mv.drive(mv.forward().scale(Math.min(0.75, d * 0.25)), true);
            bloodTrail(sl, b.position(), mv.forward(), 1.2);
            return true;
        }
    };

    static final BossProfile CHG4 = BossProfile.builder()
            .phase(1, new PhaseVals(5, 9, 0.12, 10f, 0, 999, 26, 40))
            .phase(2, new PhaseVals(5, 9, 0.14, 12f, 0.3, 50, 22, 34))
            .phase(3, new PhaseVals(5, 9, 0.15, 13f, 0.35, 45, 18, 30))
            .weight(2.2).accel(6, 5).moveSpeed(0.24)
            .gapClose(THRUST, 11).delayExtra(10)
            .evades(2, Evade.SIDESTEP)
            // đinh ba: đâm – quét – đâm xoay
            .combo(new Combo("dinh_ba", List.of(h(12, 4, 6, 6.0, 30, 0.8, 1.2), h(10, 5, 6, 5.0, 160, 0.8, 0.4),
                    heavy(14, 5, 18, 6.0, 40, 1.3, 1.8, true)), 0, 6.5, 3, 1, List.of(), false))
            .combo(new Combo("dam_xa", List.of(h(12, 4, 8, 6.0, 30, 0.9, 3.0), heavy(10, 4, 16, 6.0, 30, 1.0, 1.5, false)), 5, 9.5, 2, 1, List.of(), false))
            .combo(new Combo("xoay", List.of(heavy(8, 6, 16, 3.4, 360, 0.9, 0, false)), 0, 2.6, 8, 1, List.of(), false))
            .counter(2, 30, "xoay").counterEvade(null)
            .bigMoveEvery(1)
            .build();

    static BossProfile profile(int idx) { return switch (idx) { case 1 -> CHG1; case 2 -> CHG2; case 3 -> CHG3; default -> CHG4; }; }

    /** Animation brain có thật trong chg_N.animation.json (tools/gen_chg_brain_anims.py). */
    static Set<String> anims(int idx) {
        Set<String> s = new java.util.HashSet<>(Set.of("strafe_l", "strafe_r", "dash", "leap", "backstep", "sidestep_l", "sidestep_r"));
        if (idx == 2) s.add("backflip");
        for (Combo c : profile(idx).combos()) for (int i = 1; i <= c.hits().size(); i++) s.add("combo_" + c.id() + "_" + i);
        return s;
    }

    // ------------------------------------------------------------------ hiệu ứng phụ của đòn thường
    static void onStrike(ServerLevel sl, ChapHanhGia d, String combo, int hit, LivingEntity t) {
        Vec3 f = Vec3.directionFromRotation(0, d.getYRot());
        switch (d.idx()) {
            case 1 -> {
                if (combo.equals("dam")) {             // dậm đất đẩy lùi
                    Fx.shake(sl, d.position(), 1.2f, 8, 12f);
                    sl.sendParticles(ParticleTypes.EXPLOSION, d.getX(), d.getY() + 0.2, d.getZ(), 3, 1.2, 0.1, 1.2, 0);
                    for (Player p : sl.getEntitiesOfClass(Player.class, d.getBoundingBox().inflate(4.5, 2, 4.5))) {
                        Vec3 away = p.position().subtract(d.position()).multiply(1, 0, 1);
                        away = away.lengthSqr() < 1e-3 ? f : away.normalize();
                        p.setDeltaMovement(away.x * 1.6, 0.5, away.z * 1.6);
                        p.hurtMarked = true;
                    }
                    d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.6f);
                } else if (combo.equals("riu") && hit == 2) {
                    Vec3 at = d.position().add(f.scale(3));
                    sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.2, at.z, 2, 0.4, 0.1, 0.4, 0);
                    Fx.shake(sl, at, 0.9f, 6, 10f);
                }
            }
            case 2 -> {
                if (d.phase() >= 2) sl.sendParticles(ParticleTypes.SQUID_INK, d.getX() + f.x * 2, d.getY() + 1.2, d.getZ() + f.z * 2, 8, 0.6, 0.3, 0.6, 0.02);
            }
            case 3 -> {
                if (combo.equals("gay") && hit == 1 && t != null) d.queueSword(t.position(), 14); // triệu kiếm giữa chuỗi
            }
            case 4 -> bloodTrail(sl, d.position(), f, combo.equals("xoay") ? 0 : 5);
            default -> { }
        }
    }

    /** CHG III: lùi một bước + ném dao. */
    static void onEvade(ServerLevel sl, ChapHanhGia d, Evade kind, LivingEntity t) {
        if (d.idx() != 3 || t == null) return;
        Vec3 from = d.position().add(0, d.getBbHeight() * 0.8, 0), to = t.position().add(0, 1, 0);
        Fx.swordFly(sl, from, to, 6, 0, 0.8f);
        d.playSound(SoundEvents.TRIDENT_THROW.value(), 1.5f, 1.3f);
        if (d.distanceTo(t) < 14) d.strike(t, d.moveDamage(0.35));
        d.daggersThrown++;
    }

    /** Vệt lửa máu theo đường đi/đường đâm (CHG IV). */
    static void bloodTrail(ServerLevel sl, Vec3 from, Vec3 dir, double len) {
        for (double s = 0; s <= len; s += 0.5) {
            Vec3 p = from.add(dir.scale(s));
            sl.sendParticles(BLOOD, p.x, p.y + 0.1, p.z, 2, 0.15, 0.02, 0.15, 0);
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.1, p.z, 1, 0.1, 0.02, 0.1, 0.01);
        }
    }

    /** Brain đang giữa đòn / né / áp sát. */
    static boolean busy(ChapHanhGia d) { return BossBrain.busy(d); }
}
