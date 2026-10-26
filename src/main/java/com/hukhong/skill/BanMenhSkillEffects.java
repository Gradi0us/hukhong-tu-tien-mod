package com.hukhong.skill;

import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.entity.HuMob;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.honcot.HonCotService;
import com.hukhong.stats.StatService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Phím R (slot 2): Kỹ năng Bản Mệnh của đạo đang dùng (mở ở cấp 40); Shift+R hoặc khi chưa mở
 * -> Vạn Cốt Cộng Minh (cần >= 1 Hồn Cốt). Cooldown/chi phí đi qua SkillState + Resources.
 */
final class BanMenhSkillEffects {
    private BanMenhSkillEffects() {}

    static final int RESONANCE_CD = 900, RESONANCE_COST = 50;

    /** Skill sẽ chạy cho slot R (null = không có). resonance=true nếu là Vạn Cốt. */
    static BanMenhSkill signature(ServerPlayer p, SkillState st) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        return BanMenhSkill.of(Skill.skillPath(d, st.page), d.level());
    }

    static boolean useResonance(ServerPlayer p, SkillState st) {
        return HonCotService.equippedCount(p) > 0 && (p.isShiftKeyDown() || signature(p, st) == null);
    }

    static void castR(ServerPlayer p, SkillState st) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        boolean res = useResonance(p, st);
        BanMenhSkill s = res ? null : signature(p, st);
        if (!res && s == null) return;
        long now = lv.getGameTime();
        if (st.cooldownEnd[2] > now) return;
        if (com.hukhong.pathrules.MaRules.isLost(p)) {
            p.displayClientMessage(Component.translatable("hukhong.ma.lost_control"), true);
            return;
        }
        int cost = res ? RESONANCE_COST : s.cost, cd = res ? RESONANCE_CD : s.cooldownTicks;
        if (!Resources.tryConsume(p, cost)) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
            return;
        }
        int level = p.getData(ModAttachments.CULTIVATION).level();
        if (res) resonance(lv, p);
        else switch (s) {
            case NHAT_KIEM -> nhatKiem(lv, p, s, level);
            case PHAN_XET -> phanXet(lv, p, s, level);
            case DIA_LIET -> diaLiet(lv, p, s, level);
            case HUYET_HAI -> huyetHai(lv, p, s, level);
        }
        st.cooldownEnd[2] = now + cd;
        st.rTotal = cd;
        st.lastCombat = now;
    }

    private static boolean valid(ServerPlayer p, LivingEntity e) {
        return e != p && !(e instanceof Player) && e.isAlive() && !e.isSpectator();
    }

    private static List<LivingEntity> around(ServerLevel lv, ServerPlayer p, Vec3 c, double r) {
        return lv.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 2, r),
                e -> valid(p, e) && e.position().distanceToSqr(c) <= r * r);
    }

    private static void nhatKiem(ServerLevel lv, ServerPlayer p, BanMenhSkill s, int level) {
        double range = s.range(level);
        Vec3 from = p.position(), dir = p.getLookAngle().multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        float dmg = (float) (s.coef(level) * StatService.attackPower(p));
        for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().expandTowards(dir.scale(range)).inflate(1.5),
                e -> valid(p, e))) {
            Vec3 rel = t.position().subtract(from);
            double along = rel.dot(dir);
            if (along < 0 || along > range + 1) continue;
            if (rel.subtract(dir.scale(along)).horizontalDistance() > 2.0) continue;
            DamageRules.hurtBySkill(p, t, dmg);
        }
        Vec3 to = from.add(dir.scale(range));
        SkillEffects.beam(lv, p.getEyePosition().subtract(0, 0.3, 0), to.add(0, 1, 0));
        p.teleportTo(to.x, p.getY(), to.z); // lướt (kiểm tra va chạm đơn giản: server tự đẩy ra nếu kẹt)
        lv.sendParticles(ParticleTypes.SWEEP_ATTACK, to.x, p.getY(1), to.z, 3, 0.5, 0.3, 0.5, 0);
    }

    private static void phanXet(ServerLevel lv, ServerPlayer p, BanMenhSkill s, int level) {
        double range = s.range(level);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(range));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(lv, p, eye, end, p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0),
                e -> e instanceof LivingEntity && valid(p, (LivingEntity) e));
        if (hit != null) {
            LivingEntity t = (LivingEntity) hit.getEntity();
            double mul = t instanceof HuMob ? 2.0 : 1.0; // x2 vs Hư Hóa
            DamageRules.hurtBySkill(p, t, (float) (s.coef(level) * StatService.spellPower(p) * mul));
            end = hit.getLocation();
            lv.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY(1), t.getZ(), 25, 0.4, 0.6, 0.4, 0.1);
        }
        SkillEffects.beam(lv, eye.add(look.scale(0.8)).subtract(0, 0.2, 0), end);
    }

    private static void diaLiet(ServerLevel lv, ServerPlayer p, BanMenhSkill s, int level) {
        double r = s.range(level);
        float dmg = (float) (s.coef(level) * StatService.attackPower(p));
        int stun = 30 + (level - BanMenhSkill.UNLOCK_LEVEL);
        for (LivingEntity t : around(lv, p, p.position(), r)) {
            DamageRules.hurtBySkill(p, t, dmg);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.min(80, stun), 6));
            t.setDeltaMovement(t.getDeltaMovement().add(0, 0.4, 0));
        }
        SkillEffects.ring(lv, p.position().add(0, 0.2, 0), r, 64, ParticleTypes.CAMPFIRE_COSY_SMOKE);
    }

    /** Huyết Hải: 6 nhịp, mỗi nhịp coef x Công + hút một phần thành máu. */
    private static void huyetHai(ServerLevel lv, ServerPlayer p, BanMenhSkill s, int level) {
        double r = s.range(level);
        Vec3 c = p.position();
        float dmg = (float) (s.coef(level) * StatService.attackPower(p));
        for (int i = 0; i < 6; i++) {
            SkillScheduler.later(i * 20 + 1, () -> {
                if (!p.isAlive() || p.hasDisconnected()) return;
                float healed = 0;
                for (LivingEntity t : around(lv, p, c, r)) {
                    DamageRules.hurtBySkill(p, t, dmg);
                    healed += dmg * 0.5f;
                }
                if (healed > 0) p.heal(Math.min(healed, p.getMaxHealth() * 0.05f));
                SkillEffects.ring(lv, c.add(0, 0.2, 0), r, 48, ParticleTypes.CRIMSON_SPORE);
            });
        }
    }

    private static void resonance(ServerLevel lv, ServerPlayer p) {
        int n = HonCotService.equippedCount(p);
        double coef = HonCotMath.resonanceCoef(n);
        float dmg = (float) (coef * Math.max(StatService.attackPower(p), StatService.spellPower(p)));
        double r = 5 + n;
        for (LivingEntity t : around(lv, p, p.position(), r)) DamageRules.hurtBySkill(p, t, dmg);
        SkillEffects.ring(lv, p.position().add(0, 0.2, 0), r, 64, ParticleTypes.SOUL_FIRE_FLAME);
        lv.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(1), p.getZ(), 1, 0, 0, 0, 0);
    }
}
