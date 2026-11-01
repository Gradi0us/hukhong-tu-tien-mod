package com.hukhong.tran;

import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.gates.DamageRules;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Trận Tu - kỹ năng Q/E/R/T (slot 0..3), gọi từ SkillEffects qua SkillService.cast (phím thật). */
public final class TranSkills {
    private TranSkills() {}

    public static void run(int slot, ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        double sp = StatService.spellPower(p);
        switch (slot) {
            case 0 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.1f);
            case 1 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 1.0f);
            case 2 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.2f);
            case 3 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.9f);
            default -> { }
        }
        switch (slot) {
            case 0 -> khonTran(lv, p, sp);
            case 1 -> hoThanTran(lv, p);
            case 2 -> satTran(lv, p, sp);
            case 3 -> daiTran(lv, p);
            default -> { }
        }
    }

    /** Q Khốn Trận: 0,8 x Phép lực quanh điểm ngắm (r=4), chậm 60% 2 giây, rồi choáng 1,5 giây. Vùng tồn tại 3,5 giây. */
    private static void khonTran(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.Q_RANGE);
        TranRuntime.addZone(lv, p, TranRuntime.Kind.TRAP, c, TranMath.Q_RADIUS, TranMath.Q_TICKS);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.Q_RADIUS);
        TranVfx.rune(lv, c.add(0, 0.05, 0), TranMath.Q_RADIUS);
        float dmg = (float) (TranMath.Q_COEF * sp);
        // Chốt danh sách mục tiêu lúc đặt trận: đòn trúng có thể đẩy quái ra khỏi bán kính, nên không tính lại theo vị trí khi choáng.
        List<LivingEntity> caught = TranRuntime.around(lv, p, c, TranMath.Q_RADIUS);
        for (LivingEntity v : caught) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, dmg);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TranMath.Q_SLOW_TICKS, TranMath.Q_SLOW_AMP, false, true));
        }
        SkillScheduler.later(TranMath.Q_ROOT_DELAY_TICKS, () -> {
            for (LivingEntity v : caught) {
                if (v.isAlive() && !v.isRemoved()) PhapRuntime.stun(v, TranMath.Q_ROOT_SEC);
            }
        });
    }

    /** E Hộ Thân Trận: vùng r=5 quanh mình 8 giây, người chơi trong vùng nhận -30% sát thương. */
    private static void hoThanTran(ServerLevel lv, ServerPlayer p) {
        Vec3 c = p.position();
        TranRuntime.addZone(lv, p, TranRuntime.Kind.WARD, c, TranMath.WARD_RADIUS, TranMath.WARD_TICKS);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.WARD_RADIUS);
        TranVfx.rune(lv, c.add(0, 0.05, 0), TranMath.WARD_RADIUS);
    }

    /** R Sát Trận: 8 cột sáng cách nhau 10 tick, mỗi cột rơi ngẫu nhiên trong r=6 quanh điểm ngắm, đánh 0,55 x Phép lực (r=1,5). */
    private static void satTran(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = TranRuntime.aim(lv, p, TranMath.R_RANGE);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.R_RADIUS);
        TranVfx.rune(lv, c.add(0, 0.05, 0), TranMath.R_RADIUS);
        float dmg = (float) (TranMath.R_TICK_COEF * sp);
        for (int i = 0; i < TranMath.R_HITS; i++) {
            SkillScheduler.later(TranMath.R_INTERVAL_TICKS * (i + 1), () -> pillarStrike(lv, p, c, dmg));
        }
    }

    private static void pillarStrike(ServerLevel lv, ServerPlayer p, Vec3 c, float dmg) {
        if (!p.isAlive() || p.hasDisconnected()) return;
        double a = lv.random.nextDouble() * Math.PI * 2;
        double rr = TranMath.R_RADIUS * Math.sqrt(lv.random.nextDouble());
        Vec3 at = TranRuntime.ground(lv, c.add(Math.cos(a) * rr, 0, Math.sin(a) * rr));
        TranVfx.pillar(lv, at);
        List<LivingEntity> hit = lv.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(TranMath.R_PILLAR_RADIUS, 2, TranMath.R_PILLAR_RADIUS),
                e -> PhapRuntime.valid(p, e) && e.position().distanceTo(at) <= TranMath.R_PILLAR_RADIUS + e.getBbWidth() / 2);
        for (LivingEntity v : hit) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, dmg);
        }
    }

    /** T Đại Trận: vùng r=12 quanh mình 6 giây, kéo quái về tâm; hết giờ nổ 3,0 x Phép lực. Đứng trong vùng: hồi chiêu x0,5. */
    private static void daiTran(ServerLevel lv, ServerPlayer p) {
        Vec3 c = p.position();
        TranRuntime.addZone(lv, p, TranRuntime.Kind.ULT, c, TranMath.ULT_RADIUS, TranMath.ULT_TICKS);
        TranVfx.burst(lv, c.add(0, 0.1, 0), TranMath.ULT_RADIUS);
        TranVfx.rune(lv, c.add(0, 0.05, 0), TranMath.ULT_RADIUS);
        TranVfx.pillar(lv, c);
    }
}
