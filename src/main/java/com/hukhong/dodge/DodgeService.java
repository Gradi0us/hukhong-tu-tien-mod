package com.hukhong.dodge;

import com.hukhong.client.anim.CastAnim;
import com.hukhong.client.anim.CastAnimHook;
import com.hukhong.core.ModAttachments;
import com.hukhong.effect.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server: Thể Lực (hồi/tiêu), Lách Né (lao + i-frame), Né Hoàn Hảo (buff Thân Pháp). */
public final class DodgeService {
    private DodgeService() {}

    /** Giá trị đã gửi client gần nhất (làm tròn 0,5) để tránh spam gói. */
    private static final Map<UUID, Integer> LAST_SENT = new ConcurrentHashMap<>();

    public static int level(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).level(); }

    public static DodgeData data(ServerPlayer p) { return p.getData(ModAttachments.DODGE); }

    public static float max(ServerPlayer p) { return StaminaMath.max(level(p)); }

    public static float stamina(ServerPlayer p) { return data(p).current(max(p)); }

    private static boolean free(ServerPlayer p) { return p.isCreative() || p.isSpectator(); }

    /** Lách Né; trả về true nếu thành công. */
    public static boolean tryDodge(ServerPlayer p, float forward, float strafe) {
        if (!DodgeConfig.enabled() || !p.isAlive() || p.isSpectator() || p.isPassenger() || p.isFallFlying() || p.isSleeping()) return false;
        if (!Float.isFinite(forward) || !Float.isFinite(strafe)) return false;
        forward = Math.max(-1f, Math.min(1f, forward));
        strafe = Math.max(-1f, Math.min(1f, strafe));
        long now = p.level().getGameTime();
        DodgeData d = data(p);
        if (now < d.cooldownUntil()) return false;
        int lvl = level(p);
        float max = StaminaMath.max(lvl), cur = d.current(max), cost = StaminaMath.dodgeCost(lvl);
        if (!free(p) && cur < cost) {
            p.displayClientMessage(Component.translatable("hukhong.dodge.no_stamina"), true);
            return false;
        }
        float next = free(p) ? cur : cur - cost;
        p.setData(ModAttachments.DODGE, new DodgeData(next, now, now, now + StaminaMath.COOLDOWN, false));

        double[] dir = StaminaMath.direction(forward, strafe, p.getYRot());
        double speed = StaminaMath.dodgeSpeed(lvl);
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(dir[0] * speed, p.onGround() ? 0.15 : Math.max(v.y, 0.05), dir[1] * speed);
        p.hurtMarked = true; // đồng bộ vận tốc xuống client
        p.fallDistance = 0;

        int style = StaminaMath.style(forward, strafe);
        CastAnimHook.send(p, style == 0 ? CastAnim.DODGE_ROLL : style == 1 ? CastAnim.DODGE_DASH : CastAnim.BACKSTEP);
        ServerLevel lv = p.serverLevel();
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.7f);
        for (int i = 0; i < 6; i++) { // vệt khí rải ngược hướng lao
            double t = i * 0.35;
            lv.sendParticles(ParticleTypes.CLOUD, p.getX() - dir[0] * t, p.getY() + 0.4, p.getZ() - dir[1] * t, 1, 0.1, 0.2, 0.1, 0.01);
        }
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.25, 0.5, 0.25, 0.02);
        sync(p, next, max, true);
        return true;
    }

    /** Một tick Thể Lực (gọi từ PlayerTickEvent; gametest gọi trực tiếp vì FakePlayer không tick). */
    public static void tickPlayer(ServerPlayer p) {
        long now = p.level().getGameTime();
        DodgeData d = data(p);
        float max = max(p), cur = d.current(max);
        boolean sprint = DodgeConfig.sprintDrain() && p.isSprinting() && !free(p);
        float next = StaminaMath.tick(cur, max, now - d.lastSpend(), sprint);
        if (sprint && next <= 0f) p.setSprinting(false);
        long spend = sprint ? now : d.lastSpend();
        if (next != cur || spend != d.lastSpend() || d.stamina() < 0) p.setData(ModAttachments.DODGE, d.withStamina(next, spend));
        sync(p, next, max, false);
    }

    private static void sync(ServerPlayer p, float cur, float max, boolean force) {
        if (p instanceof FakePlayer) return;
        int key = Math.round(cur * 2f) * 1000 + Math.round(max);
        Integer prev = LAST_SENT.put(p.getUUID(), key);
        if (force || prev == null || prev != key) PacketDistributor.sendToPlayer(p, new DodgeNet.Stamina(cur, max));
    }

    /** Sát thương được phép xuyên i-frame (void, /kill...). */
    static boolean bypasses(DamageSource s) { return s.is(DamageTypeTags.BYPASSES_INVULNERABILITY); }

    /** Đòn tính "né hoàn hảo": do thực thể gây ra hoặc nổ (AoE boss); không tính môi trường (rơi/lửa/chết đuối). */
    static boolean isAttack(DamageSource s) {
        return s.getEntity() != null || s.getDirectEntity() != null || s.is(DamageTypeTags.IS_EXPLOSION);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DamageSource s = e.getSource();
        if (bypasses(s)) return;
        long now = p.level().getGameTime();
        DodgeData d = data(p);
        if (!StaminaMath.inIFrames(now, d.dodgeStart())) return;
        e.setCanceled(true);
        if (!d.perfectUsed() && DodgeConfig.perfectDodge() && isAttack(s) && StaminaMath.inPerfect(now, d.dodgeStart())) {
            p.setData(ModAttachments.DODGE, new DodgeData(d.stamina(), d.lastSpend(), d.dodgeStart(), d.cooldownUntil(), true));
            perfect(p);
        }
    }

    static void perfect(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(ModEffects.THAN_PHAP, StaminaMath.THAN_PHAP_TICKS, 0, false, true, true));
        p.displayClientMessage(Component.translatable("hukhong.dodge.perfect"), true);
        ServerLevel lv = p.serverLevel();
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        for (int i = 0; i < 10; i++) // tàn ảnh: cột hạt hình người tại chỗ vừa đứng
            lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + i * 0.18, p.getZ(), 2, 0.18, 0.02, 0.18, 0.0);
        lv.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.4);
        if (!(p instanceof FakePlayer)) PacketDistributor.sendToPlayer(p, new DodgeNet.Perfect());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) tickPlayer(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST_SENT.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { LAST_SENT.remove(e.getEntity().getUUID()); }
}
