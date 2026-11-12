package com.hukhong.phapkhi;

import com.hukhong.HuKhong;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Hộ Pháp Khí: Hộ Thân (khiên hấp thụ), Kim Cang (giảm sát thương), Độn Không (chớp bước), Hồi Xuân (hồi máu). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhapKhiDefense {
    private PhapKhiDefense() {}

    /** Khiên hấp thụ = % máu tối đa, 8 giây. */
    public static final double[] SHIELD_PCT = {0.20, 0.35, 0.50, 0.75, 1.00};
    public static final int SHIELD_TICKS = 160;
    /** Kim Cang: giảm sát thương 30..60%, 6 giây. */
    public static final double[] REDUCE = {0.30, 0.38, 0.45, 0.52, 0.60};
    public static final int REDUCE_TICKS = 120;
    /** Độn Không: tầm chớp 8..20 block; bất tử ngắn sau khi chớp. */
    public static final double[] BLINK = {8, 11, 14, 17, 20};
    public static final int BLINK_INVULN_TICKS = 15;
    /** Hồi Xuân: hồi % máu tối đa. */
    public static final double[] HEAL_PCT = {0.20, 0.35, 0.50, 0.70, 1.00};

    private record Buff(long end, double pct) {}

    /** Giữ theo UUID người chơi (không giữ tham chiếu thực thể). */
    private static final Map<UUID, Buff> REDUCTION = new HashMap<>();
    private static final Map<UUID, Long> INVULN = new HashMap<>();

    /** qm = hệ số phẩm chế; nhân vào khiên Hộ Thân. */
    static boolean use(ServerPlayer p, PhapKhiKind kind, PhapKhiTier tier, double qm) {
        if (!(p.level() instanceof ServerLevel lv)) return false;
        int t = tier.ordinal();
        long now = lv.getGameTime();
        switch (kind) {
            case HO_THAN -> {
                float shield = (float) (p.getMaxHealth() * SHIELD_PCT[t] * qm);
                int amp = Math.min(250, Math.max(0, (int) Math.ceil(shield / 4f) - 1));
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHIELD_TICKS, amp, false, false));
                p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), shield));
                shellFx(lv, p, 0xFFD866, 1.3f + 0.1f * t, SHIELD_TICKS);
                lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
            case KIM_CANG -> {
                REDUCTION.put(p.getUUID(), new Buff(now + REDUCE_TICKS, REDUCE[t]));
                shellFx(lv, p, 0xE0B040, 1.4f + 0.1f * t, REDUCE_TICKS);
                Fx.rune(lv, p, p.position().add(0, 0.06, 0), 1.8f, 0xFFD866, 40, 8);
                lv.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.6f);
                lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            case DON_KHONG -> {
                if (!blink(lv, p, BLINK[t])) return false;
            }
            case HOI_XUAN -> {
                p.heal((float) (p.getMaxHealth() * HEAL_PCT[t]));
                shellFx(lv, p, 0x7FE06A, 1.2f, 30);
                lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
                Fx.groundRing(lv, p, p.position().add(0, 0.06, 0), 2f, 0x7FE06A, 24);
                lv.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.1f);
            }
            default -> { return false; }
        }
        return true;
    }

    private static void shellFx(ServerLevel lv, ServerPlayer p, int rgb, float radius, int life) {
        Fx.shell(lv, p, p, radius, rgb, life);
        Fx.rune(lv, p, p.position().add(0, 0.06, 0), radius + 0.5f, rgb, Math.min(life, 40), 6);
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.8, 0.5, 0.05);
    }

    /** Chớp theo hướng nhìn: dừng trước vật cản, tìm chỗ đứng không kẹt khối, bất tử ngắn, xoá rơi. */
    static boolean blink(ServerLevel lv, ServerPlayer p, double dist) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        var hit = lv.clip(new ClipContext(eye, eye.add(look.scale(dist)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double max = hit.getType() == HitResult.Type.MISS ? dist : Math.max(0, eye.distanceTo(hit.getLocation()) - 0.6);
        Vec3 from = p.position();
        for (double d = max; d >= 1.0; d -= 0.5) {
            Vec3 feet = from.add(look.scale(d));
            for (double up : new double[]{0, 1, 2}) {
                Vec3 dest = feet.add(0, up, 0);
                if (!lv.noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) continue;
                Vec3 mid = from.add(0, 1, 0);
                Fx.ribbon(lv, p, mid, dest.add(0, 1, 0), 0, 4, 0, 0.3f, 0x8FE6FF);
                Fx.rune(lv, p, from.add(0, 0.06, 0), 1.6f, 0x8FE6FF, 20, 5);
                lv.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 40, 0.4, 0.8, 0.4, 0.5);
                p.teleportTo(lv, dest.x, dest.y, dest.z, p.getYRot(), p.getXRot());
                if (p.position().distanceToSqr(dest) > 0.01) p.moveTo(dest.x, dest.y, dest.z, p.getYRot(), p.getXRot()); // người chơi giả (không có kết nối thật)
                p.fallDistance = 0;
                INVULN.put(p.getUUID(), lv.getGameTime() + BLINK_INVULN_TICKS);
                Fx.rune(lv, p, dest.add(0, 0.06, 0), 1.6f, 0x8FE6FF, 20, 5);
                Fx.shell(lv, p, p, 1.2f, 0x8FE6FF, BLINK_INVULN_TICKS);
                lv.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1, dest.z, 40, 0.4, 0.8, 0.4, 0.5);
                lv.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
                return true;
            }
        }
        p.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("hukhong.phapkhi.no_room", "Không đủ chỗ để độn."), true);
        return false;
    }

    /** Hệ số còn lại sau khi trừ mọi giảm thương đang có (Kim Cang x Hộ Trận). */
    public static double damageFactor(Player p, long now) {
        double f = 1.0;
        Buff b = REDUCTION.get(p.getUUID());
        if (b != null) {
            if (b.end() < now) REDUCTION.remove(p.getUUID()); else f *= 1.0 - b.pct();
        }
        return f * PhapKhiZones.damageFactor(p);
    }

    public static boolean invulnerable(Player p, long now) {
        Long end = INVULN.get(p.getUUID());
        if (end == null) return false;
        if (end < now) { INVULN.remove(p.getUUID()); return false; }
        return true;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.level().isClientSide) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        long now = p.level().getGameTime();
        if (invulnerable(p, now)) { e.setCanceled(true); return; }
        double f = damageFactor(p, now);
        if (f < 1.0) e.setAmount((float) (e.getAmount() * f));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        REDUCTION.remove(e.getEntity().getUUID());
        INVULN.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        REDUCTION.clear();
        INVULN.clear();
    }
}
