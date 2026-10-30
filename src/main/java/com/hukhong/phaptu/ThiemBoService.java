package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import com.hukhong.pathrules.MaRules;
import com.hukhong.skill.Skill;
import com.hukhong.skill.SkillService;
import com.hukhong.skill.SkillState;
import com.hukhong.skillbook.SkillBook;
import com.hukhong.skillbook.SkillBookService;
import com.hukhong.stats.ModAttributes;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quang E "Thiểm Bộ". Server là nguồn sự thật: giữ E (>= HOLD_TICKS) = chạy tia sáng (tốc độ + bước chân, hao Pháp Lực/s);
 * nhả sớm (bấm nhanh) = đi qua SkillService.cast (chi phí + hồi chiêu như mọi kỹ năng) -> {@link #dash} lùi + Bá Thể.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ThiemBoService {
    private ThiemBoService() {}

    static final class St {
        boolean pressed, sprinting;
        int held;
        double drainAcc;
        Vec3 last;
    }

    private static final Map<UUID, St> STATES = new ConcurrentHashMap<>();
    /** Bá Thể: người chơi -> game time hết miễn thương. */
    private static final Map<UUID, Long> IMMUNE = new ConcurrentHashMap<>();
    private static final ResourceLocation SPEED_ID = ModAttributes.id("phap/thiem_bo_speed");
    private static final ResourceLocation STEP_ID = ModAttributes.id("phap/thiem_bo_step");
    private static final int GOLD = 0xFFE066, VIOLET = 0x9A7BFF, CYAN = 0x8FD8FF;
    static boolean isTho(ServerPlayer p) { return PhapTu.element(p) == PhapElement.THO; }
    static boolean isLoi(ServerPlayer p) { return PhapTu.element(p) == PhapElement.LOI; }
    static int col(ServerPlayer p) { return isLoi(p) ? VIOLET : GOLD; }

    public static void register(PayloadRegistrar r) {
        r.playToServer(ThiemBoPayload.TYPE, ThiemBoPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> action(sp, pl.action()));
        });
    }

    // ------------------------------------------------------------------ điều kiện

    /** Quang, thế bật, trang Pháp Tu, phím E đang là Thiểm Bộ mặc định (không bị gán sách kỹ năng khác). */
    public static boolean eligible(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || MaRules.isLost(p)) return false;
        PhapElement el = PhapTu.element(p);
        if (el != PhapElement.QUANG && el != PhapElement.LOI && el != PhapElement.THO) return false; // Quang: Thiem Bo; Loi: Loi Tran; Tho: bam = thap, giu = xuyen tuong
        SkillState st = SkillService.state(p);
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        Path page = Skill.skillPath(d, st.page);
        if (!st.stance || page != Path.PHAP) return false;
        if (com.hukhong.phaptu.beam.PhapBeamService.busy(p)) return false;
        SkillBook.Resolved r = SkillBook.resolve(SkillBookService.data(p), page, 1, d.level(), ThoService.lockMask(p));
        return r != null && r.base() == Skill.PHAP_2 && r.book() == null;
    }

    /** Còn modifier tốc độ Thiểm Bộ trên người chơi không (test). */
    public static boolean hasSpeedModifier(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        return a != null && a.getModifier(SPEED_ID) != null;
    }

    public static boolean sprinting(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.sprinting;
    }

    // ------------------------------------------------------------------ mạng

    public static void action(ServerPlayer p, int action) {
        if (action == ThiemBoPayload.PRESS) {
            St s = STATES.computeIfAbsent(p.getUUID(), k -> new St());
            s.pressed = true;
            s.held = 0;
        } else if (action == ThiemBoPayload.RELEASE) {
            release(p);
        } else if (action == ThiemBoPayload.T_PRESS || action == ThiemBoPayload.T_RELEASE) {
            if (ThoService.isTho(p)) ThoService.tKey(p, action == ThiemBoPayload.T_PRESS); // Thổ: giữ T dẫn nổ Thiên Sơn
        }
    }

    /** Nhả E: đang chạy -> dừng; chưa đủ HOLD_TICKS -> bấm nhanh (cast qua SkillService). */
    public static void release(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || !s.pressed) return;
        s.pressed = false;
        if (s.sprinting) { stopSprint(p, s, null); return; }
        if (!ThiemBoMath.isHold(s.held) && eligible(p)) SkillService.cast(p, 1);
    }

    /** Dừng mọi trạng thái (tắt thế, đổi trang, chết, thoát). */
    public static void cancel(ServerPlayer p) {
        ThoService.cancelPhase(p);
        St s = STATES.get(p.getUUID());
        if (s == null) return;
        s.pressed = false;
        if (s.sprinting) stopSprint(p, s, null);
    }

    // ------------------------------------------------------------------ giữ E: chạy tia sáng

    /** Mỗi tick người chơi (PlayerTickEvent.Post; test gọi tay). */
    public static void tick(ServerPlayer p) {
        long now = p.level().getGameTime();
        Long end = IMMUNE.get(p.getUUID());
        if (end != null) {
            if (now >= end) { IMMUNE.remove(p.getUUID()); p.displayClientMessage(Component.translatable("hukhong.phap.thiem_bo.immune_end"), true); }
        }
        St s = STATES.get(p.getUUID());
        if (s == null || !(p.level() instanceof ServerLevel lv)) return;
        if (s.sprinting && (!eligible(p) || !s.pressed)) { stopSprint(p, s, null); s.pressed = false; return; }
        if (!s.pressed) return;
        s.held++;
        if (!s.sprinting) {
            if (s.held != ThiemBoMath.HOLD_TICKS || !eligible(p)) return;
            if (isTho(p)) { ThoService.startPhase(p); return; } // Thổ: giữ E = xuyên tường 5s (không chạy tia sáng)
            CultivationData d0 = p.getData(ModAttachments.CULTIVATION);
            if (d0.resource() < ThiemBoMath.minStartMana(d0.resourceMax())) {
                p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
                return;
            }
            startSprint(p, s, lv);
        }
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        s.drainAcc += ThiemBoMath.drainPerTick(d.resourceMax(), com.hukhong.banmenh.BanMenhService.skillCostFactor(p));
        int take = (int) s.drainAcc;
        if (take > 0) {
            s.drainAcc -= take;
            if (d.resource() < take) {
                Resources.add(p, -d.resource());
                s.pressed = false;
                stopSprint(p, s, Component.translatable("hukhong.phap.thiem_bo.out_of_mana"));
                return;
            }
            Resources.add(p, -take);
        }
        p.setSprinting(true);
        p.fallDistance = 0;
        trail(p, lv, s);
    }

    private static void startSprint(ServerPlayer p, St s, ServerLevel lv) {
        s.sprinting = true;
        s.drainAcc = 0;
        s.last = p.position();
        AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, ThiemBoMath.SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        AttributeInstance st = p.getAttribute(Attributes.STEP_HEIGHT);
        if (st != null) st.addOrUpdateTransientModifier(new AttributeModifier(STEP_ID, ThiemBoMath.STEP_BONUS, AttributeModifier.Operation.ADD_VALUE));
        p.setSprinting(true);
        p.displayClientMessage(isLoi(p) ? Component.translatable("hukhong.phap.loi_tran.run").withColor(VIOLET)
                : Component.translatable("hukhong.phap.thiem_bo.run").withColor(GOLD), true);
        if (isLoi(p)) Fx.shell(lv, p, p, 1.15f, VIOLET, 14);
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.25f, 2.0f);
    }

    private static void stopSprint(ServerPlayer p, St s, Component msg) {
        s.sprinting = false;
        s.last = null;
        AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.removeModifier(SPEED_ID);
        AttributeInstance st = p.getAttribute(Attributes.STEP_HEIGHT);
        if (st != null) st.removeModifier(STEP_ID);
        p.setSprinting(false);
        if (msg != null) p.displayClientMessage(msg, true);
    }

    private static void trail(ServerPlayer p, ServerLevel lv, St s) {
        Vec3 cur = p.position();
        Vec3 prev = s.last == null ? cur : s.last;
        s.last = cur;
        if (prev.distanceToSqr(cur) < 0.01) return;
        Vec3 a = prev.add(0, 0.9, 0), b = cur.add(0, 0.9, 0);
        boolean loi = isLoi(p);
        Fx.ribbon(lv, p, a, b, 0, 8, 0, 0.28f, loi ? VIOLET : GOLD);
        Fx.ribbon(lv, p, a.add(0, 0.5, 0), b.add(0, 0.5, 0), 0, 6, 0, 0.1f, loi ? CYAN : 0xFFFFFF);
        if (loi && s.held % ThiemBoMath.LOI_ZAP_TICKS == 0) loiZap(p, lv);
        var rnd = p.getRandom();
        for (int i = 0; i < 2; i++) {
            Vec3 q = prev.lerp(cur, rnd.nextDouble()).add((rnd.nextDouble() - 0.5) * 0.5, 0.2 + rnd.nextDouble() * 1.4, (rnd.nextDouble() - 0.5) * 0.5);
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        if (s.held % 6 == 0) lv.playSound(null, cur.x, cur.y, cur.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.35f, 1.9f);
        if (s.held % 14 == 0) lv.playSound(null, cur.x, cur.y, cur.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.9f);
    }

    /** Loi Tran: tia set nho tu than nguoi danh ke dich gan nhat (ban kinh LOI_ZAP_RANGE). */
    private static void loiZap(ServerPlayer p, ServerLevel lv) {
        double r = ThiemBoMath.LOI_ZAP_RANGE;
        var list = lv.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && PhapRuntime.valid(p, e));
        net.minecraft.world.entity.LivingEntity t = null;
        for (var e : list) if (t == null || e.distanceToSqr(p) < t.distanceToSqr(p)) t = e;
        if (t == null) return;
        float dmg = (float) ThiemBoMath.loiZapDamage(com.hukhong.stats.StatService.spellPower(p));
        com.hukhong.skillbook.SkillBookRuntime.hurt(p, t, dmg);
        Vec3 a = p.position().add(0, 1.1, 0), b = t.position().add(0, t.getBbHeight() * 0.6, 0);
        Fx.ribbon(lv, p, a, b, 0, 4, 0, 0.08f, CYAN);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 8, 0.2, 0.3, 0.2, 0.1);
        lv.playSound(null, b.x, b.y, b.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.35f, 1.8f);
    }

    // ------------------------------------------------------------------ bấm nhanh: lùi + Bá Thể

    /** Gọi từ PhapSkills.quang slot 1 (sau khi SkillService đã trừ Pháp Lực; hồi chiêu đặt ngay sau). */
    public static void dash(ServerLevel lv, ServerPlayer p) {
        Vec3 l = p.getLookAngle();
        Vec3 back = new Vec3(-l.x, 0, -l.z);
        back = back.lengthSqr() < 1e-4 ? new Vec3(0, 0, -1) : back.normalize();
        double clear = ThiemBoMath.DASH_DIST + 0.5;
        for (double h : new double[]{0.3, 1.0, 1.7}) { // chân, thân, đầu
            Vec3 o = p.position().add(0, h, 0);
            var r = lv.clip(new ClipContext(o, o.add(back.scale(ThiemBoMath.DASH_DIST + 0.5)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (r.getType() != HitResult.Type.MISS) clear = Math.min(clear, r.getLocation().distanceTo(o));
        }
        double dist = ThiemBoMath.safeDist(clear);
        double v = ThiemBoMath.impulse(dist);
        Vec3 from = p.position().add(0, 0.9, 0);
        p.setDeltaMovement(back.x * v, 0.1, back.z * v);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Vec3 to = from.add(back.scale(dist));
        Fx.ribbon(lv, p, from, to, 0, 6, 0, 0.3f, GOLD);
        Fx.ribbon(lv, p, from.add(0, 0.5, 0), to.add(0, 0.5, 0), 0, 5, 0, 0.12f, 0xFFFFFF);
        lv.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 10, 0.3, 0.5, 0.3, 0.08);
        lv.playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.7f);
        grantImmunity(p);
    }

    public static void grantImmunity(ServerPlayer p) {
        IMMUNE.put(p.getUUID(), p.level().getGameTime() + ThiemBoMath.IMMUNE_TICKS);
        if (p.level() instanceof ServerLevel lv) {
            Fx.shell(lv, p, p, 1.3f, GOLD, ThiemBoMath.IMMUNE_TICKS);
            lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
        p.displayClientMessage(Component.translatable("hukhong.phap.thiem_bo.immune").withColor(GOLD), true);
    }

    public static boolean immune(ServerPlayer p) { return immune(p, p.level().getGameTime()); }

    public static boolean immune(ServerPlayer p, long gameTime) {
        Long end = IMMUNE.get(p.getUUID());
        return end != null && gameTime < end;
    }

    /** Hồi chiêu riêng của Quang E (tick), hoặc -1 nếu không phải. Đã gồm hệ số hồi chiêu nguyên tố. */
    public static int cooldownOverride(ServerPlayer p, Path page, int slot) {
        if (page != Path.PHAP || slot != 1 || PhapTu.element(p) != PhapElement.QUANG) return -1;
        return (int) Math.round(ThiemBoMath.COOLDOWN * PhapRuntime.cooldownMult(p, page));
    }

    // ------------------------------------------------------------------ sự kiện

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!immune(p)) {
            if (sprinting(p) && isLoi(p)) e.setAmount((float) (e.getAmount() * (1 - ThiemBoMath.LOI_DR))); // Loi Tran cuong hoa than the
            return;
        }
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // hư không / lệnh /kill
        e.setCanceled(true);
        if (p.level() instanceof ServerLevel lv)
            lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 4, 0.4, 0.5, 0.4, 0.05);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) tick(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { cancel(p); STATES.remove(p.getUUID()); IMMUNE.remove(p.getUUID()); }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { cancel(p); IMMUNE.remove(p.getUUID()); }
    }
}
