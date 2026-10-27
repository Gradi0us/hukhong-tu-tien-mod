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

import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;

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
            case KIEM_1 -> tramKhi(lv, p);
            case KIEM_2 -> lienHoanTram(lv, p);
            case KIEM_ULT -> kiemVuc(lv, p);
            case MA_1 -> huyetTrao(lv, p);
            case MA_2 -> maKhiHoThe(lv, p);
            case MA_ULT -> maThanGiangThe(lv, p);
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
                e -> e instanceof LivingEntity le && valid(p, le));
        if (eh != null) {
            end = eh.getLocation();
            DamageRules.hurtBySkill(p, (LivingEntity) eh.getEntity(), (float) (Skill.DAO_1_COEF * com.hukhong.stats.StatService.spellPower(p)));
        }
        Vec3 from = eye.add(look.scale(0.8)).subtract(0, 0.2, 0);
        beam(lv, from, end);
        Fx.play(lv, FxType.BEAM, from, end.subtract(from), (float) from.distanceTo(end), Fx.GOLD);
        helixBeam(lv, from, end, 0.35, Math.max(2, from.distanceTo(end) / 2.5), 28, GOLD);
        lv.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 10, 0.2, 0.2, 0.2, 0.15);
        ring(lv, end, 0.7, 12, ParticleTypes.END_ROD);
        com.hukhong.vfx.ModSounds.play(lv, p, com.hukhong.vfx.ModSounds.SKILL_THANH_QUANG, 1.2f, 1.0f);
        layer(lv, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.9f);
        layer(lv, p.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);
    }

    /** Ban Phúc: đồng đội gần nhất trong 20 block nhận Tấn Công+Tốc Độ+Giáp Phép; không có thì bản thân (yếu hơn). */
    private static void banPhuc(ServerLevel lv, ServerPlayer p) {
        boolean ma = p.getData(ModAttachments.CULTIVATION).paths().contains(com.hukhong.core.Path.MA); // Ma Tu: không hỗ trợ người khác
        ServerPlayer target = ma ? null : lv.players().stream()
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
        lv.sendParticles(com.hukhong.vfx.ModParticles.THANH_QUANG.get(), target.getX(), target.getY() + 1, target.getZ(), 15, 0.4, 0.6, 0.4, 0.1);
        spiral(lv, target.position(), 0.9, 0.3, 2.2, 2.5, 30, GOLD);
        Fx.play(lv, FxType.TALISMAN_CIRCLE, target.position(), Vec3.ZERO, 1.8f, Fx.GOLD, 36);
        Fx.play(lv, FxType.LIGHT_PILLAR, target.position(), Vec3.ZERO, 0.4f, Fx.GOLD, 24);
        ring(lv, target.position().add(0, 0.1, 0), 1.0, 16, com.hukhong.vfx.ModParticles.LINH_KHI.get());
        com.hukhong.vfx.ModSounds.play(lv, p, com.hukhong.vfx.ModSounds.SKILL_LINH_KHI, 1f, 1.0f);
        layer(lv, target.position(), SoundEvents.PLAYER_LEVELUP, 0.5f, 1.6f);
    }

    /** Thánh Giáng (tối thượng): AoE 12 block, người chơi trong vùng được hồi + 4 buff; bản thân nhận phần nhẹ hơn. */
    private static void thanhGiang(ServerLevel lv, ServerPlayer p) {
        for (ServerPlayer o : lv.players()) {
            if (!o.isAlive() || o.isSpectator() || o.distanceToSqr(p) > 144) continue;
            boolean self = o == p;
            if (!self && p.getData(ModAttachments.CULTIVATION).paths().contains(com.hukhong.core.Path.MA)) continue;
            float sp = (float) com.hukhong.stats.StatService.spellPower(p);
            o.heal(Math.max(4f, sp * (self ? 0.8f : 0.6f)));
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, self ? 0 : 1));
            if (!self) {
                o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
                o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            }
        }
        ring(lv, p.position().add(0, 0.2, 0), 12, 48, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
        lv.sendParticles(com.hukhong.vfx.ModParticles.THANH_QUANG.get(), p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 1.0, 0.6, 0.3);
        ringLater(lv, p.position().add(0, 0.2, 0), 7, 32, 4, ParticleTypes.END_ROD);
        ringLater(lv, p.position().add(0, 0.2, 0), 3, 20, 8, GOLD);
        spiral(lv, p.position(), 2.2, 0.4, 7, 3, 45, GOLD);
        Fx.play(lv, FxType.TALISMAN_CIRCLE, p.position(), Vec3.ZERO, 11f, Fx.GOLD, 70);
        Fx.play(lv, FxType.LIGHT_PILLAR, p.position(), Vec3.ZERO, 1.8f, Fx.GOLD, 50);
        Fx.play(lv, FxType.SHOCKWAVE, p.position(), Vec3.ZERO, 12f, Fx.WHITE, 22);
        pillar(lv, p.position(), 9, 0.3, 12, 2, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
        com.hukhong.vfx.ModSounds.play(lv, p, com.hukhong.vfx.ModSounds.SKILL_THANH_QUANG, 1.5f, 0.7f);
        layer(lv, p.position(), SoundEvents.BEACON_POWER_SELECT, 1f, 1.2f);
        layer(lv, p.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 0.6f);
    }

    // ---- Thể Tu ----

    /** Chấn Địa: AoE 4 block, 4 + 0.2*cấp, hất tung. */
    private static void chanDia(ServerLevel lv, ServerPlayer p, CultivationData d) {
        AABB box = p.getBoundingBox().inflate(4, 1.5, 4);
        List<LivingEntity> hit = lv.getEntitiesOfClass(LivingEntity.class, box,
                e -> valid(p, e) && e.distanceToSqr(p) <= 16);
        for (LivingEntity e : hit) {
            DamageRules.hurtBySkill(p, e, (float) (Skill.THE_1_COEF * com.hukhong.stats.StatService.attackPower(p)));
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
            e.hurtMarked = true;
        }
        ring(lv, p.position().add(0, 0.2, 0), 4, 28, com.hukhong.vfx.ModParticles.BA_KHI.get());
        Fx.play(lv, FxType.GROUND_CRACK, p.position(), Vec3.ZERO, 5f, Fx.ORANGE);
        Fx.play(lv, FxType.SHOCKWAVE, p.position(), Vec3.ZERO, 5f, Fx.ORANGE);
        Fx.play(lv, FxType.SCREEN_TINT, p.position(), Vec3.ZERO, 0.18f, 0xFFB060, 8);
        lv.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0, 0, 0, 0);
        ring(lv, p.position().add(0, 0.2, 0), 2, 16, ParticleTypes.CAMPFIRE_COSY_SMOKE);
        ringLater(lv, p.position().add(0, 0.2, 0), 3.2, 24, 3, ParticleTypes.POOF);
        for (int i = 0; i < 8; i++) { // vết nứt tỏa ra
            double a = i * Math.PI / 4;
            for (double r = 0.8; r <= 4; r += 0.8)
                lv.sendParticles(ParticleTypes.CRIT, p.getX() + Math.cos(a) * r, p.getY() + 0.15, p.getZ() + Math.sin(a) * r, 1, 0.05, 0, 0.05, 0);
        }
        com.hukhong.vfx.ModSounds.play(lv, p, com.hukhong.vfx.ModSounds.SKILL_BA_KHI, 1.2f, 1.0f);
        layer(lv, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 0.6f);
        layer(lv, p.position(), SoundEvents.ANVIL_LAND, 0.7f, 0.5f);
    }

    /** Thiết Bố Sam: giảm sát thương nhận ~20% trong 6s. */
    private static void thietBoSam(ServerLevel lv, ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        ring(lv, p.position().add(0, 0.3, 0), 1.0, 16, com.hukhong.vfx.ModParticles.BA_KHI.get());
        spiral(lv, p.position(), 0.9, 0.9, 2, 2, 30, com.hukhong.vfx.ModParticles.BA_KHI.get());
        Fx.play(lv, FxType.SHOCKWAVE, p.position(), Vec3.ZERO, 2.4f, Fx.ORANGE);
        Fx.play(lv, FxType.TALISMAN_CIRCLE, p.position(), Vec3.ZERO, 1.6f, 0xFFB050, 30);
        com.hukhong.vfx.ModSounds.play(lv, p, com.hukhong.vfx.ModSounds.SKILL_BA_KHI, 1f, 1.5f);
        layer(lv, p.position(), SoundEvents.IRON_GOLEM_REPAIR, 0.8f, 1.2f);
    }

    /** Kim Thân Bất Hoại (tối thượng): giảm ~40% sát thương + khiên 8s, miễn làm chậm/yếu. */
    private static void kimThan(ServerLevel lv, ServerPlayer p) {
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.removeEffect(MobEffects.WEAKNESS);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 2));
        ring(lv, p.position().add(0, 0.2, 0), 1.2, 24, com.hukhong.vfx.ModParticles.BA_KHI.get());
        lv.sendParticles(com.hukhong.vfx.ModParticles.BA_KHI.get(), p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.9, 0.5, 0.25);
        spiral(lv, p.position(), 1.3, 0.5, 2.6, 3, 45, GOLD);
        Fx.play(lv, FxType.LIGHT_PILLAR, p.position(), Vec3.ZERO, 1.2f, 0xFFC040, 44);
        Fx.play(lv, FxType.SHOCKWAVE, p.position(), Vec3.ZERO, 7f, 0xFFC040, 20);
        Fx.play(lv, FxType.GROUND_CRACK, p.position(), Vec3.ZERO, 4f, 0xFFC040);
        Fx.play(lv, FxType.TALISMAN_CIRCLE, p.position(), Vec3.ZERO, 3f, 0xFFC040, 60);
        ringLater(lv, p.position().add(0, 1.0, 0), 1.5, 20, 3, GOLD);
        ringLater(lv, p.position().add(0, 2.0, 0), 1.0, 14, 6, GOLD);
        com.hukhong.vfx.ModSounds.play(lv, p, com.hukhong.vfx.ModSounds.SKILL_BA_KHI, 1.5f, 0.5f);
        layer(lv, p.position(), SoundEvents.ANVIL_PLACE, 0.8f, 0.7f);
        layer(lv, p.position(), SoundEvents.BEACON_ACTIVATE, 0.7f, 1.4f);
    }


    // ---- Kiếm Tu ----

    private static boolean valid(ServerPlayer p, LivingEntity e) {
        return e != p && !(e instanceof Player) && e.isAlive() && !e.isSpectator()
                && !(e instanceof net.minecraft.world.entity.npc.Npc)
                && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p));
    }

    /** Gây sát thương kỹ năng = coef x Công; bỏ khung bất tử để nhiều nhát liên tiếp đều trúng. Trả sát thương đã đặt. */
    private static float slash(ServerPlayer p, LivingEntity t, double coef) {
        float dmg = (float) (coef * com.hukhong.stats.StatService.attackPower(p));
        t.invulnerableTime = 0;
        DamageRules.hurtBySkill(p, t, dmg);
        return dmg;
    }

    /** Mục tiêu nằm gần tia nhìn dài `range`, bán kính `width`. */
    private static List<LivingEntity> lineTargets(ServerLevel lv, ServerPlayer p, double range, double width) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(width + 1);
        return lv.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!valid(p, e)) return false;
            Vec3 c = e.getBoundingBox().getCenter().subtract(eye);
            double t = c.dot(look);
            if (t < 0 || t > range + 0.5) return false;
            return c.subtract(look.scale(t)).length() <= width + e.getBbWidth() / 2;
        });
    }

    /** Mục tiêu trong hình quạt phía trước (cosHalf = cos nửa góc). */
    private static List<LivingEntity> coneTargets(ServerLevel lv, ServerPlayer p, double range, double cosHalf) {
        Vec3 look = p.getLookAngle(), eye = p.getEyePosition();
        return lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range, 1.5, range), e -> {
            if (!valid(p, e)) return false;
            Vec3 c = e.getBoundingBox().getCenter().subtract(eye);
            double d = c.length();
            return d <= range + e.getBbWidth() / 2 && (d < 0.8 || c.normalize().dot(look) >= cosHalf);
        });
    }

    /** Vòng cung particle trước mặt (lưỡi trăng): nửa góc `half` rad, bán kính r. */
    private static void arc(ServerLevel lv, ServerPlayer p, double r, double half, int n,
                            net.minecraft.core.particles.ParticleOptions type, double tilt) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.4, 0), look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        for (int i = 0; i <= n; i++) {
            double a = -half + 2 * half * i / n;
            Vec3 q = eye.add(flat.scale(Math.cos(a) * r)).add(right.scale(Math.sin(a) * r)).add(0, Math.sin(a) * tilt, 0);
            lv.sendParticles(type, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Trảm Khí: kiếm khí xuyên tầm 8 block, 120% Công. */
    /** Kiếm Ý Cốt: kiếm khí +25% tầm. */
    private static double qiRange(ServerPlayer p) {
        return com.hukhong.honcot.HonCotService.has(p, com.hukhong.honcot.Bone.KIEM_Y) ? com.hukhong.honcot.HonCotMath.KIEM_Y_RANGE_MULT : 1.0;
    }

    private static void tramKhi(ServerLevel lv, ServerPlayer p) {
        final double range = 8 * qiRange(p);
        for (LivingEntity e : lineTargets(lv, p, range, 1.2)) slash(p, e, Skill.KIEM_1_COEF);
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0), look = p.getLookAngle();
        for (double t = 1.0; t <= range; t += 0.5) {
            Vec3 q = eye.add(look.scale(t));
            lv.sendParticles(com.hukhong.vfx.ModParticles.KIEM_KHI.get(), q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
        }
        Fx.play(lv, FxType.SWORD_CRESCENT, eye.add(look.scale(0.8)), look, (float) (range / 8 * 1.5), Fx.CYAN);
        Fx.play(lv, FxType.BEAM, eye.add(look.scale(1.5)), look, (float) range, 0xD8F6FF, 7);
        arc(lv, p, 2.4, 0.9, 10, com.hukhong.vfx.ModParticles.KIEM_KHI.get(), 0.4);
        arc(lv, p, 3.2, 0.7, 8, ParticleTypes.CRIT, 0.3);
        helixBeam(lv, eye.add(look.scale(1.5)), eye.add(look.scale(range)), 0.3, 4, 24, ParticleTypes.END_ROD);
        lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.6f);
        layer(lv, p.position(), SoundEvents.TRIDENT_THROW.value(), 0.6f, 1.8f);
        layer(lv, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.5f);
    }

    /** Liên Hoàn Trảm: 3 nhát quạt 6 block, mỗi nhát 70% Công, cách nhau 6 tick. */
    private static void lienHoanTram(ServerLevel lv, ServerPlayer p) {
        final double reach = 6 * qiRange(p);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Runnable r = () -> {
                if (p.isRemoved() || !p.isAlive() || !(p.level() instanceof ServerLevel l2)) return;
                for (LivingEntity e : coneTargets(l2, p, reach, 0.45)) slash(p, e, Skill.KIEM_2_COEF);
                Fx.play(l2, FxType.SWORD_CRESCENT, p.getEyePosition().subtract(0, 0.5, 0).add(p.getLookAngle().scale(0.5)), p.getLookAngle(), 1.1f + 0.25f * k, k == 1 ? 0xFFFFFF : Fx.CYAN);
                arc(l2, p, 2.0 + k * 0.6, 1.0, 12, com.hukhong.vfx.ModParticles.KIEM_KHI.get(), (k - 1) * 0.5);
                arc(l2, p, 2.6 + k * 0.6, 1.0, 6, ParticleTypes.CRIT, (k - 1) * 0.5);
                l2.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.3f + 0.15f * k);
                layer(l2, p.position(), SoundEvents.IRON_GOLEM_ATTACK, 0.35f, 1.6f + 0.2f * k);
                arc(l2, p, 3.4 + k * 0.5, 0.8, 6, ParticleTypes.END_ROD, (1 - k) * 0.4);
            };
            if (i == 0) r.run(); else SkillScheduler.later(6 * i, r);
        }
    }

    /** Kiếm Vực (tối thượng): vòng kiếm khí bán kính 5 quanh người 4s, mỗi 0,5s 55% Công. */
    private static void kiemVuc(ServerLevel lv, ServerPlayer p) {
        lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
        layer(lv, p.position(), SoundEvents.TRIDENT_THUNDER.value(), 0.6f, 1.5f);
        layer(lv, p.position(), SoundEvents.BEACON_ACTIVATE, 0.7f, 1.8f);
        spiral(lv, p.position(), 1.5, 0.3, 4, 3, 40, com.hukhong.vfx.ModParticles.KIEM_KHI.get());
        Fx.play(lv, FxType.SWORD_DOMAIN, p.position(), Vec3.ZERO, (float) (5 * qiRange(p)), Fx.CYAN, 84);
        Fx.play(lv, FxType.SHOCKWAVE, p.position(), Vec3.ZERO, (float) (6 * qiRange(p)), Fx.CYAN, 18);
        Fx.play(lv, FxType.SCREEN_TINT, p.position(), Vec3.ZERO, 0.15f, 0xBFEFFF, 10);
        kiemVucPulse(p, 0);
    }

    private static void kiemVucPulse(ServerPlayer p, int n) {
        if (n >= 8 || p.isRemoved() || !p.isAlive() || !(p.level() instanceof ServerLevel lv)) return;
        double r = 5 * qiRange(p);
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r, 2, r),
                e -> valid(p, e) && e.distanceToSqr(p) <= r * r)) slash(p, e, Skill.KIEM_ULT_COEF);
        double rot = n * 0.5;
        for (int i = 0; i < 24; i++) {
            double a = rot + 2 * Math.PI * i / 24;
            double yy = p.getY() + 0.9 + 0.5 * Math.sin(a * 2 + n);
            lv.sendParticles(com.hukhong.vfx.ModParticles.KIEM_KHI.get(), p.getX() + Math.cos(a) * r, yy,
                    p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 4; i++) {
            double a = rot + Math.PI / 2 * i;
            lv.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * r * 0.8, p.getY() + 1,
                    p.getZ() + Math.sin(a) * r * 0.8, 1, 0, 0, 0, 0);
        }
        if (n % 2 == 0) {
            Fx.play(lv, FxType.SWORD_RAIN, p.position(), Vec3.ZERO, (float) r, 0xBFEFFF, 36);
            lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.9f);
        }
        SkillScheduler.later(10, () -> kiemVucPulse(p, n + 1));
    }

    // ---- Ma Tu ----

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7f, 0.05f, 0.08f), 1.1f);

    /** Huyết Trảo: 3 vệt móng cận chiến 4 block, 125% Công, Hấp Huyết nhân đôi trên đòn này. */
    private static void huyetTrao(ServerLevel lv, ServerPlayer p) {
        float total = 0;
        for (LivingEntity e : coneTargets(lv, p, 4, 0.5)) total += slash(p, e, Skill.MA_1_COEF);
        double ls = com.hukhong.stats.StatService.value(p, com.hukhong.stats.ModAttributes.LIFESTEAL);
        if (total > 0 && ls > 0) p.heal(Math.min((float) (total * ls), p.getMaxHealth() * 0.04f)); // phần nhân đôi (phần 1 ở DamageRules)
        for (int i = -1; i <= 1; i++)
            Fx.play(lv, FxType.SWORD_CRESCENT, p.getEyePosition().subtract(0, 0.5, 0).add(p.getLookAngle().scale(0.4)), p.getLookAngle(), 0.9f, Fx.RED, 9);
        for (int i = -1; i <= 1; i++) arc(lv, p, 2.2 + 0.1 * i, 0.9, 10, BLOOD, i * 0.45);
        arc(lv, p, 2.0, 0.9, 8, com.hukhong.vfx.ModParticles.MA_KHI.get(), 0);
        if (total > 0) lv.sendParticles(BLOOD, p.getX(), p.getY() + 1.2, p.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
        layer(lv, p.position(), SoundEvents.RAVAGER_ATTACK, 0.5f, 1.4f);
        layer(lv, p.position(), SoundEvents.WITHER_SHOOT, 0.25f, 1.9f);
    }

    /** Ma Khí Hộ Thể: lá chắn 15% HP tối đa trong 6s (chỉ bản thân, qua Absorption). */
    private static void maKhiHoThe(ServerLevel lv, ServerPlayer p) {
        float shield = p.getMaxHealth() * 0.15f;
        int amp = Math.max(0, (int) Math.ceil(shield / 4f) - 1);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, amp, false, false));
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), shield));
        for (int h = 0; h < 3; h++) ring(lv, p.position().add(0, 0.3 + 0.7 * h, 0), h == 1 ? 1.1 : 0.8, 14,
                com.hukhong.vfx.ModParticles.MA_KHI.get());
        spiral(lv, p.position(), 1.0, 1.0, 2.2, 2.5, 32, BLOOD);
        Fx.play(lv, FxType.BLOOD_VORTEX, p.position(), Vec3.ZERO, 1.6f, Fx.RED, 34);
        Fx.play(lv, FxType.TALISMAN_CIRCLE, p.position(), Vec3.ZERO, 1.8f, 0xB01030, 34);
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.5f);
        layer(lv, p.position(), SoundEvents.SOUL_ESCAPE.value(), 0.8f, 0.8f);
    }

    /** Ma Thần Giáng Thế (tối thượng): 12s +40% Công, +Hấp Huyết; Ác Tính +15. Bonus áp ở MaRules. */
    private static void maThanGiangThe(ServerLevel lv, ServerPlayer p) {
        com.hukhong.pathrules.MaRules.activateThan(p);
        lv.sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(), p.getX(), p.getY() + 1, p.getZ(), 25, 0.6, 1.0, 0.6, 0.25);
        ring(lv, p.position().add(0, 0.1, 0), 3, 28, com.hukhong.vfx.ModParticles.MA_KHI.get());
        spiral(lv, p.position(), 2.0, 0.4, 5, 3, 45, ParticleTypes.SOUL_FIRE_FLAME);
        Fx.play(lv, FxType.BLOOD_VORTEX, p.position(), Vec3.ZERO, 4.5f, Fx.RED, 60);
        Fx.play(lv, FxType.DEMON_SILHOUETTE, p.position().add(0, 0.2, 0), Vec3.ZERO, 2.4f, 0xA00C20, 60);
        Fx.play(lv, FxType.GROUND_CRACK, p.position(), Vec3.ZERO, 6f, 0xC01028);
        Fx.play(lv, FxType.SCREEN_TINT, p.position(), Vec3.ZERO, 0.22f, 0x700010, 16);
        pillar(lv, p.position(), 6, 0.35, 10, 2, BLOOD);
        ringLater(lv, p.position().add(0, 0.1, 0), 5.5, 32, 4, BLOOD);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 0.6f);
        layer(lv, p.position(), SoundEvents.WITHER_SPAWN, 0.35f, 1.6f);
    }

    // ---- VFX helpers ----

    static void beam(ServerLevel lv, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.1) return;
        Vec3 step = dir.scale(1.0 / len);
        for (double t = 0; t <= len; t += 0.4) {
            Vec3 q = from.add(step.scale(t));
            lv.sendParticles(com.hukhong.vfx.ModParticles.THANH_QUANG.get(), q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
            if (((int) (t / 0.4)) % 3 == 0) lv.sendParticles(com.hukhong.vfx.ModParticles.LINH_KHI.get(), q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Xoắn ốc đứng: bán kính r0->r1, cao h, turns vòng, n hạt. */
    public static void spiral(ServerLevel lv, Vec3 base, double r0, double r1, double h, double turns, int n,
                              net.minecraft.core.particles.ParticleOptions type) {
        for (int i = 0; i < n; i++) {
            double f = (double) i / n, a = f * turns * 2 * Math.PI, r = r0 + (r1 - r0) * f;
            lv.sendParticles(type, base.x + Math.cos(a) * r, base.y + f * h, base.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Xoắn ốc quanh trục from->to (tia xoắn). */
    public static void helixBeam(ServerLevel lv, Vec3 from, Vec3 to, double r, double turns, int n,
                                 net.minecraft.core.particles.ParticleOptions type) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.5) return;
        Vec3 f = d.scale(1 / len);
        Vec3 u = Math.abs(f.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = f.cross(u).normalize(), up = right.cross(f).normalize();
        for (int i = 0; i < n; i++) {
            double t = (double) i / n, a = t * turns * 2 * Math.PI;
            Vec3 q = from.add(d.scale(t)).add(right.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r));
            lv.sendParticles(type, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** Cột hạt thẳng đứng cao h. */
    public static void pillar(ServerLevel lv, Vec3 c, double h, double r, int rows, int per,
                              net.minecraft.core.particles.ParticleOptions type) {
        for (int i = 0; i < rows; i++)
            lv.sendParticles(type, c.x, c.y + h * i / rows, c.z, per, r, 0.05, r, 0.01);
    }

    /** Vòng nở dần: ring bán kính r sau delay tick. */
    public static void ringLater(ServerLevel lv, Vec3 c, double r, int n, int delay,
                                 net.minecraft.core.particles.ParticleOptions type) {
        SkillScheduler.later(delay, () -> ring(lv, c, r, n, type));
    }

    /** Âm tầng phụ: vanilla pitch riêng. */
    public static void layer(ServerLevel lv, Vec3 at, net.minecraft.sounds.SoundEvent snd, float vol, float pitch) {
        lv.playSound(null, at.x, at.y, at.z, snd, SoundSource.PLAYERS, vol, pitch);
    }

    public static void ring(ServerLevel lv, Vec3 c, double r, int n, net.minecraft.core.particles.ParticleOptions type) {
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            lv.sendParticles(type, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
        }
    }
}
