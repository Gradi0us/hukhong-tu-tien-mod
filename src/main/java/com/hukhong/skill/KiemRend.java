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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kiếm Tu:
 * <ul>
 *   <li>Xé Toạc: mỗi lần Kiếm Tu gây sát thương lên kẻ địch thêm 1 tầng (tối đa 50, hết sau 8s không bị đánh).</li>
 *   <li>R (Bản Mệnh) kích nổ Xé Toạc: kẻ địch trong 12 ô nhận tầng x 40% Công, xoá tầng.</li>
 *   <li>Bá Thể (khi thi triển Vạn Kiếm Quy Tông và 3s sau đó): không bị đẩy lùi / khống chế, giảm 30% sát thương nhận.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class KiemRend {
    private KiemRend() {}

    public static final int MAX_STACKS = 50, EXPIRE_TICKS = 160, BA_THE_AFTER = 60;
    public static final double DETONATE_PER_STACK = 0.40, DETONATE_RADIUS = 12, BA_THE_DR = 0.30;
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.85f, 0.1f, 0.12f), 0.9f);

    static final class Rend { int stacks; long expire; UUID owner; }

    private static final Map<UUID, Rend> REND = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BA_THE = new ConcurrentHashMap<>();
    private static boolean detonating;

    public static int stacks(LivingEntity e) {
        Rend r = REND.get(e.getUUID());
        return r == null || e.level().getGameTime() > r.expire ? 0 : r.stacks;
    }

    private static boolean isKiem(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).paths().contains(Path.KIEM); }

    /** Bá Thể: tới gameTime until. */
    public static void baThe(ServerPlayer p, long until) { BA_THE.merge(p.getUUID(), until, Math::max); }

    public static boolean inBaThe(Player p) { Long t = BA_THE.get(p.getUUID()); return t != null && p.level().getGameTime() < t; }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p || e.getEntity() instanceof Player) return;
        if (detonating || e.getNewDamage() <= 0 || !isKiem(p) || !(p.level() instanceof ServerLevel lv)) return;
        LivingEntity t = e.getEntity();
        long now = lv.getGameTime();
        Rend r = REND.computeIfAbsent(t.getUUID(), k -> new Rend());
        if (now > r.expire) r.stacks = 0;
        r.stacks = Math.min(MAX_STACKS, r.stacks + 1);
        r.expire = now + EXPIRE_TICKS;
        r.owner = p.getUUID();
        lv.sendParticles(RED, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 2 + r.stacks / 10, 0.25, 0.3, 0.25, 0.02);
        if (r.stacks % 10 == 0 || r.stacks == MAX_STACKS)
            p.displayClientMessage(Component.translatable("hukhong.kiem.rend.stack", r.stacks, MAX_STACKS).withColor(0xE04050), true);
    }

    /** R Bản Mệnh của Kiếm Tu: kích nổ Xé Toạc quanh người. Trả số mục tiêu bị kích nổ. */
    public static int detonate(ServerLevel lv, ServerPlayer p) {
        long now = lv.getGameTime();
        double atk = StatService.attackPower(p);
        int hits = 0;
        for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(DETONATE_RADIUS),
                x -> x.isAlive() && x != p && !(x instanceof Player) && x.distanceToSqr(p) <= DETONATE_RADIUS * DETONATE_RADIUS)) {
            Rend r = REND.get(t.getUUID());
            if (r == null || now > r.expire || r.stacks <= 0 || !p.getUUID().equals(r.owner)) continue;
            int n = r.stacks;
            REND.remove(t.getUUID());
            t.invulnerableTime = 0;
            detonating = true;
            try { DamageRules.hurtBySkill(p, t, (float) (n * DETONATE_PER_STACK * atk)); } finally { detonating = false; }
            hits++;
            Vec3 c = t.getBoundingBox().getCenter();
            float k = n / (float) MAX_STACKS;
            for (int i = 0; i < 3 + n / 8; i++) { // các vết kiếm xé ngang qua mục tiêu
                double a = lv.random.nextDouble() * Math.PI * 2;
                Vec3 d = new Vec3(Math.cos(a), (lv.random.nextDouble() - 0.5) * 0.8, Math.sin(a)).scale(1.2 + 1.2 * k);
                Fx.ribbon(lv, p, c.subtract(d), c.add(d), 0, 3, i, 0.10f + 0.12f * k, i % 2 == 0 ? 0xFF3040 : 0xFFFFFF);
            }
            Fx.play(lv, FxType.SHOCKWAVE, c, Vec3.ZERO, 1.2f + 2.2f * k, 0xE02030, 12);
            lv.sendParticles(RED, c.x, c.y, c.z, 20 + n, 0.4, 0.5, 0.4, 0.15);
            lv.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1 + n / 10, 0.5, 0.4, 0.5, 0);
            lv.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.6f + 0.6f * k);
        }
        if (hits > 0) p.displayClientMessage(Component.translatable("hukhong.kiem.rend.detonate", hits).withColor(0xE04050), true);
        return hits;
    }

    // ---- Bá Thể: không đẩy lùi, không khống chế, giảm sát thương ----

    @SubscribeEvent
    public static void onKnock(LivingKnockBackEvent e) {
        if (e.getEntity() instanceof Player p && inBaThe(p)) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && inBaThe(p)) e.setAmount((float) (e.getAmount() * (1 - BA_THE_DR)));
    }

    /** Gỡ khống chế mỗi tick khi đang Bá Thể. */
    static void cleanse(ServerPlayer p) {
        if (!inBaThe(p)) return;
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.removeEffect(MobEffects.WEAKNESS);
        p.removeEffect(MobEffects.DIG_SLOWDOWN);
        p.removeEffect(MobEffects.BLINDNESS);
        if (p.tickCount % 6 == 0 && p.level() instanceof ServerLevel lv)
            lv.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 3, 0.4, 0.6, 0.4, 0.05);
    }

    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) cleanse(p);
    }
}
