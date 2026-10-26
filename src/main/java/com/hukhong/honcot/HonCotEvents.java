package com.hukhong.honcot;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuVuong;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.gates.RewardGate;
import com.hukhong.stats.StatService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Kỹ năng bị động của Hồn Cốt + nguồn rơi (quái Hư Hóa cấp >= 61, boss thế giới). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HonCotEvents {
    private HonCotEvents() {}

    private static final String PITY = "hk_bone_pity";

    // CD lưu trong persistent data người chơi (tick game; không reset khi relog).
    private static boolean ready(ServerPlayer p, String key) {
        return p.level().getGameTime() >= p.getPersistentData().getLong("hk_bone_cd_" + key);
    }

    private static void setCd(ServerPlayer p, String key, int ticks) {
        p.getPersistentData().putLong("hk_bone_cd_" + key, p.level().getGameTime() + ticks);
    }

    // ------------------------------------------------------------------ nhận sát thương
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            float amt = e.getAmount();
            double guard = HonCotService.power(p, Bone.Fx.GUARD_REDUCE);
            if (guard > 0 && p.isBlocking()) e.setAmount(amt = (float) (amt * (1 - Math.min(0.5, 0.25 * guard))));

            double step = HonCotService.power(p, Bone.Fx.SHADOW_STEP);
            if (step > 0 && ready(p, "step") && amt > 0) { // Ảnh Thiểm: né + lướt 6 ô
                setCd(p, "step", 280);
                e.setCanceled(true);
                shadowStep(p);
                return;
            }
            double ls = HonCotService.power(p, Bone.Fx.LAST_STAND_SHIELD);
            if (ls > 0 && ready(p, "shield") && p.getHealth() - amt < p.getMaxHealth() * 0.30f) {
                setCd(p, "shield", 1800);
                p.setAbsorptionAmount(p.getAbsorptionAmount() + (float) (p.getMaxHealth() * 0.20 * ls));
                p.displayClientMessage(Component.translatable("hukhong.honcot.msg.shield"), true);
            }
            double vo = HonCotService.power(p, Bone.Fx.VOID_ABSORB);
            if (vo > 0 && e.getSource().getEntity() instanceof HuMob) Resources.add(p, Math.round((float) (amt * 0.10 * vo * 4)));
        }
        // người chơi đánh
        if (e.getSource().getEntity() instanceof ServerPlayer p && e.getEntity() != p
                && !DamageRules.isSkill() && e.getEntity() instanceof LivingEntity t && !(t instanceof Player)) {
            double palm = HonCotService.power(p, Bone.Fx.LIGHTNING_PALM);
            if (palm > 0 && ready(p, "palm") && p.level() instanceof ServerLevel sl) {
                setCd(p, "palm", 200);
                LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
                if (b != null) { b.moveTo(t.position()); b.setVisualOnly(true); sl.addFreshEntity(b); }
                DamageRules.hurtBySkill(p, t, (float) (1.5 * StatService.attackPower(p) * palm));
            }
            double claw = HonCotService.power(p, Bone.Fx.BLEED_CLAW);
            if (claw > 0) {
                var tag = p.getPersistentData();
                int n = tag.getInt("hk_bone_hits") + 1;
                if (n >= 4) {
                    n = 0;
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                    if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY(1), t.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                }
                tag.putInt("hk_bone_hits", n);
            }
        }
    }

    private static void shadowStep(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
        Vec3 dir = look.normalize().scale(-1); // lướt ra sau, tránh đòn
        Vec3 to = p.position().add(dir.scale(6));
        if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY(1), p.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
        p.teleportTo(to.x, p.getY(), to.z);
        p.invulnerableTime = Math.max(p.invulnerableTime, 6);
    }

    // ------------------------------------------------------------------ hồi sinh / dậm chân / thấy quái
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        double rv = HonCotService.power(p, Bone.Fx.REVIVE);
        if (rv <= 0 || !ready(p, "revive")) return;
        setCd(p, "revive", 18000);
        e.setCanceled(true);
        p.setHealth((float) Math.max(1, p.getMaxHealth() * Math.min(0.8, 0.40 * rv)));
        p.removeAllEffects();
        p.displayClientMessage(Component.translatable("hukhong.honcot.msg.revive"), true);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getDistance() < 3 || HonCotService.power(p, Bone.Fx.STOMP) <= 0) return;
        for (LivingEntity t : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(3, 1, 3),
                x -> x != p && !(x instanceof Player) && x.isAlive())) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6));
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        if (HonCotService.power(p, Bone.Fx.SEE_HOSTILES) > 0) {
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), x -> x instanceof HuMob && x.isAlive())) {
                m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
        }
    }

    // ------------------------------------------------------------------ nguồn rơi
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        var v = e.getEntity();
        if (v.level().isClientSide) return;
        boolean boss = v instanceof HuVuong;
        if (!(v instanceof HuMob m) || !RewardGate.canReward(v)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer killer)) return;
        var r = v.getRandom();
        int lv = m.huLevel();
        ItemStack drop = null;
        if (boss) {
            var tag = killer.getPersistentData();
            int pity = tag.getInt(PITY);
            if (HonCotMath.bossDrops(pity, r.nextDouble())) {
                tag.putInt(PITY, 0);
                drop = roll(r, lv, true);
            } else tag.putInt(PITY, pity + 1);
        } else if (r.nextDouble() < HonCotMath.dropChance(lv, false)) {
            drop = roll(r, lv, false);
        }
        if (drop != null) e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY() + 0.5, v.getZ(), drop));
    }

    private static ItemStack roll(net.minecraft.util.RandomSource r, int lv, boolean boss) {
        Bone b = HonCotMath.pickBone(r.nextDouble());
        return HonCotRegistry.stack(b, HonCotMath.tierFor(lv, boss, r.nextDouble()), HonCotMath.rollQuality(r.nextDouble(), r.nextDouble()));
    }

    /** Dùng cho lệnh test: Hồn Cốt ngẫu nhiên hoặc chỉ định. */
    public static ItemStack testStack(net.minecraft.util.RandomSource r, Bone b, int tier) {
        return HonCotRegistry.stack(b == null ? HonCotMath.pickBone(r.nextDouble()) : b, tier,
                HonCotMath.rollQuality(r.nextDouble(), r.nextDouble()));
    }
}
