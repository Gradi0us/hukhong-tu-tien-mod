package com.hukhong.dan;

import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Đan Tu - kỹ năng Q/E/R/T (slot 0..3), gọi từ SkillEffects qua SkillService.cast (phím thật). */
public final class DanSkills {
    private DanSkills() {}

    public static void run(int slot, ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        double sp = StatService.spellPower(p);
        switch (slot) {
            case 0 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.9f, 1.2f);
            case 1 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.9f, 1.1f);
            case 2 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9f, 0.9f);
            case 3 -> lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
            default -> { }
        }
        switch (slot) {
            case 0 -> doc(lv, p, sp);
            case 1 -> hoiXuan(lv, p, sp);
            case 2 -> phanThien(lv, p, sp);
            case 3 -> kimDan(lv, p);
            default -> { }
        }
    }

    /** Q Độc Đan Vũ: 5 viên rơi ngẫu nhiên quanh điểm ngắm (r=3), mỗi viên 0,4 x Phép lực + 2 tầng độc; xong để lại hồ độc r=2 trong 4 giây (+1 tầng/giây). */
    private static void doc(ServerLevel lv, ServerPlayer p, double sp) {
        Vec3 c = DanRuntime.aim(lv, p, DanMath.Q_RANGE);
        DanVfx.aimRing(lv, p, c.add(0, 0.05, 0), DanMath.Q_SCATTER); // vòng ngắm: viên đan sẽ rơi trong vòng này
        for (int i = 0; i < DanMath.Q_PILLS; i++) {
            double a = lv.random.nextDouble() * Math.PI * 2;
            double rr = DanMath.Q_SCATTER * Math.sqrt(lv.random.nextDouble());
            Vec3 at = DanRuntime.ground(lv, c.add(Math.cos(a) * rr, 0, Math.sin(a) * rr));
            SkillScheduler.later(DanMath.Q_PILL_GAP_TICKS * (i + 1), () -> {
                if (p.isAlive() && !p.hasDisconnected()) {
                    DanVfx.throwArc(lv, at.add(0, 4, 0), at); // viên đan rơi từ trên cao xuống
                    DanRuntime.burst(lv, p, at, DanMath.Q_PILL_RADIUS, DanMath.Q_COEF * sp, DanMath.Q_STACKS);
                }
            });
        }
        SkillScheduler.later(DanMath.Q_PILL_GAP_TICKS * DanMath.Q_PILLS, () -> {
            if (p.isAlive() && !p.hasDisconnected()) DanRuntime.addPool(lv, p, c);
        });
    }

    /** E Hồi Xuân Đan: hồi 0,8 x Phép lực + 8% Sinh lực tối đa cho mình và người chơi trong 6 block; xóa hiệu ứng xấu. Ma Tu chỉ hồi cho bản thân. */
    private static void hoiXuan(ServerLevel lv, ServerPlayer p, double sp) {
        boolean ma = p.getData(ModAttachments.CULTIVATION).paths().contains(Path.MA); // Ma Tu không hỗ trợ người khác
        List<ServerPlayer> who = new ArrayList<>();
        who.add(p);
        if (!ma) {
            for (ServerPlayer o : lv.players()) {
                if (o != p && o.isAlive() && !o.isSpectator() && o.distanceToSqr(p) <= DanMath.HEAL_RADIUS * DanMath.HEAL_RADIUS) who.add(o);
            }
        }
        for (ServerPlayer o : who) {
            o.heal((float) (DanMath.HEAL_COEF * sp + DanMath.HEAL_MAX_PCT * o.getMaxHealth()));
            for (MobEffectInstance e : new ArrayList<>(o.getActiveEffects())) {
                if (!e.getEffect().value().isBeneficial()) o.removeEffect(e.getEffect());
            }
            DanVfx.goldRing(lv, p, o.position(), 1.2);
        }
    }

    /** R Đan Hỏa Phần Thiên: nón lửa 8 block (nửa góc ~53 độ), mỗi mục tiêu gây 1,2 x Phép lực x (1 + 0,15 x tầng độc), tiêu hao toàn bộ độc trên mục tiêu. */
    private static void phanThien(ServerLevel lv, ServerPlayer p, double sp) {
        DanVfx.flameCone(lv, p.getEyePosition(), p.getLookAngle(), DanMath.R_RANGE, DanMath.R_COS);
        for (LivingEntity v : cone(lv, p, DanMath.R_RANGE, DanMath.R_COS)) {
            int stacks = DanRuntime.consumeStacks(v);
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, (float) DanMath.rDamage(sp, stacks));
        }
    }

    /** T Cửu Chuyển Kim Đan: 10 giây mọi kỹ năng không tốn tài nguyên; đòn thường thành bom (r=3, 1,0 x Phép lực, +2 tầng). */
    private static void kimDan(ServerLevel lv, ServerPlayer p) {
        DanRuntime.startKim(lv, p);
        DanVfx.goldRing(lv, p, p.position(), 2.5);
    }

    /** Hình nón trước mặt người chơi: trong {@code range}, lệch góc tối đa theo {@code cosHalf} (gần sát thì luôn trúng). */
    private static List<LivingEntity> cone(ServerLevel lv, ServerPlayer p, double range, double cosHalf) {
        Vec3 look = p.getLookAngle(), eye = p.getEyePosition();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range, 2, range), x -> PhapRuntime.valid(p, x))) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            double l = d.length();
            if (l <= range + e.getBbWidth() / 2 && (l < 0.8 || d.normalize().dot(look) >= cosHalf)) out.add(e);
        }
        return out;
    }
}
