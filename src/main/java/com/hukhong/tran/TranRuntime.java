package com.hukhong.tran;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Trạng thái trận đang sống của Trận Tu (chỉ server, không lưu save): vùng Khốn/Hộ Thân/Đại Trận, dấu Phù Văn Đạn,
 * và các hiệu ứng tick (kéo quái, nổ Đại Trận, hồi Trận Lực, tăng sát thương trong trận).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TranRuntime {
    private TranRuntime() {}

    enum Kind { TRAP, WARD, ULT }

    /** {@code who} giữ tham chiếu người chơi chủ trận: FakePlayer không nằm trong PlayerList nên không tra cứu theo UUID được. */
    record Zone(UUID owner, ServerPlayer who, ResourceKey<Level> dim, Vec3 c, double r, long end, Kind kind) {}

    record Mark(UUID owner, long end) {}

    private static final List<Zone> ZONES = new ArrayList<>();
    private static final Map<UUID, Mark> MARKS = new HashMap<>(); // khóa = UUID mục tiêu; mốc thời gian theo tick server

    // ------------------------------------------------------------------ tạo trận / đánh dấu (TranSkills, PassiveService gọi)

    static void addZone(ServerLevel lv, ServerPlayer p, Kind kind, Vec3 c, double r, int ticks) {
        long now = lv.getServer().getTickCount();
        ZONES.add(new Zone(p.getUUID(), p, lv.dimension(), c, r, now + ticks, kind));
    }

    /** Đòn thường Trận Tu: tia phù văn trễ 3 tick + đánh dấu mục tiêu 5 giây. Chỉ gọi từ PassiveService cho đòn cận chiến. */
    public static void basicHit(ServerPlayer atk, LivingEntity target) {
        if (!(atk.level() instanceof ServerLevel lv)) return;
        long now = lv.getServer().getTickCount();
        MARKS.put(target.getUUID(), new Mark(atk.getUUID(), now + TranMath.MARK_TICKS));
        double dmg = TranMath.BASIC_COEF * StatService.spellPower(atk);
        Vec3 from = atk.getEyePosition().subtract(0, 0.25, 0);
        TranVfx.bolt(lv, from, target.position().add(0, target.getBbHeight() * 0.5, 0));
        SkillScheduler.later(TranMath.BASIC_BOLT_DELAY_TICKS, () -> {
            if (!atk.isAlive() || atk.hasDisconnected() || !target.isAlive() || target.isRemoved()) return;
            target.invulnerableTime = 0;
            DamageRules.hurtBySkill(atk, target, (float) dmg);
            TranVfx.rune(lv, target.position().add(0, 0.05, 0), 0.7);
        });
    }

    // ------------------------------------------------------------------ hồi chiêu (SkillService gọi)

    /** Hệ số hồi chiêu: x0,5 nếu người chơi đang đứng trong Đại Trận của chính mình. */
    public static double cooldownMult(ServerPlayer p, Path page) {
        if (page != Path.TRAN) return 1.0;
        for (Zone z : ZONES) {
            if (z.kind() == Kind.ULT && z.owner().equals(p.getUUID()) && z.dim().equals(p.level().dimension())
                    && p.position().distanceTo(z.c()) <= z.r()) return TranMath.cooldownMult(true);
        }
        return TranMath.cooldownMult(false);
    }

    // ------------------------------------------------------------------ sát thương vào/ra (Hộ Thân, Trận Tâm, dấu)

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!ZONES.isEmpty() && e.getEntity() instanceof ServerPlayer victim && inZone(victim, Kind.WARD)) {
            e.setAmount(e.getAmount() * (float) (1 - TranMath.WARD_DR));
        }
        if (!(e.getSource().getEntity() instanceof ServerPlayer atk) || atk == e.getEntity()) return;
        CultivationData d = atk.getData(ModAttachments.CULTIVATION);
        if (!d.paths().contains(Path.TRAN)) return;
        double m = 1;
        if (ownZoneContains(atk)) m *= 1 + TranMath.PASSIVE_DMG;
        if (DamageRules.isSkill() && markedBy(e.getEntity(), atk)) m *= 1 + TranMath.MARK_BONUS;
        if (m != 1) e.setAmount(e.getAmount() * (float) m);
    }

    private static boolean inZone(ServerPlayer p, Kind k) {
        for (Zone z : ZONES) {
            if (z.kind() == k && z.dim().equals(p.level().dimension()) && p.position().distanceTo(z.c()) <= z.r()) return true;
        }
        return false;
    }

    private static boolean ownZoneContains(ServerPlayer p) {
        for (Zone z : ZONES) {
            if (z.owner().equals(p.getUUID()) && z.dim().equals(p.level().dimension()) && p.position().distanceTo(z.c()) <= z.r()) return true;
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
        if (ZONES.isEmpty() && MARKS.isEmpty()) return;
        MinecraftServer srv = e.getServer();
        long now = srv.getTickCount();
        if (!MARKS.isEmpty() && now % 10 == 0) MARKS.values().removeIf(m -> m.end() < now);
        Set<ServerPlayer> regen = new HashSet<>();
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            ServerLevel lv = srv.getLevel(z.dim());
            ServerPlayer owner = z.who();
            if (owner != null && (!owner.isAlive() || owner.hasDisconnected())) owner = null;
            if (lv == null || now >= z.end()) {
                if (z.kind() == Kind.ULT && lv != null && owner != null) detonate(lv, owner, z);
                it.remove();
                continue;
            }
            if (owner == null) continue;
            if (z.kind() == Kind.ULT && now % TranMath.ULT_PULL_INTERVAL_TICKS == 0) pull(lv, owner, z);
            int every = z.kind() == Kind.TRAP ? 10 : 20;
            if (now % every == 0) TranVfx.rune(lv, z.c().add(0, 0.05, 0), z.r());
            if (now % 20 == 0 && owner.position().distanceTo(z.c()) <= z.r()) regen.add(owner);
        }
        for (ServerPlayer p : regen) Resources.add(p, TranMath.REGEN_PER_SEC);
    }

    /** Kéo quái về tâm: hướng tâm x0,25 mỗi 5 tick. Không kéo người chơi; quái đã sát tâm (<1,5 block) thì đứng yên. */
    static int pullCalls = 0;
    private static void pull(ServerLevel lv, ServerPlayer owner, Zone z) {
        pullCalls++;
        for (LivingEntity v : around(lv, owner, z.c(), z.r())) {
            Vec3 d = new Vec3(z.c().x - v.getX(), 0, z.c().z - v.getZ());
            if (d.lengthSqr() < 2.25) continue;
            // Dịch chuyển trực tiếp: setDeltaMovement đơn thuần không làm quái trôi đáng kể (đo trong GameTest daiTranPulls).
            double step = Math.min(TranMath.ULT_PULL * TranMath.ULT_PULL_INTERVAL_TICKS, Math.sqrt(d.lengthSqr()) - 1.5);
            Vec3 move = d.normalize().scale(step);
            v.setPos(v.getX() + move.x, v.getY(), v.getZ() + move.z);
        }
    }

    /** Đại Trận nổ khi hết giờ: 3,0 x Phép lực lên mọi mục tiêu còn trong vùng (đã bị kéo về tâm). */
    private static void detonate(ServerLevel lv, ServerPlayer owner, Zone z) {
        float dmg = (float) (TranMath.ULT_COEF * StatService.spellPower(owner));
        for (LivingEntity v : around(lv, owner, z.c(), z.r())) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(owner, v, dmg);
        }
        TranVfx.rune(lv, z.c().add(0, 0.05, 0), z.r());
        TranVfx.pillar(lv, z.c());
    }

    // ------------------------------------------------------------------ tiện ích dùng chung (TranSkills gọi)

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
        // Không truyền Entity null vào CollisionContext (NPE, xem PhapSkills.ground)
        var r = lv.clip(new ClipContext(v, v.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return r.getType() == HitResult.Type.MISS ? v : r.getLocation();
    }

    /** Số trận đang sống (chỉ để kiểm thử). */
    static int zoneCount() { return ZONES.size(); }
    static int pullCount() { return pullCalls; }

    // ------------------------------------------------------------------ dọn khi tắt server

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        ZONES.clear();
        MARKS.clear();
    }
}
