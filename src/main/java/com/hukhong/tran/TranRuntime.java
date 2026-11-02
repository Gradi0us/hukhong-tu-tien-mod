package com.hukhong.tran;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.network.TranSyncPayload;
import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Trạng thái Trận Tu đang sống (chỉ server, không lưu save): thế hiện tại của từng người chơi, Kiệt Lực/Kiệt Sức,
 * các vùng trận (Khốn, Hộ Thân, Giam Cầm, Khốn Linh, Trấn Áp, Hồi Linh) và dấu Phù Văn Đạn.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TranRuntime {
    private TranRuntime() {}

    enum Kind { TRAP, WARD, CAGE, SNARE, SUPPRESS, HEAL }

    /** Vùng trận. {@code who} giữ tham chiếu người chơi chủ trận (FakePlayer không nằm trong PlayerList). */
    static final class Zone {
        final UUID owner;
        final ServerPlayer who;
        final ResourceKey<Level> dim;
        final Vec3 c;
        final double r;
        final Kind kind;
        long end;
        /** CAGE: quái đang bị nhốt; SUPPRESS: quái đã bị choáng khi vào. */
        final Set<UUID> held = new HashSet<>();
        int hits, hitLimit;

        Zone(UUID owner, ServerPlayer who, ResourceKey<Level> dim, Vec3 c, double r, long end, Kind kind) {
            this.owner = owner; this.who = who; this.dim = dim; this.c = c; this.r = r; this.end = end; this.kind = kind;
        }
    }

    /** Trạng thái thế của một người chơi. */
    static final class St {
        final ServerPlayer who;
        TranStance stance = TranStance.BASE;
        long armedUntil = -1, tCdEnd = -1;
        int stacks;
        long decayAt;
        long exhaustEnd = -1;
        St(ServerPlayer who) { this.who = who; }
    }

    record Mark(UUID owner, long end) {}

    private static final List<Zone> ZONES = new ArrayList<>();
    private static final Map<UUID, St> STATES = new HashMap<>();
    private static final Map<UUID, Mark> MARKS = new HashMap<>();

    static long now(ServerPlayer p) { return p.level().getGameTime(); }

    static St st(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new St(p)); }

    public static TranStance stance(ServerPlayer p) { return st(p).stance; }

    /** Đang bị Kiệt Sức: không hồi Trận Lực. */
    public static boolean exhausted(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.exhaustEnd > now(p);
    }

    // ------------------------------------------------------------------ thế (T / chọn thế)

    /**
     * Phím T và phím Q/E/R khi đang chọn thế. Trả true nếu phím đã được xử lý ở đây (không cast kỹ năng).
     * T: đang ở thế -> về BASE (hồi 1 giây); ở BASE -> bật chọn thế 3 giây; bấm lại khi đang chọn -> huỷ.
     */
    public static boolean onKey(ServerPlayer p, int slot) {
        St s = st(p);
        long now = now(p);
        if (slot == 3) {
            if (s.stance != TranStance.BASE) {
                s.stance = TranStance.BASE;
                s.tCdEnd = now + TranMath.T_RETURN_CD_TICKS;
                s.armedUntil = -1;
                p.displayClientMessage(Component.translatable("hukhong.tran.return_base"), true);
            } else if (s.armedUntil >= now) {
                s.armedUntil = -1;
            } else if (now >= s.tCdEnd) {
                s.armedUntil = now + TranMath.ARM_TICKS;
                p.displayClientMessage(Component.translatable("hukhong.tran.arm"), true);
            }
            sendSync(p);
            return true;
        }
        if (s.stance == TranStance.BASE && s.armedUntil >= now) {
            s.stance = TranStance.fromSlot(slot);
            s.armedUntil = -1;
            p.displayClientMessage(Component.translatable("hukhong.tran.entered", Component.translatable(s.stance.langKey())), true);
            TranVfx.stanceEnter(lvOf(p), p, s.stance.ordinal());
            sendSync(p);
            return true;
        }
        return false;
    }

    private static ServerLevel lvOf(ServerPlayer p) { return (ServerLevel) p.level(); }

    /** Kiệt Lực: mỗi đòn trong thế Sát Thương +1; đủ 5 -> Kiệt Sức 5 giây. */
    static void addKietLuc(ServerPlayer p) {
        St s = st(p);
        long now = now(p);
        if (s.exhaustEnd > now) return;
        if (s.stacks == 0) s.decayAt = now + TranMath.KL_DECAY_TICKS;
        s.stacks++;
        if (s.stacks >= TranMath.KL_MAX) {
            s.stacks = 0;
            s.exhaustEnd = now + TranMath.KL_EXHAUST_TICKS;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TranMath.KL_EXHAUST_TICKS, TranMath.KL_SLOW_AMP, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, TranMath.KL_EXHAUST_TICKS, TranMath.KL_WEAK_AMP, false, true));
            p.displayClientMessage(Component.translatable("hukhong.tran.exhaust"), true);
            TranVfx.exhaust(lvOf(p), p);
        } else {
            p.displayClientMessage(Component.translatable("hukhong.tran.kiet_luc", s.stacks, TranMath.KL_MAX), true);
        }
        sendSync(p);
    }

    /** Đồng bộ thế/Kiệt Lực/Kiệt Sức/chọn thế sang HUD client. */
    public static void sendSync(ServerPlayer p) {
        St s = st(p);
        long now = now(p);
        int armed = (int) Math.max(0, s.armedUntil - now);
        int exhaust = (int) Math.max(0, s.exhaustEnd - now);
        PacketDistributor.sendToPlayer(p, new TranSyncPayload(s.stance.ordinal(), armed, s.stacks, exhaust));
    }

    // ------------------------------------------------------------------ tạo trận (TranSkills gọi)

    static Zone addZone(ServerLevel lv, ServerPlayer p, Kind kind, Vec3 c, double r, int ticks) {
        long now = lv.getServer().getTickCount();
        Zone z = new Zone(p.getUUID(), p, lv.dimension(), c, r, now + ticks, kind);
        ZONES.add(z);
        TranVfx.zoneDecal(lv, p, c, r, ticks);
        return z;
    }

    static Zone cage(ServerLevel lv, ServerPlayer p, Vec3 c) {
        Zone z = addZone(lv, p, Kind.CAGE, c, TranMath.CAGE_RADIUS, TranMath.CAGE_TICKS);
        for (LivingEntity v : around(lv, p, c, z.r)) z.held.add(v.getUUID());
        z.hitLimit = TranMath.cageHitLimit(z.held.size());
        return z;
    }

    static void snare(ServerLevel lv, ServerPlayer p, Vec3 c) { addZone(lv, p, Kind.SNARE, c, TranMath.SNARE_RADIUS, TranMath.SNARE_TICKS); }

    static void suppress(ServerLevel lv, ServerPlayer p, Vec3 c) { addZone(lv, p, Kind.SUPPRESS, c, TranMath.SUP_RADIUS, TranMath.SUP_TICKS); }

    static void ward(ServerLevel lv, ServerPlayer p, Vec3 c, double r) { addZone(lv, p, Kind.WARD, c, r, TranMath.WARD_TICKS); }

    static void heal(ServerLevel lv, ServerPlayer p, Vec3 c) { addZone(lv, p, Kind.HEAL, c, TranMath.HEAL_RADIUS, TranMath.HEAL_TICKS); }

    /** Đòn thường Trận Tu: tia phù văn trễ 3 tick + đánh dấu mục tiêu 5 giây. */
    public static void basicHit(ServerPlayer atk, LivingEntity target) {
        if (!(atk.level() instanceof ServerLevel lv)) return;
        long now = lv.getServer().getTickCount();
        MARKS.put(target.getUUID(), new Mark(atk.getUUID(), now + TranMath.MARK_TICKS));
        double dmg = TranMath.BASIC_COEF * StatService.spellPower(atk);
        lv.playSound(null, atk.getX(), atk.getY(), atk.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.7f, 1.3f);
        Vec3 from = atk.getEyePosition().subtract(0, 0.25, 0);
        TranVfx.bolt(lv, from, target.position().add(0, target.getBbHeight() * 0.5, 0));
        SkillScheduler.later(TranMath.BASIC_BOLT_DELAY_TICKS, () -> {
            if (!atk.isAlive() || atk.hasDisconnected() || !target.isAlive() || target.isRemoved()) return;
            target.invulnerableTime = 0;
            DamageRules.hurtBySkill(atk, target, (float) dmg);
            TranVfx.rune(lv, target.position().add(0, 0.05, 0), 0.7);
        });
    }

    // ------------------------------------------------------------------ sát thương vào/ra

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!ZONES.isEmpty()) {
            if (e.getEntity() instanceof ServerPlayer victim && inZone(victim.position(), victim.level(), Kind.WARD)) {
                e.setAmount(e.getAmount() * (float) (1 - TranMath.WARD_DR));
            }
            if (e.getEntity() instanceof ServerPlayer && e.getSource().getEntity() instanceof Mob m
                    && inZone(m.position(), m.level(), Kind.SUPPRESS)) {
                e.setAmount(e.getAmount() * (float) (1 - TranMath.SUP_DR));
            }
        }
        if (!(e.getSource().getEntity() instanceof ServerPlayer atk) || atk == e.getEntity()) return;
        CultivationData d = atk.getData(ModAttachments.CULTIVATION);
        if (!d.paths().contains(Path.TRAN)) return;
        double m = 1;
        if (ownZoneContains(atk)) m *= 1 + TranMath.PASSIVE_DMG;
        if (DamageRules.isSkill() && markedBy(e.getEntity(), atk)) m *= 1 + TranMath.MARK_BONUS;
        if (m != 1) e.setAmount(e.getAmount() * (float) m);
    }

    private static boolean inZone(Vec3 pos, Level lv, Kind k) {
        for (Zone z : ZONES) {
            if (z.kind == k && z.dim.equals(lv.dimension()) && pos.distanceTo(z.c) <= z.r) return true;
        }
        return false;
    }

    private static boolean ownZoneContains(ServerPlayer p) {
        for (Zone z : ZONES) {
            if (z.owner.equals(p.getUUID()) && z.dim.equals(p.level().dimension()) && p.position().distanceTo(z.c) <= z.r) return true;
        }
        return false;
    }

    private static boolean markedBy(LivingEntity t, ServerPlayer atk) {
        Mark m = MARKS.get(t.getUUID());
        if (m == null || !m.owner().equals(atk.getUUID()) || !(t.level() instanceof ServerLevel sl)) return false;
        return sl.getServer().getTickCount() <= m.end();
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer srv = e.getServer();
        long now = srv.getTickCount();
        if (!MARKS.isEmpty() && now % 10 == 0) MARKS.values().removeIf(m -> m.end() < now);
        if (!STATES.isEmpty()) tickStates(now);
        if (ZONES.isEmpty()) return;
        Set<ServerPlayer> regen = new HashSet<>();
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            ServerLevel lv = srv.getLevel(z.dim);
            ServerPlayer owner = z.who;
            if (owner != null && (!owner.isAlive() || owner.hasDisconnected())) owner = null;
            if (lv == null || now >= z.end) {
                if (z.kind == Kind.CAGE && lv != null) TranVfx.cageBreak(lv, z.c, z.r);
                it.remove();
                continue;
            }
            if (owner == null) continue;
            switch (z.kind) {
                case CAGE -> tickCage(lv, owner, z, now);
                case SNARE -> { if (now % TranMath.SNARE_TICK_INTERVAL == 0) tickSnare(lv, owner, z); }
                case SUPPRESS -> { if (now % TranMath.SUP_CHECK_INTERVAL == 0) tickSuppress(lv, owner, z); }
                case HEAL -> { if (now % TranMath.HEAL_TICK_INTERVAL == 0) tickHeal(lv, owner, z); }
                default -> { }
            }
            if (now % 10 == 0) drawZone(lv, z);
            if (z.kind == Kind.TRAP && now % 5 == 0) TranVfx.rune(lv, z.c.add(0, 0.05, 0), z.r);
            if (now % 20 == 0 && z.kind == Kind.WARD && owner.position().distanceTo(z.c) <= z.r) regen.add(owner);
        }
        for (ServerPlayer p : regen) if (!exhausted(p)) Resources.add(p, TranMath.REGEN_PER_SEC);
    }

    private static void drawZone(ServerLevel lv, Zone z) {
        switch (z.kind) {
            case TRAP -> { }
            case WARD -> TranVfx.ward(lv, z.c, z.r);
            case CAGE -> TranVfx.cage(lv, z.c, z.r, (float) z.hits / Math.max(1, z.hitLimit));
            case SNARE -> TranVfx.slowField(lv, z.c, z.r);
            case SUPPRESS -> TranVfx.suppress(lv, z.c, z.r);
            case HEAL -> TranVfx.healField(lv, z.c, z.r);
        }
    }

    /** Lồng: quái trong lồng không ra được (đẩy về trong bán kính), mỗi giây mỗi quái đập 1 lần; đủ số đập thì vỡ. */
    private static void tickCage(ServerLevel lv, ServerPlayer owner, Zone z, long now) {
        for (Iterator<UUID> it = z.held.iterator(); it.hasNext(); ) {
            Entity en = lv.getEntity(it.next());
            if (!(en instanceof LivingEntity v) || !v.isAlive()) { it.remove(); continue; }
            double dx = v.getX() - z.c.x, dz = v.getZ() - z.c.z;
            double d = Math.sqrt(dx * dx + dz * dz), lim = z.r - TranMath.CAGE_LEASH;
            if (d > lim) {
                double k = d < 1e-6 ? 0 : lim / d;
                v.setPos(z.c.x + dx * k, v.getY(), z.c.z + dz * k);
            }
        }
        for (LivingEntity v : around(lv, owner, z.c, z.r)) z.held.add(v.getUUID());
        if (now % TranMath.CAGE_HIT_INTERVAL_TICKS == 0 && !z.held.isEmpty()) {
            z.hits += z.held.size();
            for (UUID id : z.held) {
                if (lv.getEntity(id) instanceof LivingEntity v) TranVfx.cageCrack(lv, v.position().add(0, v.getBbHeight() * 0.5, 0));
            }
            lv.playSound(null, z.c.x, z.c.y, z.c.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 0.6f);
            if (z.hits >= z.hitLimit) {
                z.end = now; // vỡ: xoá ở vòng tick sau
                z.held.clear();
            }
        }
    }

    private static void tickSnare(ServerLevel lv, ServerPlayer owner, Zone z) {
        float dmg = (float) (TranMath.SNARE_DOT_COEF * StatService.spellPower(owner));
        for (LivingEntity v : around(lv, owner, z.c, z.r)) {
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TranMath.SNARE_TICK_INTERVAL + 10, TranMath.SNARE_SLOW_AMP, false, true));
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(owner, v, dmg);
        }
    }

    /** Trấn Áp: quái vừa bước vào vùng bị choáng 1 giây (mỗi quái một lần cho mỗi trận). */
    private static void tickSuppress(ServerLevel lv, ServerPlayer owner, Zone z) {
        for (LivingEntity v : around(lv, owner, z.c, z.r)) {
            if (z.held.add(v.getUUID())) PhapRuntime.stun(v, TranMath.SUP_STUN_SEC);
        }
    }

    /** Người chơi trong vùng (kể cả chủ trận: FakePlayer không nằm trong lv.players()). */
    private static List<ServerPlayer> playersNear(ServerLevel lv, ServerPlayer owner, Vec3 c, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer q : lv.players()) if (q.isAlive() && q.position().distanceTo(c) <= r) out.add(q);
        if (owner != null && owner.isAlive() && owner.level() == lv && owner.position().distanceTo(c) <= r && !out.contains(owner)) out.add(owner);
        return out;
    }

    private static void tickHeal(ServerLevel lv, ServerPlayer owner, Zone z) {
        for (ServerPlayer q : playersNear(lv, owner, z.c, z.r)) {
            q.heal((float) (q.getMaxHealth() * TranMath.HEAL_PCT));
            Resources.add(q, TranMath.HEAL_LUC_PER_SEC);
        }
    }

    /** Kiệt Lực giảm 1 mỗi 4 giây; đồng bộ HUD định kỳ. */
    private static void tickStates(long now) {
        for (St s : STATES.values()) {
            if (s.stacks > 0 && now >= s.decayAt) {
                s.stacks--;
                s.decayAt = now + TranMath.KL_DECAY_TICKS;
            }
        }
        if (now % 10 == 0) {
            for (St s : STATES.values()) {
                if (s.who != null && !s.who.hasDisconnected()) sendSync(s.who);
            }
        }
    }

    /** Kim Cang Trận: hấp thụ 20% máu tối đa cho người chơi trong r6 (kể cả người dùng), 8 giây rồi xoá phần còn lại. */
    static void shield(ServerLevel lv, ServerPlayer caster, Vec3 c) {
        List<ServerPlayer> recipients = playersNear(lv, caster, c, TranMath.SHIELD_RADIUS);
        for (ServerPlayer q : recipients) {
            float amount = (float) (q.getMaxHealth() * TranMath.SHIELD_PCT);
            q.setAbsorptionAmount(Math.max(q.getAbsorptionAmount(), amount));
            TranVfx.shield(lv, q);
            SkillScheduler.later(TranMath.SHIELD_TICKS, () -> {
                if (q.isAlive() && q.getAbsorptionAmount() <= amount + 0.01f) q.setAbsorptionAmount(0);
            });
        }
    }

    // ------------------------------------------------------------------ tiện ích dùng chung

    static List<LivingEntity> around(ServerLevel lv, ServerPlayer p, Vec3 c, double r) {
        return lv.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 4, r),
                e -> PhapRuntime.valid(p, e) && e.position().distanceTo(c) <= r + e.getBbWidth() / 2);
    }

    /** Điểm ngắm: mục tiêu đầu tiên trên tia nhìn; không có thì điểm chạm khối; không có nữa thì cuối tia, hạ xuống đất. */
    static Vec3 aim(ServerLevel lv, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(range));
        EntityHitResult ehr = ProjectileUtil.getEntityHitResult(lv, p, eye, end, new AABB(eye, end).inflate(1),
                e -> e instanceof LivingEntity le && PhapRuntime.valid(p, le));
        if (ehr != null) return ehr.getEntity().position();
        BlockHitResult r = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return ground(lv, r.getType() == HitResult.Type.MISS ? end : r.getLocation());
    }

    static Vec3 ground(ServerLevel lv, Vec3 v) {
        var r = lv.clip(new ClipContext(v, v.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return r.getType() == HitResult.Type.MISS ? v : r.getLocation();
    }

    /** Số vùng đang sống của loại k (kiểm thử). */
    static int count(Kind k) {
        int n = 0;
        for (Zone z : ZONES) if (z.kind == k) n++;
        return n;
    }

    /** Số vùng loại k do chính người chơi p lập (kiểm thử chạy song song trong cùng batch vẫn đúng). */
    static int countOwned(Kind k, ServerPlayer p) {
        int n = 0;
        for (Zone z : ZONES) if (z.kind == k && z.owner.equals(p.getUUID())) n++;
        return n;
    }

    static Zone ownedZone(Kind k, ServerPlayer p) {
        for (Zone z : ZONES) if (z.kind == k && z.owner.equals(p.getUUID())) return z;
        return null;
    }

    static Zone zoneOf(Kind k) {
        for (Zone z : ZONES) if (z.kind == k) return z;
        return null;
    }

    // ------------------------------------------------------------------ dọn

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) STATES.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        ZONES.clear();
        MARKS.clear();
        STATES.clear();
    }
}
