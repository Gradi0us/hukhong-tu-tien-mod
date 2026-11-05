package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ma Tu R - Ma Vực (lĩnh vực của quỷ): bán kính 7 ô quanh người dùng trong 20s.
 * Mở: kẻ địch trong vùng mất ngay 10% máu tối đa. Trong vùng: kỹ năng Ma được cường hoá +30%. Mỗi 10s vùng nổ một lần.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MaDomain {
    private MaDomain() {}

    public static final double RADIUS = 7.0, OPEN_PCT = 0.10, SKILL_MULT = 1.30, BLAST_ATK = 2.5, BLAST_PCT = 0.05;
    /** Trần cho phần % máu (boss): tối đa n x Công. */
    public static final double PCT_CAP_ATK = 15.0;
    public static final int DURATION = 400, BLAST_EVERY = 200;
    static final int RED = 0xC01028, PURPLE = 0x6A10A0;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.75f, 0.03f, 0.08f), 1.6f);

    private static final Map<UUID, Integer> LEFT = new ConcurrentHashMap<>();
    /** Máu đã rút tạm thời theo từng chủ -> mục tiêu (trả lại khi ra khỏi vùng / vùng tắt). */
    private static final Map<UUID, Map<UUID, Float>> DRAINED = new ConcurrentHashMap<>();
    /** Huyết Giáp (E): hết hạn (gameTime). */
    private static final Map<UUID, Long> ARMOR = new ConcurrentHashMap<>();
    public static final double TICK_ATK = 0.6, SELF_DRAIN = 0.10, LIFESTEAL = 0.10, REFLECT = 0.50;
    public static final int ARMOR_TICKS = 160;
    private static final net.minecraft.resources.ResourceLocation LS_ID = com.hukhong.stats.ModAttributes.id("ma/domain_lifesteal");

    public static boolean armored(Player p) { Long t = ARMOR.get(p.getUUID()); return t != null && p.level().getGameTime() < t; }

    /** E - Huyết Giáp: 8s phản 50% sát thương nhận phải về kẻ tấn công. */
    static void armor(ServerLevel lv, ServerPlayer p) {
        ARMOR.put(p.getUUID(), lv.getGameTime() + ARMOR_TICKS);
        p.displayClientMessage(Component.translatable("hukhong.ma.armor.on").withColor(RED), true);
    }

    @SubscribeEvent
    public static void onHurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !armored(p) || !(p.level() instanceof ServerLevel lv)) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity a) || a == p || a instanceof Player) return;
        reflect(lv, p, a, e.getNewDamage());
    }

    /** Huyết Giáp: trả 50% sát thương vừa nhận về kẻ tấn công a. */
    public static void reflect(ServerLevel lv, ServerPlayer p, LivingEntity a, float taken) {
        if (!armored(p)) return;
        float back = (float) (taken * REFLECT);
        if (back <= 0) return;
        a.invulnerableTime = 0;
        DamageRules.hurtBySkill(p, a, back);
        Fx.ribbon(lv, p, p.getBoundingBox().getCenter(), a.getBoundingBox().getCenter(), 0.3, 4, 0, 0.16f, RED);
        lv.sendParticles(BLOOD, a.getX(), a.getY() + a.getBbHeight() * 0.6, a.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
        lv.playSound(null, a.getX(), a.getY(), a.getZ(), SoundEvents.THORNS_HIT, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    private static void setLifesteal(ServerPlayer p, boolean on) {
        var inst = p.getAttribute(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.LIFESTEAL));
        if (inst == null) return;
        if (on) inst.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(LS_ID, LIFESTEAL,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        else inst.removeModifier(LS_ID);
    }

    /** Trả lại phần máu đã rút cho mục tiêu (nếu còn sống). */
    private static void restore(ServerLevel lv, ServerPlayer p, UUID id, float amt) {
        if (lv.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
            e.heal(amt);
            lv.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 2, 0.2, 0.1, 0.2, 0);
        }
    }

    /** Tái kích hoạt R khi Ma Vực đang mở: nổ = máu tối đa bản thân x số kẻ địch trong vùng, lên từng kẻ địch; vùng tắt. */
    public static void detonate(ServerLevel lv, ServerPlayer p) {
        List<LivingEntity> in = inside(lv, p);
        int n = in.size();
        float dmg = p.getMaxHealth() * Math.max(1, n);
        Vec3 c = p.position();
        for (LivingEntity e : in) {
            e.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, e, dmg);
            e.setDeltaMovement(e.position().subtract(c).multiply(1, 0, 1).normalize().scale(1.2).add(0, 0.7, 0));
            e.hurtMarked = true;
        }
        DRAINED.remove(p.getUUID()); // máu đã rút bị nuốt, không trả lại
        Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.5, 0), Vec3.ZERO, (float) RADIUS * 1.8f, RED, 22);
        Fx.play(lv, FxType.BLOOD_VORTEX, c, Vec3.ZERO, (float) RADIUS * 1.3f, RED, 30);
        Fx.play(lv, FxType.DEMON_SILHOUETTE, c, Vec3.ZERO, 3.4f, PURPLE, 30);
        Fx.play(lv, FxType.SCREEN_TINT, c, Vec3.ZERO, 0.45f, 0x900010, 18);
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), (float) RADIUS + 2, RED, 16, 70);
        Fx.pool(lv, p, c.add(0, 0.05, 0), (float) RADIUS, 0x3A0008, 80);
        Fx.shake(lv, c, 1.6f, 24, 40f);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            Vec3 q = c.add(Math.cos(a) * RADIUS, 0.2, Math.sin(a) * RADIUS);
            Fx.ribbon(lv, p, c.add(0, 1, 0), q.add(0, 2.5, 0), 0.6, 6, i % 3, 0.25f, i % 2 == 0 ? RED : PURPLE);
        }
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 2, 1, 0.5, 1, 0);
        lv.sendParticles(BLOOD, c.x, c.y + 1.5, c.z, 300, RADIUS * 0.5, 1.2, RADIUS * 0.5, 0.4);
        lv.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2f, 0.5f);
        lv.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.8f, 1.2f);
        p.displayClientMessage(Component.translatable("hukhong.ma.domain.detonate", n).withColor(RED), true);
        end(p);
    }

    public static boolean active(Player p) { return LEFT.getOrDefault(p.getUUID(), 0) > 0; }

    public static int left(Player p) { return LEFT.getOrDefault(p.getUUID(), 0); }

    /** Hệ số cường hoá cho kỹ năng của Ma Tu khi đang mở Ma Vực. */
    public static double skillMult(ServerPlayer p) {
        return active(p) && p.getData(ModAttachments.CULTIVATION).paths().contains(Path.MA) ? SKILL_MULT : 1.0;
    }

    private static List<LivingEntity> inside(ServerLevel lv, ServerPlayer p) {
        return lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RADIUS, 4, RADIUS),
                e -> e != p && e.isAlive() && !(e instanceof Player) && !(e instanceof net.minecraft.world.entity.npc.Npc)
                        && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p))
                        && Math.hypot(e.getX() - p.getX(), e.getZ() - p.getZ()) <= RADIUS + e.getBbWidth() / 2);
    }

    private static float pct(ServerPlayer p, LivingEntity e, double pct) {
        return (float) Math.min(e.getMaxHealth() * pct, StatService.attackPower(p) * PCT_CAP_ATK);
    }

    static void cast(ServerLevel lv, ServerPlayer p) {
        LEFT.put(p.getUUID(), DURATION);
        sync(p, DURATION);
        Vec3 c = p.position();
        setLifesteal(p, true);
        Map<UUID, Float> dr = DRAINED.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>());
        for (LivingEntity e : inside(lv, p)) {
            e.invulnerableTime = 0;
            float before = e.getHealth();
            DamageRules.hurtBySkill(p, e, pct(p, e, OPEN_PCT));
            float took = Math.max(0, before - e.getHealth());
            if (took > 0 && e.isAlive()) dr.merge(e.getUUID(), took, Float::sum);
            Fx.ribbon(lv, p, c.add(0, 0.2, 0), e.getBoundingBox().getCenter(), 0.4, 5, 0, 0.14f, RED);
            lv.sendParticles(BLOOD, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 14, 0.3, 0.4, 0.3, 0.05);
        }
        Fx.play(lv, FxType.DEMON_SILHOUETTE, c, Vec3.ZERO, 2.6f, RED, 40);
        Fx.play(lv, FxType.BLOOD_VORTEX, c, Vec3.ZERO, (float) RADIUS, RED, 30);
        Fx.play(lv, FxType.SCREEN_TINT, c, Vec3.ZERO, 0.3f, 0x700010, 16);
        Fx.telegraphCircle(lv, p, c.add(0, 0.05, 0), (float) RADIUS, 20, RED);
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), (float) RADIUS + 1, RED, 12, 60);
        Fx.shake(lv, c, 0.8f, 14, 24f);
        lv.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8f, 0.7f);
        lv.playSound(null, c.x, c.y, c.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.2f, 0.6f);
        p.displayClientMessage(Component.translatable("hukhong.ma.domain.open").withColor(RED), true);
    }

    private static void blast(ServerLevel lv, ServerPlayer p) {
        Vec3 c = p.position();
        float atk = (float) (StatService.attackPower(p) * BLAST_ATK);
        for (LivingEntity e : inside(lv, p)) {
            e.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, e, atk + pct(p, e, BLAST_PCT));
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) away = away.normalize();
            e.setDeltaMovement(away.x * 0.6, 0.45, away.z * 0.6);
            e.hurtMarked = true;
        }
        Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.3, 0), Vec3.ZERO, (float) RADIUS, RED, 16);
        Fx.play(lv, FxType.BLOOD_VORTEX, c, Vec3.ZERO, (float) RADIUS, PURPLE, 20);
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), (float) RADIUS, RED, 18);
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), (float) RADIUS, PURPLE, 10, 40);
        Fx.shake(lv, c, 0.9f, 12, 24f);
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            Vec3 q = c.add(Math.cos(a) * RADIUS * 0.8, 0.2, Math.sin(a) * RADIUS * 0.8);
            Fx.ribbon(lv, p, q, q.add(0, 3.5, 0), 0, 6, i % 4, 0.22f, i % 2 == 0 ? RED : PURPLE); // cột máu trồi quanh vành
        }
        lv.sendParticles(BLOOD, c.x, c.y + 1, c.z, 120, RADIUS * 0.4, 0.6, RADIUS * 0.4, 0.2);
        lv.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 0.5, c.z, 60, RADIUS * 0.4, 0.3, RADIUS * 0.4, 0.08);
        lv.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.6f);
        lv.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) tick(p);
    }

    /** Mỗi tick người chơi (test gọi tay vì người chơi giả không tick). */
    public static void tick(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        Integer l = LEFT.get(p.getUUID());
        if (l == null) return;
        if (!p.isAlive() || l <= 0) { end(p); return; }
        l--;
        LEFT.put(p.getUUID(), l);
        hover(lv, p);
        Map<UUID, Float> dr = DRAINED.get(p.getUUID());
        if (dr != null && p.tickCount % 5 == 0 && !dr.isEmpty()) {
            java.util.Set<UUID> in = new java.util.HashSet<>();
            for (LivingEntity x : inside(lv, p)) in.add(x.getUUID());
            dr.entrySet().removeIf(en -> {
                if (in.contains(en.getKey())) return false;
                restore(lv, p, en.getKey(), en.getValue()); // ra khỏi Ma Vực: trả lại 10% máu
                return true;
            });
        }
        if ((DURATION - l) % 20 == 0) {
            float tick = (float) (StatService.attackPower(p) * TICK_ATK);
            for (LivingEntity x : inside(lv, p)) {
                x.invulnerableTime = 0;
                DamageRules.hurtBySkill(p, x, tick);
                lv.sendParticles(BLOOD, x.getX(), x.getY() + x.getBbHeight() * 0.5, x.getZ(), 5, 0.25, 0.3, 0.25, 0.02);
            }
            float self = (float) (p.getMaxHealth() * SELF_DRAIN); // giá phải trả: -10% máu tối đa/giây (hút máu hồi lại được)
            p.setHealth(Math.max(1f, p.getHealth() - self));
            lv.sendParticles(BLOOD, p.getX(), p.getY() + 1, p.getZ(), 10, 0.3, 0.5, 0.3, 0.03);
        }
        int age = DURATION - l;
        if (age % BLAST_EVERY == 0) blast(lv, p);
        Vec3 c = p.position();
        Vec3 g = groundBelow(lv, p);
        if (age % 4 == 0) Fx.groundRing(lv, null, g.add(0, 0.06, 0), (float) RADIUS, age % 8 == 0 ? RED : PURPLE, 6); // vòng bán kính đi theo người
        if (age % 20 == 1) {
            Fx.pool(lv, null, g.add(0, 0.05, 0), (float) RADIUS, 0x3A0008, 22);
            Fx.rune(lv, null, g.add(0, 0.07, 0), (float) RADIUS * 0.85f, RED, 22, 9);
        }
        if (age % 10 == 0) { // sương máu

            lv.sendParticles(BLOOD, c.x, c.y + 0.3, c.z, 24, RADIUS * 0.45, 0.2, RADIUS * 0.45, 0.01);
            lv.sendParticles(ParticleTypes.SOUL, c.x, c.y + 0.5, c.z, 4, RADIUS * 0.4, 0.4, RADIUS * 0.4, 0.02);
        }
        if (age % 20 == 0 && l > 0) p.displayClientMessage(Component.translatable("hukhong.ma.domain.left", l / 20,
                (BLAST_EVERY - age % BLAST_EVERY) / 20).withColor(RED), true);
        if (l == 0) { end(p); p.displayClientMessage(Component.translatable("hukhong.ma.domain.end").withColor(0x999999), true); }
    }

    private static void sync(ServerPlayer p, int ticks) {
        var pl = new MaDomainPayload(p.getId(), ticks);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, pl);
    }

    private static void end(ServerPlayer p) {
        LEFT.remove(p.getUUID());
        setLifesteal(p, false);
        Map<UUID, Float> dr = DRAINED.remove(p.getUUID());
        if (dr != null && p.level() instanceof ServerLevel lv) dr.forEach((id, amt) -> restore(lv, p, id, amt)); // vùng tắt: trả lại
        p.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
        p.fallDistance = 0;
        sync(p, 0);
    }

    private static Vec3 groundBelow(ServerLevel lv, ServerPlayer p) {
        var hit = lv.clip(new net.minecraft.world.level.ClipContext(p.position().add(0, 0.2, 0), p.position().add(0, -6, 0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? p.position() : hit.getLocation();
    }

    /** Bay lơ lửng ~1 ô trên mặt đất trong lúc mở Ma Vực (vẫn đi ngang bình thường). */
    private static void hover(ServerLevel lv, ServerPlayer p) {
        if (p.getAbilities().flying) return;
        double gap = p.getY() - groundBelow(lv, p).y;
        p.fallDistance = 0;
        // dùng hiệu ứng (client tự mô phỏng chuyển động, không giật): thấp hơn 1 ô -> bay lên nhẹ; cao hơn -> rơi chậm
        if (gap < 0.95) {
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 6, 0, false, false, false));
        } else {
            p.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
            if (gap < 3) p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 6, 0, false, false, false));
        }
        if (p.tickCount % 3 == 0) lv.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() - 0.1, p.getZ(), 3, 0.3, 0.05, 0.3, 0.01);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) end(p);
    }

    /** Test / tắt thế. */
    public static void reset(Player p) { if (p instanceof ServerPlayer sp) end(sp); else LEFT.remove(p.getUUID()); }
}
