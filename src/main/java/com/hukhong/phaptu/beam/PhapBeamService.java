package com.hukhong.phaptu.beam;

import com.hukhong.HuKhong;
import com.hukhong.client.anim.CastAnim;
import com.hukhong.client.anim.CastAnimHook;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.pathrules.MaRules;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.phaptu.linhtruong.LinhTruongService;
import com.hukhong.skill.Skill;
import com.hukhong.skill.SkillService;
import com.hukhong.skill.SkillState;
import com.hukhong.skillbook.SkillBookMath;
import com.hukhong.skillbook.SkillBookRuntime;
import com.hukhong.skillbook.SkillBookService;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tinh Hà Quang Trụ (phím T của Pháp Tu Quang/Lôi/Hỏa/Thủy): server là nguồn sự thật của trạng thái, chi phí, sát thương, hồi chiêu.
 * IDLE -> (giữ T 1,5s) CHARGING -> READY (tối đa 5s) -> (giữ chuột phải) FIRING (tối đa 4s) -> IDLE + hồi chiêu.
 * Bộ đếm tick là bộ đếm riêng của trạng thái (mỗi lần {@link #tick} +1) nên test có thể tự "quay" thời gian.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhapBeamService {
    private PhapBeamService() {}

    public enum Phase { IDLE, CHARGING, READY, FIRING }

    static final class St {
        Phase phase = Phase.IDLE;
        int age;                 // tick trong pha hiện tại
        double drainAcc;
        int skillLevel = 1;
        PhapElement element;
        final Map<UUID, Integer> stackAge = new HashMap<>();
        int clock;               // đếm tick tổng, cho ICD tầng
    }

    private static final Map<UUID, St> STATES = new ConcurrentHashMap<>();
    private static final ResourceLocation SLOW_ID = ModAttributes.id("phap/beam_slow");

    public static Phase phase(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s == null ? Phase.IDLE : s.phase;
    }

    /** Đang nạp / sẵn sàng / bắn: các phím kỹ năng khác bị khoá. */
    public static boolean busy(ServerPlayer p) { return phase(p) != Phase.IDLE; }

    /** Người chơi có phím T bị thay bằng tia này không (đúng nguyên tố Pháp Tu). */
    public static boolean locksT(ServerPlayer p) { return PhapBeamMath.locksT(PhapTu.element(p)); }

    private static boolean eligible(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || !locksT(p)) return false;
        SkillState st = SkillService.state(p);
        return st.stance && Skill.skillPath(p.getData(ModAttachments.CULTIVATION), st.page) == Path.PHAP && !MaRules.isLost(p);
    }

    // ------------------------------------------------------------------ vào từ mạng

    public static void action(ServerPlayer p, int action) {
        switch (action) {
            case PhapBeamActionPayload.CHARGE_START -> chargeStart(p);
            case PhapBeamActionPayload.CHARGE_STOP -> chargeStop(p);
            case PhapBeamActionPayload.FIRE_START -> fireStart(p);
            case PhapBeamActionPayload.FIRE_STOP -> fireStop(p);
            default -> { }
        }
    }

    public static boolean chargeStart(ServerPlayer p) {
        if (phase(p) != Phase.IDLE || !eligible(p)) return false;
        SkillState sk = SkillService.state(p);
        if (sk.cooldownEnd[3] > p.level().getGameTime()) return false;
        St s = STATES.computeIfAbsent(p.getUUID(), k -> new St());
        s.phase = Phase.CHARGING;
        s.age = 0;
        s.drainAcc = 0;
        s.stackAge.clear();
        s.element = PhapTu.element(p);
        s.skillLevel = SkillBookService.data(p).level(Skill.PHAP_ULT.icon);
        slow(p, PhapBeamMath.CHARGE_SLOW);
        CastAnimHook.send(p, CastAnim.CHANNEL);
        p.displayClientMessage(Component.translatable("hukhong.phap.beam.charging").withColor(s.element.rgb), true);
        if (p.level() instanceof ServerLevel lv) {
            Vec3 at = origin(p);
            play(lv, at, SoundEvents.BEACON_ACTIVATE, 0.5f, 0.5f);
            play(lv, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 0.6f);
        }
        broadcast(p, s);
        return true;
    }

    /** Nhả T: chỉ huỷ khi còn đang nạp (không tốn Pháp Lực, khoá nhẹ). Đã sẵn sàng thì giữ nguyên. */
    public static void chargeStop(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || s.phase != Phase.CHARGING) return;
        SkillState sk = SkillService.state(p);
        sk.cooldownEnd[3] = Math.max(sk.cooldownEnd[3], p.level().getGameTime() + PhapBeamMath.CANCEL_LOCK_TICKS);
        reset(p, s);
        SkillService.sync(p);
        p.displayClientMessage(Component.translatable("hukhong.phap.beam.cancelled"), true);
    }

    public static boolean fireStart(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || s.phase != Phase.READY || !eligible(p)) return false;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        double f = com.hukhong.banmenh.BanMenhService.skillCostFactor(p);
        int cost = PhapBeamMath.upfrontCost(d.resourceMax(), f);
        if (d.resource() < cost) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
            return false;
        }
        Resources.add(p, -cost);
        s.phase = Phase.FIRING;
        s.age = 0;
        s.drainAcc = 0;
        slow(p, PhapBeamMath.FIRE_SLOW);
        AttributeInstance cc = p.getAttribute(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.CRIT_CHANCE));
        if (cc != null) cc.addOrUpdateTransientModifier(new AttributeModifier(CRIT_ID, PhapBeamMath.FIRE_CRIT_BONUS, AttributeModifier.Operation.ADD_VALUE));
        CastAnimHook.send(p, CastAnim.CHANNEL);
        if (p.level() instanceof ServerLevel lv) {
            Vec3 at = origin(p);
            Fx.shake(lv, p.position(), 0.6f, 10, 14f);
            play(lv, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.7f);
            play(lv, at, SoundEvents.BEACON_POWER_SELECT, 1.4f, 0.5f);
            play(lv, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 0.6f);
            Fx.play(lv, FxType.SHOCKWAVE, at, Vec3.ZERO, 3.5f, s.element.rgb, 14);
        }
        broadcast(p, s);
        return true;
    }

    public static void fireStop(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s != null && s.phase == Phase.FIRING) endFire(p, s);
    }

    /** Huỷ mọi pha (tắt thế, đổi trang, chết, thoát). Đang bắn thì vẫn tính hồi chiêu. */
    public static void cancel(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || s.phase == Phase.IDLE) return;
        if (s.phase == Phase.FIRING) endFire(p, s);
        else { reset(p, s); SkillService.sync(p); }
    }

    // ------------------------------------------------------------------ tick

    /** Gọi mỗi tick của người chơi (PlayerTickEvent.Post; test gọi tay). */
    public static void tick(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || s.phase == Phase.IDLE) return;
        if (!(p.level() instanceof ServerLevel lv)) return;
        if (!eligible(p)) { cancel(p); return; }
        s.clock++;
        s.age++;
        switch (s.phase) {
            case CHARGING -> {
                if (s.age % 20 == 1) CastAnimHook.send(p, CastAnim.CHANNEL);
                if (s.age % 3 == 0) { // tiếng ù dâng cao
                    float k = PhapBeamMath.chargeProgress(s.age);
                    Vec3 at = origin(p);
                    play(lv, at, SoundEvents.BEACON_AMBIENT, 0.5f + 0.5f * k, 0.5f + 1.1f * k);
                    if (s.age % 6 == 0) play(lv, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4f + 0.5f * k, 0.6f + 1.2f * k);
                }
                if (s.age >= PhapBeamMath.CHARGE_TICKS) {
                    s.phase = Phase.READY;
                    s.age = 0;
                    Vec3 at = origin(p);
                    play(lv, at, SoundEvents.BEACON_POWER_SELECT, 1.2f, 1.6f);
                    play(lv, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.9f);
                    p.displayClientMessage(Component.translatable("hukhong.phap.beam.ready").withColor(s.element.rgb), true);
                    broadcast(p, s);
                }
            }
            case READY -> {
                if (s.age % 20 == 1) CastAnimHook.send(p, CastAnim.CHANNEL);
                if (s.age % 24 == 0) play(lv, origin(p), SoundEvents.BEACON_AMBIENT, 0.5f, 1.7f);
                if (s.age >= PhapBeamMath.READY_TICKS) {
                    SkillState sk = SkillService.state(p);
                    sk.cooldownEnd[3] = Math.max(sk.cooldownEnd[3], lv.getGameTime() + PhapBeamMath.EXPIRE_LOCK_TICKS);
                    reset(p, s);
                    SkillService.sync(p);
                    p.displayClientMessage(Component.translatable("hukhong.phap.beam.expired"), true);
                }
            }
            case FIRING -> tickFire(p, lv, s);
            default -> { }
        }
    }

    private static void tickFire(ServerPlayer p, ServerLevel lv, St s) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        double f = com.hukhong.banmenh.BanMenhService.skillCostFactor(p);
        s.drainAcc += PhapBeamMath.drainPerTick(d.resourceMax(), f);
        int take = (int) s.drainAcc;
        if (take > 0) {
            s.drainAcc -= take;
            if (d.resource() < take) { // hết Pháp Lực
                Resources.add(p, -d.resource());
                p.displayClientMessage(Component.translatable("hukhong.phap.beam.out_of_mana"), true);
                endFire(p, s);
                return;
            }
            Resources.add(p, -take);
        }
        Vec3 at = origin(p);
        if (s.age % 3 == 0) {
            play(lv, at, SoundEvents.BEACON_AMBIENT, 1.0f, 0.5f);
            if (s.age % 6 == 0) play(lv, at, SoundEvents.ELDER_GUARDIAN_AMBIENT, 0.5f, 1.8f);
        }
        if (s.age % 5 == 0) Fx.shake(lv, p.position(), 0.22f, 6, 10f);
        if (s.age % PhapBeamMath.PULSE_TICKS == 0) pulse(p, lv, s);
        if (s.age % 8 == 0) impactDecal(p, lv, s);
        if (s.age % 4 == 1) CastAnimHook.send(p, CastAnim.CHANNEL);
        if (s.age >= PhapBeamMath.FIRE_MAX_TICKS) endFire(p, s);
    }

    // ------------------------------------------------------------------ sát thương

    private static Vec3 eye(ServerPlayer p) { return p.getEyePosition().subtract(0, 0.25, 0); }

    /** Gốc nhìn thấy: đầu Linh Trượng nếu cầm, không thì ngang ngực. */
    private static Vec3 origin(ServerPlayer p) {
        Vec3 t = LinhTruongService.tipIfHeld(p);
        return t != null ? t : p.getEyePosition().add(p.getLookAngle().scale(0.7)).subtract(0, 0.3, 0);
    }

    /** Mục tiêu hợp lệ nằm trong ống (origin, dir, RANGE, RADIUS); gần trước. Không đụng người chơi, đồng minh, thú nuôi. */
    static List<LivingEntity> targets(ServerLevel lv, ServerPlayer p, Vec3 origin, Vec3 dir) {
        Vec3 end = origin.add(dir.scale(PhapBeamMath.RANGE));
        AABB box = new AABB(origin, end).inflate(PhapBeamMath.RADIUS + 1);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, box, x -> PhapRuntime.valid(p, x))) {
            Vec3 c = e.getBoundingBox().getCenter().subtract(origin);
            double t = c.dot(dir);
            if (t < 0 || t > PhapBeamMath.RANGE + 0.5) continue;
            if (c.subtract(dir.scale(t)).length() <= PhapBeamMath.RADIUS + e.getBbWidth() / 2) out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
        return out;
    }

    private static void pulse(ServerPlayer p, ServerLevel lv, St s) {
        double dmg = PhapBeamMath.pulseDamage(s.element, PhapTu.aptitude(p), StatService.spellPower(p));
        Vec3 from = eye(p), dir = p.getLookAngle();
        if (s.stackAge.size() > 64) s.stackAge.clear();
        for (LivingEntity t : targets(lv, p, from, dir)) {
            Integer last = s.stackAge.get(t.getUUID());
            boolean stack = last == null || s.clock - last >= PhapBeamMath.STACK_ICD_TICKS;
            if (stack) s.stackAge.put(t.getUUID(), s.clock);
            PhapRuntime.noStack = !stack;
            try {
                t.invulnerableTime = 0;
                SkillBookRuntime.begin(p, SkillBookMath.dmgMult(s.skillLevel), 4);
                DamageRules.hurtBySkill(p, t, (float) dmg);
            } finally {
                PhapRuntime.noStack = false;
                SkillBookRuntime.clear(p);
            }
        }
    }

    private static Vec3 impact(ServerLevel lv, ServerPlayer p) {
        Vec3 from = p.getEyePosition(), to = from.add(p.getLookAngle().scale(PhapBeamMath.RANGE));
        var r = lv.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS ? null : r.getLocation();
    }

    private static void impactDecal(ServerPlayer p, ServerLevel lv, St s) {
        Vec3 hit = impact(lv, p);
        if (hit == null) return;
        int rgb = s.element.rgb;
        Fx.groundCrack(lv, p, hit.add(0, 0.05, 0), 3.2f, rgb, 5, 36);
        if (s.element == PhapElement.HOA) Fx.pool(lv, p, hit.add(0, 0.05, 0), 3.0f, 0xFF4A10, 50);
        else Fx.groundRing(lv, p, hit.add(0, 0.06, 0), 3.6f, rgb, 20);
    }

    // ------------------------------------------------------------------ kết thúc / dọn

    private static void endFire(ServerPlayer p, St s) {
        PhapElement el = s.element;
        long now = p.level().getGameTime();
        SkillState sk = SkillService.state(p);
        int cd = (int) Math.round(SkillBookMath.cooldown(PhapBeamMath.COOLDOWN_TICKS, s.skillLevel)
                * PhapRuntime.cooldownMult(p, Path.PHAP));
        sk.cooldownEnd[3] = now + cd;
        sk.lastCombat = now;
        if (p.level() instanceof ServerLevel lv && el != null) {
            Vec3 at = origin(p);
            Fx.play(lv, FxType.SHOCKWAVE, at, Vec3.ZERO, 5f, el.rgb, 16);
            play(lv, at, SoundEvents.BEACON_DEACTIVATE, 1.3f, 0.6f);
            play(lv, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.5f);
        }
        reset(p, s);
        SkillService.sync(p);
    }

    private static void reset(ServerPlayer p, St s) {
        boolean wasIdle = s.phase == Phase.IDLE;
        s.phase = Phase.IDLE;
        s.age = 0;
        s.drainAcc = 0;
        unslow(p);
        if (!wasIdle) broadcast(p, s);
        s.stackAge.clear();
    }

    private static void slow(ServerPlayer p, double amount) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null) a.addOrUpdateTransientModifier(new AttributeModifier(SLOW_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static final net.minecraft.resources.ResourceLocation CRIT_ID = com.hukhong.stats.ModAttributes.id("phap/beam_crit");

    private static void unslow(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null) a.removeModifier(SLOW_ID);
        AttributeInstance cc = p.getAttribute(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.CRIT_CHANCE));
        if (cc != null) cc.removeModifier(CRIT_ID); // bao kich +35% chi khi dang ban
    }

    private static void play(ServerLevel lv, Vec3 at, SoundEvent s, float vol, float pitch) {
        lv.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, Math.max(0.5f, Math.min(2f, pitch)));
    }

    private static void broadcast(ServerPlayer p, St s) {
        int el = s.element == null ? -1 : s.element.ordinal();
        int ticks = switch (s.phase) {
            case CHARGING -> PhapBeamMath.CHARGE_TICKS;
            case READY -> PhapBeamMath.READY_TICKS;
            case FIRING -> PhapBeamMath.FIRE_MAX_TICKS;
            default -> 0;
        };
        try {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new PhapBeamStatePayload(p.getId(), s.phase.ordinal(), el, ticks));
        } catch (RuntimeException ignored) {
            // người chơi giả (GameTest) không có kết nối
        }
    }

    // ------------------------------------------------------------------ sự kiện

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!STATES.isEmpty() && e.getEntity() instanceof ServerPlayer p) tick(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { cancel(p); STATES.remove(p.getUUID()); unslow(p); }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) cancel(p);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) { STATES.clear(); }
}
