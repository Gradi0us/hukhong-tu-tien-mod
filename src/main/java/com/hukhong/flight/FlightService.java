package com.hukhong.flight;

import com.hukhong.HuKhong;
import com.hukhong.banmenh.BanMenhAttachments;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bay server-authoritative: mayfly + flyingSpeed (Kiếm/Đạo/Ma), nhảy bậc không (Thể).
 * Chi phí qua Resources (nhân hệ số đa đạo). Hết tài nguyên / boss thế giới gần -> ngắt + giảm rơi.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class FlightService {
    private FlightService() {}

    private static final float VANILLA_FLY_SPEED = 0.05f;
    private static final int SLOW_FALL_TICKS = 60;
    private static final int FALL_GRACE_TICKS = 100;
    private static final int JUMP_COOLDOWN = 6;
    private static final int COMBAT_TICKS = 100;

    private static final class State {
        FlightMode mode; int pairs; double acc; int jumps; long lastJump;
    }

    private static final Map<UUID, State> STATE = new HashMap<>();
    private static final Map<UUID, Long> GRACE = new HashMap<>();

    public static boolean isActive(ServerPlayer p) { return STATE.containsKey(p.getUUID()); }

    /** Kiểu bay khả dụng: đạo chính ưu tiên, rồi các đạo còn lại đủ cấp (Kiếm cần Bản Mệnh Kiếm). */
    public static FlightMode modeFor(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        Path primary = d.primaryPath();
        FlightMode best = null;
        for (FlightMode m : FlightMode.values()) {
            if (!d.paths().contains(m.path) || d.level() < m.minLevel) continue;
            if (m == FlightMode.KIEM && !p.getData(BanMenhAttachments.BAN_MENH.get()).hasType()) continue;
            if (best == null || m.path == primary) best = m;
        }
        return best;
    }

    public static void toggle(ServerPlayer p) {
        if (isActive(p)) { stop(p, "hukhong.flight.off", false); return; }
        if (p.isCreative() || p.isSpectator()) return;
        FlightMode m = modeFor(p);
        if (m == null) { msg(p, "hukhong.flight.locked"); return; }
        String ban = FlightBans.banKey(p, m);
        if (ban != null) { msgFb(p, ban); return; }
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        if (d.resource() < m.startMin) { msg(p, "hukhong.flight.empty"); return; }
        State s = new State();
        s.mode = m; s.pairs = FlightMode.wingPairs(d.level());
        STATE.put(p.getUUID(), s);
        if (m.trueFlight()) {
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.getAbilities().setFlyingSpeed(speedFor(m, s.pairs));
            p.onUpdateAbilities();
            p.setDeltaMovement(p.getDeltaMovement().add(0, 0.3, 0));
            p.hurtMarked = true;
        }
        sync(p, s);
        msg(p, "hukhong.flight.on." + m.name().toLowerCase());
    }

    /** Thể tu: nhảy thêm giữa không, chỉ tốn Bá Khí. */
    public static void airJump(ServerPlayer p) {
        State s = STATE.get(p.getUUID());
        if (s == null || s.mode != FlightMode.THE) return;
        if (p.onGround() || p.isInWater() || p.onClimbable() || p.getAbilities().flying) return;
        long now = p.level().getGameTime();
        if (now - s.lastJump < JUMP_COOLDOWN) return;
        int level = p.getData(ModAttachments.CULTIVATION).level();
        int max = level >= 95 ? 5 : level >= 80 ? 4 : 3;
        if (s.jumps >= max) return;
        if (!Resources.tryConsume(p, 3, moveFactor(p))) { stop(p, "hukhong.flight.empty", true); return; }
        s.jumps++; s.lastJump = now;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, 0.55, v.z);
        p.fallDistance = 0;
        p.hurtMarked = true;
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 8, 0.25, 0.02, 0.25, 0.01);
    }

    public static void stop(ServerPlayer p, String msgKey, boolean cutoff) {
        State s = STATE.remove(p.getUUID());
        if (s == null) return;
        if (s.mode.trueFlight() && !p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.getAbilities().setFlyingSpeed(VANILLA_FLY_SPEED);
            p.onUpdateAbilities();
        }
        if (!p.onGround()) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false, true));
            GRACE.put(p.getUUID(), p.level().getGameTime() + FALL_GRACE_TICKS);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new FlightSyncPayload(p.getId(), -1, 0));
        if (msgKey != null) msg(p, msgKey);
    }

    private static float speedFor(FlightMode m, int pairs) {
        if (m == FlightMode.DAO) return pairs >= 3 ? 0.09f : pairs == 2 ? 0.075f : m.speed;
        return m.speed;
    }

    private static void sync(ServerPlayer p, State s) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new FlightSyncPayload(p.getId(), s.mode.ordinal(), s.pairs));
    }

    /** Phong Hành Cốt: bay/lướt -15% tiêu hao. */
    private static double moveFactor(ServerPlayer p) {
        return com.hukhong.honcot.HonCotService.has(p, com.hukhong.honcot.Bone.PHONG_HANH) ? com.hukhong.honcot.HonCotMath.PHONG_HANH_COST_FACTOR : 1.0;
    }

    private static void msgFb(ServerPlayer p, String key) {
        String vi = switch (key) {
            case "hukhong.flight.banned_age" -> "Thiên Nhân Ngũ Suy giai đoạn V: không thể bay.";
            case "hukhong.flight.banned_hu_hoa" -> "Đang nhiễm Hư Hóa: không thể ngự kiếm.";
            default -> null;
        };
        p.displayClientMessage(vi == null ? Component.translatable(key) : Component.translatableWithFallback(key, vi), true);
    }

    private static void msg(ServerPlayer p, String key) { msgFb(p, key); }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        State s = STATE.get(p.getUUID());
        if (s == null) return;
        if (p.isCreative() || p.isSpectator() || !p.isAlive()) { stop(p, null, false); return; }
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        if (d.level() < s.mode.minLevel) { stop(p, "hukhong.flight.locked", true); return; }
        if (p.tickCount % 20 == 0) {
            String ban = FlightBans.banKey(p, s.mode);
            if (ban != null) { stop(p, ban, true); return; }
        }

        if (s.mode == FlightMode.THE) {
            if (p.onGround()) s.jumps = 0;
            if (d.resource() < 5) stop(p, "hukhong.flight.empty", true);
            return;
        }
        // Giữ abilities đúng (server-authoritative).
        if (!p.getAbilities().mayfly) { p.getAbilities().mayfly = true; p.onUpdateAbilities(); }
        float spd = speedFor(s.mode, s.pairs);
        if (p.getAbilities().getFlyingSpeed() != spd) { p.getAbilities().setFlyingSpeed(spd); p.onUpdateAbilities(); }
        if (!p.getAbilities().flying) return; // đã hạ cánh: không tốn

        Vec3 v = p.getDeltaMovement();
        double rate = v.horizontalDistanceSqr() < 0.0004 ? s.mode.hoverCost : p.isSprinting() ? s.mode.fastCost : s.mode.cost;
        boolean combat = p.level().getGameTime() - p.getData(com.hukhong.skill.SkillAttachments.STATE).lastCombat <= COMBAT_TICKS;
        if (s.mode == FlightMode.KIEM && com.hukhong.banmenh.BanMenhService.freeFlight(p, combat)) rate = 0; // Ngọc Tâm Lv80
        rate *= moveFactor(p);
        s.acc += rate / 20.0;
        int whole = (int) s.acc;
        if (whole > 0) {
            s.acc -= whole;
            if (d.resource() < Resources.costFor(d, whole)) { stop(p, "hukhong.flight.empty", true); return; }
            Resources.add(p, -Resources.costFor(d, whole));
        }
        if (p.getData(ModAttachments.CULTIVATION).resource() <= 0) stop(p, "hukhong.flight.empty", true);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Long until = GRACE.get(p.getUUID());
        if (STATE.containsKey(p.getUUID()) || (until != null && until >= p.level().getGameTime())) {
            e.setCanceled(true);
            p.fallDistance = 0;
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer viewer) || !(e.getTarget() instanceof ServerPlayer target)) return;
        State s = STATE.get(target.getUUID());
        if (s != null) PacketDistributor.sendToPlayer(viewer, new FlightSyncPayload(target.getId(), s.mode.ordinal(), s.pairs));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        STATE.remove(e.getEntity().getUUID());
        GRACE.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) {
        STATE.clear();
        GRACE.clear();
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        STATE.remove(e.getEntity().getUUID());
    }
}
