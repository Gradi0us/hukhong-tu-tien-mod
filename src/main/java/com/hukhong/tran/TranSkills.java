package com.hukhong.tran;

import com.hukhong.client.anim.CastAnimHook;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.skill.Skill;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.skill.SkillState;
import com.hukhong.stats.StatService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Trận Tu: phím T/Q/E/R qua SkillService.cast -> {@link #cast}; kỹ năng theo thế qua {@link #run}. */
public final class TranSkills {
    private TranSkills() {}

    /** Một phím của Trận Tu: T đổi thế (TranRuntime.onKey), Q/E/R theo thế hiện tại (hoặc chọn thế nếu đang chọn). */
    public static void cast(ServerPlayer p, SkillState st, int slot) {
        if (slot < 0 || slot > 3) return;
        if (TranRuntime.onKey(p, slot)) return;
        long now = p.level().getGameTime();
        TranStance s = TranRuntime.stance(p);
        Skill k = TranStance.skillOf(s, slot);
        if (k == Skill.TRAN_PILLAR) return; // Sát Trận giữ-để-nạp: TranRuntime.chargeStart/chargeRelease
        if (k == null || st.cooldownEnd[slot] > now) return;
        if (!Resources.tryConsume(p, k.cost, 1.0)) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
            return;
        }
        st.cooldownEnd[slot] = now + k.cooldownTicks;
        st.lastCombat = now;
        CastAnimHook.play(p, k, null, slot);
        run(k, p);
        if (s == TranStance.SAT_THUONG) TranRuntime.addKietLuc(p);
        TranRuntime.sendSync(p);
    }

    public static void run(Skill k, ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        double sp = StatService.spellPower(p);
        switch (k) {
            case TRAN_1 -> khonTran(lv, p, sp);
            case TRAN_2 -> satTran(lv, p, sp);
            case TRAN_3 -> hoThanTran(lv, p);
            case TRAN_CAGE -> cage(lv, p);
            case TRAN_SLOW -> snare(lv, p, sp);
            case TRAN_SUPPRESS -> suppress(lv, p);
            case TRAN_BLADES -> blades(lv, p, sp);
            case TRAN_DOOM -> doom(lv, p, sp);
            case TRAN_WARD -> wardPn(lv, p);
            case TRAN_HEAL -> healPn(lv, p);
            case TRAN_SHIELD -> shieldPn(lv, p);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ BASE

    /** Q Khốn Trận: 0,8 x Phép lực quanh điểm ngắm (r=4), chậm 60% 2 giây, rồi choáng 1,5 giây. Vùng tồn tại 3,5 giây. */
    private static void khonTran(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.Q_RANGE);
        TranRuntime.addZone(lv, p, TranRuntime.Kind.TRAP, c, TranMath.Q_RADIUS, TranMath.Q_TICKS);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.Q_RADIUS);
        TranVfx.rune(lv, c.add(0, 0.05, 0), TranMath.Q_RADIUS);
        float dmg = (float) (TranMath.Q_COEF * sp);
        // Chốt danh sách mục tiêu lúc đặt trận: đòn trúng có thể đẩy quái ra khỏi bán kính.
        List<LivingEntity> caught = TranRuntime.around(lv, p, c, TranMath.Q_RADIUS);
        for (LivingEntity v : caught) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, dmg);
            v.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    TranMath.Q_SLOW_TICKS, TranMath.Q_SLOW_AMP, false, true));
        }
        SkillScheduler.later(TranMath.Q_ROOT_DELAY_TICKS, () -> {
            for (LivingEntity v : caught) {
                if (v.isAlive() && !v.isRemoved()) PhapRuntime.stun(v, TranMath.Q_ROOT_SEC);
            }
        });
    }

    /** E Sát Trận: 8 cột sáng trong r=6 quanh điểm ngắm, mỗi cột 0,55 x Phép lực. */
    private static void satTran(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.R_RANGE);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.R_RADIUS);
        TranVfx.rune(lv, c.add(0, 0.05, 0), TranMath.R_RADIUS);
        float dmg = (float) (TranMath.R_TICK_COEF * sp);
        for (int i = 0; i < TranMath.R_HITS; i++) {
            SkillScheduler.later(TranMath.R_INTERVAL_TICKS * (i + 1),
                    () -> pillarStrike(lv, p, c, dmg, TranMath.R_RADIUS, TranMath.R_PILLAR_RADIUS));
        }
    }

    /** R Hộ Thân Trận: vùng r=5 quanh mình 8 giây, người chơi trong vùng nhận -30% sát thương. */
    private static void hoThanTran(ServerLevel lv, ServerPlayer p) {
        Vec3 c = p.position();
        TranRuntime.ward(lv, p, c, TranMath.WARD_RADIUS);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.WARD_RADIUS);
    }

    // ------------------------------------------------------------------ KHỐNG CHẾ

    /** Q Giam Cầm Trận: lồng bán kính 4 tại điểm ngắm, 8 giây; quái bị nhốt không ra khỏi lồng, đủ số đập thì vỡ. */
    private static void cage(ServerLevel lv, ServerPlayer p) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.CAGE_RANGE);
        TranRuntime.cage(lv, p, c);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.CAGE_RADIUS);
        TranVfx.cage(lv, c, TranMath.CAGE_RADIUS, 0f);
    }

    /** E Khốn Linh Trận: vùng r5 6 giây, chậm 60% + 0,15 x Phép lực sát thương mỗi giây. */
    private static void snare(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.SNARE_RANGE);
        TranRuntime.snare(lv, p, c);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.SNARE_RADIUS);
    }

    /** R Trấn Áp Trận: vùng r5 5 giây, quái trong vùng gây -30% sát thương, choáng 1 giây khi vừa bước vào. */
    private static void suppress(ServerLevel lv, ServerPlayer p) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.SUP_RANGE);
        TranRuntime.suppress(lv, p, c);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.SUP_RADIUS);
    }

    // ------------------------------------------------------------------ SÁT THƯƠNG

    /**
     * Sát Trận (Q giữ để nạp) khi thả: cột sáng khổng lồ tại điểm rơi, bán kính và hệ số theo tỉ lệ tụ lực f;
     * mọi địch trong bán kính nhận (hệ số) x Phép lực, rồi người thi triển Kiệt Sức 3 giây.
     */
    static void chargeStrike(ServerLevel lv, ServerPlayer p, Vec3 c, double f) {
        Vec3 at = c; // c đã ở mặt đất (TranRuntime.aimGround): cột giáng đúng tâm pháp trận đang hiện
        double r = TranMath.chargeRadius(f);
        float dmg = (float) (TranMath.chargeCoef(f) * StatService.spellPower(p));
        TranVfx.giantPillar(lv, p, at, r);
        for (LivingEntity v : TranRuntime.around(lv, p, at, r)) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, dmg);
        }
        TranRuntime.forceExhaust(p, TranMath.GIANT_EXHAUST_TICKS);
    }

    /** E Vạn Kiếm Trận: 12 lưỡi kiếm rơi trong 2 giây quanh điểm ngắm (r4), mỗi lưỡi 0,45 x Phép lực. */
    private static void blades(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.BLADE_RANGE);
        TranVfx.bladeRain(lv, c, TranMath.BLADE_RADIUS);
        float dmg = (float) (TranMath.BLADE_COEF * sp);
        for (int i = 0; i < TranMath.BLADE_COUNT; i++) {
            SkillScheduler.later(TranMath.BLADE_INTERVAL_TICKS * (i + 1),
                    () -> pillarStrike(lv, p, c, dmg, TranMath.BLADE_RADIUS, TranMath.BLADE_HIT_R));
        }
    }

    /** R Diệt Thế Trận: tụ lực 1,5 giây rồi nổ r6, 4,0 x Phép lực. */
    private static void doom(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.DOOM_RANGE);
        TranVfx.doomCharge(lv, c, TranMath.DOOM_RADIUS, 0.33f);
        SkillScheduler.later(10, () -> TranVfx.doomCharge(lv, c, TranMath.DOOM_RADIUS, 0.66f));
        SkillScheduler.later(20, () -> TranVfx.doomCharge(lv, c, TranMath.DOOM_RADIUS, 1f));
        float dmg = (float) (TranMath.DOOM_COEF * sp);
        SkillScheduler.later(TranMath.DOOM_CHARGE_TICKS, () -> {
            if (!p.isAlive() || p.hasDisconnected()) return;
            TranVfx.doomBlast(lv, c, TranMath.DOOM_RADIUS);
            for (LivingEntity v : TranRuntime.around(lv, p, c, TranMath.DOOM_RADIUS)) {
                v.invulnerableTime = 0;
                DamageRules.hurtBySkill(p, v, dmg);
            }
        });
    }

    // ------------------------------------------------------------------ PHÒNG NGỰ

    /** Q Hộ Thân Trận: r6 8 giây, người chơi trong vùng -30% sát thương. */
    private static void wardPn(ServerLevel lv, ServerPlayer p) {
        TranRuntime.ward(lv, p, p.position(), TranMath.PN_WARD_RADIUS);
        TranVfx.burst(lv, p.position().add(0, 0.1, 0), TranMath.PN_WARD_RADIUS);
    }

    /** E Hồi Linh Trận: r6 6 giây, mỗi giây người chơi trong vùng hồi 3% máu tối đa + 2 Trận Lực. */
    private static void healPn(ServerLevel lv, ServerPlayer p) {
        TranRuntime.heal(lv, p, p.position());
        TranVfx.burst(lv, p.position().add(0, 0.1, 0), TranMath.HEAL_RADIUS);
    }

    /** R Kim Cang Trận: khiên hấp thụ 20% máu tối đa cho người chơi trong r6, 8 giây. */
    private static void shieldPn(ServerLevel lv, ServerPlayer p) {
        TranRuntime.shield(lv, p, p.position());
        TranVfx.burst(lv, p.position().add(0, 0.1, 0), TranMath.SHIELD_RADIUS);
    }

    // ------------------------------------------------------------------ tiện ích

    /** Một cột sáng rơi ngẫu nhiên trong bán kính spread quanh c, đánh mục tiêu trong hitR của điểm rơi. */
    private static void pillarStrike(ServerLevel lv, ServerPlayer p, Vec3 c, float dmg, double spread, double hitR) {
        if (!p.isAlive() || p.hasDisconnected()) return;
        double a = lv.random.nextDouble() * Math.PI * 2;
        double rr = spread * Math.sqrt(lv.random.nextDouble());
        Vec3 at = TranRuntime.ground(lv, c.add(Math.cos(a) * rr, 0, Math.sin(a) * rr));
        TranVfx.pillar(lv, at);
        List<LivingEntity> hit = lv.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(hitR, 2, hitR),
                e -> PhapRuntime.valid(p, e) && e.position().distanceTo(at) <= hitR + e.getBbWidth() / 2);
        for (LivingEntity v : hit) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, dmg);
        }
    }
}
