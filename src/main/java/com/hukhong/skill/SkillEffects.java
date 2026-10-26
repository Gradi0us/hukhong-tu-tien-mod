package com.hukhong.skill;

import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.gates.DamageRules;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

/** Hiệu ứng kỹ năng (số liệu đơn giản hóa) + VFX/âm thanh placeholder vanilla. */
public final class SkillEffects {
    private SkillEffects() {}

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 1.2f);

    static void run(Skill s, ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        switch (s) {
            case DAO_1 -> thanhQuangTien(lv, p, d);
            case DAO_2 -> banPhuc(lv, p);
            case DAO_ULT -> thanhGiang(lv, p);
            case THE_1 -> chanDia(lv, p, d);
            case THE_2 -> thietBoSam(lv, p);
            case THE_ULT -> kimThan(lv, p);
        }
    }

    // ---- Đạo Tu ----

    /** Thánh Quang Tiễn: tia sáng 14 block, sát thương 6 + 0.3*cấp lên 1 mục tiêu không phải người chơi. */
    private static void thanhQuangTien(ServerLevel lv, ServerPlayer p, CultivationData d) {
        double range = 14;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult bh = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(lv, p, eye, end, box,
                e -> e instanceof LivingEntity && !(e instanceof Player) && e.isAlive() && !e.isSpectator());
        if (eh != null) {
            end = eh.getLocation();
            DamageRules.hurtBySkill(p, (LivingEntity) eh.getEntity(), (float) (1.10 * com.hukhong.stats.StatService.spellPower(p)));
        }
        beam(lv, eye.add(look.scale(0.8)).subtract(0, 0.2, 0), end);
        lv.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
    }

    /** Ban Phúc: đồng đội gần nhất trong 20 block nhận Tấn Công+Tốc Độ+Giáp Phép; không có thì bản thân (yếu hơn). */
    private static void banPhuc(ServerLevel lv, ServerPlayer p) {
        ServerPlayer target = lv.players().stream()
                .filter(o -> o != p && o.isAlive() && !o.isSpectator() && o.distanceToSqr(p) <= 400)
                .min(Comparator.comparingDouble(o -> o.distanceToSqr(p))).orElse(null);
        if (target == null) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 0));
            target = p;
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
            beam(lv, p.getEyePosition().subtract(0, 0.3, 0), target.getEyePosition());
        }
        lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 15, 0.4, 0.6, 0.4, 0.1);
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.2f);
    }

    /** Thánh Giáng (tối thượng): AoE 12 block, người chơi trong vùng được hồi + 4 buff; bản thân nhận phần nhẹ hơn. */
    private static void thanhGiang(ServerLevel lv, ServerPlayer p) {
        for (ServerPlayer o : lv.players()) {
            if (!o.isAlive() || o.isSpectator() || o.distanceToSqr(p) > 144) continue;
            boolean self = o == p;
            float sp = (float) com.hukhong.stats.StatService.spellPower(p);
            o.heal(Math.max(4f, sp * (self ? 0.8f : 0.6f)));
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, self ? 0 : 1));
            if (!self) {
                o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
                o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            }
        }
        ring(lv, p.position().add(0, 0.2, 0), 12, 48, ParticleTypes.END_ROD);
        lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 1.0, 0.6, 0.3);
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1f);
    }

    // ---- Thể Tu ----

    /** Chấn Địa: AoE 4 block, 4 + 0.2*cấp, hất tung. */
    private static void chanDia(ServerLevel lv, ServerPlayer p, CultivationData d) {
        AABB box = p.getBoundingBox().inflate(4, 1.5, 4);
        List<LivingEntity> hit = lv.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != p && !(e instanceof Player) && e.isAlive() && e.distanceToSqr(p) <= 16);
        for (LivingEntity e : hit) {
            DamageRules.hurtBySkill(p, e, (float) (1.0 * com.hukhong.stats.StatService.attackPower(p)));
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
            e.hurtMarked = true;
        }
        ring(lv, p.position().add(0, 0.2, 0), 4, 28, ParticleTypes.CLOUD);
        lv.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0, 0, 0, 0);
        lv.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    /** Thiết Bố Sam: giảm sát thương nhận ~20% trong 6s. */
    private static void thietBoSam(ServerLevel lv, ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        ring(lv, p.position().add(0, 0.3, 0), 1.0, 16, ParticleTypes.CRIT);
        lv.playSound(null, p.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1f, 1f);
    }

    /** Kim Thân Bất Hoại (tối thượng): giảm ~40% sát thương + khiên 8s, miễn làm chậm/yếu. */
    private static void kimThan(ServerLevel lv, ServerPlayer p) {
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.removeEffect(MobEffects.WEAKNESS);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 2));
        ring(lv, p.position().add(0, 0.2, 0), 1.2, 24, ParticleTypes.FLAME);
        lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.9, 0.5, 0.25);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    // ---- VFX helpers ----

    static void beam(ServerLevel lv, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.1) return;
        Vec3 step = dir.scale(1.0 / len);
        for (double t = 0; t <= len; t += 0.4) {
            Vec3 q = from.add(step.scale(t));
            lv.sendParticles(GOLD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            if (((int) (t / 0.4)) % 3 == 0) lv.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    public static void ring(ServerLevel lv, Vec3 c, double r, int n, net.minecraft.core.particles.ParticleOptions type) {
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            lv.sendParticles(type, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
        }
    }
}
