package com.hukhong.dan;

import com.hukhong.HuKhong;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Trạng thái Đan Tu (chỉ server, không lưu save): tầng Đan Độc trên mục tiêu, hồ độc, và cửa sổ Cửu Chuyển Kim Đan.
 * Độc gây sát thương qua DamageRules.hurtBySkill (không dùng MobEffects.POISON vì không scale theo Phép lực).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DanRuntime {
    private DanRuntime() {}

    /** {@code t}/{@code who} giữ tham chiếu: FakePlayer không nằm trong PlayerList nên không tra cứu theo UUID được. */
    private static final class Poison {
        final LivingEntity t;
        ServerPlayer who;
        int stacks;
        long end;
        long nextHit;
        Poison(LivingEntity t) { this.t = t; }
    }

    /** Hồ độc: mỗi nhịp {@link DanMath#POOL_INTERVAL_TICKS} cộng {@link DanMath#POOL_STACKS} tầng cho quái trong bán kính. */
    private static final class Pool {
        final ServerPlayer who;
        final ResourceKey<Level> dim;
        final Vec3 c;
        final long end;
        long next;
        Pool(ServerPlayer who, ResourceKey<Level> dim, Vec3 c, long start) {
            this.who = who; this.dim = dim; this.c = c;
            this.end = start + DanMath.POOL_TICKS;
            this.next = start + DanMath.POOL_INTERVAL_TICKS;
        }
    }

    private static final Map<UUID, Poison> POISON = new HashMap<>();
    private static final List<Pool> POOLS = new ArrayList<>();
    private static final Map<UUID, Long> KIM_UNTIL = new HashMap<>(); // khóa = UUID người chơi; mốc theo tick server

    // ------------------------------------------------------------------ độc

    /** Cộng {@code n} tầng độc (tối đa 10), làm mới 6 giây. */
    public static void addPoison(ServerPlayer who, LivingEntity t, int n) {
        if (!(t.level() instanceof ServerLevel lv) || !t.isAlive()) return;
        long now = lv.getServer().getTickCount();
        Poison po = POISON.computeIfAbsent(t.getUUID(), k -> new Poison(t));
        po.who = who;
        po.stacks = DanMath.addStacks(po.stacks, n);
        po.end = now + DanMath.POISON_TICKS;
        if (po.nextHit < now) po.nextHit = now + DanMath.POISON_INTERVAL_TICKS;
        DanVfx.poisonPuff(lv, t.position(), po.stacks);
    }

    /** Số tầng độc hiện có trên mục tiêu (để kiểm thử và R tiêu hao). */
    public static int stacks(LivingEntity t) {
        Poison po = POISON.get(t.getUUID());
        return po == null ? 0 : po.stacks;
    }

    /** Lấy số tầng rồi xóa độc: dùng cho Đan Hỏa Phần Thiên. */
    static int consumeStacks(LivingEntity t) {
        Poison po = POISON.remove(t.getUUID());
        return po == null ? 0 : po.stacks;
    }

    // ------------------------------------------------------------------ đòn thường (PassiveService gọi)

    /** Đòn thường Đan Tu: viên đan nổ ném tới mục tiêu sau {@link DanMath#BASIC_DELAY_TICKS} tick (+1 tầng); trong Kim Đan thì thành bom. */
    public static void basicHit(ServerPlayer atk, LivingEntity target) {
        if (!(atk.level() instanceof ServerLevel lv)) return;
        boolean bomb = kimActive(atk);
        Vec3 from = atk.getEyePosition().subtract(0, 0.25, 0);
        DanVfx.throwArc(lv, from, target.position().add(0, target.getBbHeight() * 0.5, 0));
        double sp = StatService.spellPower(atk);
        SkillScheduler.later(DanMath.BASIC_DELAY_TICKS, () -> {
            if (!atk.isAlive() || atk.hasDisconnected()) return;
            if (bomb) {
                burst(lv, atk, target.position().add(0, 0.2, 0), DanMath.BOMB_RADIUS, DanMath.ULT_BOMB_COEF * sp, DanMath.BOMB_STACKS);
            } else if (target.isAlive() && !target.isRemoved()) {
                target.invulnerableTime = 0;
                DamageRules.hurtBySkill(atk, target, (float) (DanMath.BASIC_COEF * sp));
                addPoison(atk, target, DanMath.BASIC_STACKS);
                DanVfx.burst(lv, target.position().add(0, 0.2, 0), DanMath.BASIC_RADIUS);
            }
        });
    }

    // ------------------------------------------------------------------ hồ độc / nổ (DanSkills gọi)

    static void addPool(ServerLevel lv, ServerPlayer p, Vec3 c) {
        long now = lv.getServer().getTickCount();
        POOLS.add(new Pool(p, lv.dimension(), c, now));
        DanVfx.pool(lv, c, DanMath.POOL_RADIUS);
    }

    /** Nổ đan trên một điểm: sát thương {@code dmg} + {@code stacks} tầng cho mọi mục tiêu trong bán kính. */
    static void burst(ServerLevel lv, ServerPlayer p, Vec3 at, double r, double dmg, int stacks) {
        DanVfx.burst(lv, at, r);
        for (LivingEntity v : around(lv, p, at, r)) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, (float) dmg);
            addPoison(p, v, stacks);
        }
    }

    // ------------------------------------------------------------------ Cửu Chuyển Kim Đan (T)

    static void startKim(ServerLevel lv, ServerPlayer p) {
        KIM_UNTIL.put(p.getUUID(), lv.getServer().getTickCount() + (long) DanMath.KIM_TICKS);
    }

    public static boolean kimActive(ServerPlayer p) {
        Long end = KIM_UNTIL.get(p.getUUID());
        return end != null && p.getServer() != null && p.getServer().getTickCount() < end;
    }

    /** SkillService gọi: trong Kim Đan mọi kỹ năng không tốn tài nguyên (hồi chiêu vẫn tính). */
    public static boolean freeCast(ServerPlayer p, Path page) {
        return page == Path.DAN && kimActive(p);
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (POISON.isEmpty() && POOLS.isEmpty()) return;
        MinecraftServer srv = e.getServer();
        long now = srv.getTickCount();
        for (Iterator<Pool> it = POOLS.iterator(); it.hasNext(); ) {
            Pool z = it.next();
            ServerLevel lv = srv.getLevel(z.dim);
            if (lv == null || now >= z.end) { it.remove(); continue; }
            while (z.next <= now && z.next <= z.end) {
                z.next += DanMath.POOL_INTERVAL_TICKS;
                if (!z.who.isAlive() || z.who.hasDisconnected()) continue;
                DanVfx.pool(lv, z.c, DanMath.POOL_RADIUS);
                for (LivingEntity v : around(lv, z.who, z.c, DanMath.POOL_RADIUS)) addPoison(z.who, v, DanMath.POOL_STACKS);
            }
        }
        for (Iterator<Poison> it = POISON.values().iterator(); it.hasNext(); ) {
            Poison po = it.next();
            if (!po.t.isAlive() || po.t.isRemoved() || now >= po.end || po.stacks <= 0) { it.remove(); continue; }
            while (po.nextHit <= now) {
                po.nextHit += DanMath.POISON_INTERVAL_TICKS;
                if (!po.who.isAlive() || po.who.hasDisconnected()) continue;
                po.t.invulnerableTime = 0;
                DamageRules.hurtBySkill(po.who, po.t, (float) DanMath.poisonDamage(StatService.spellPower(po.who), po.stacks));
                if (!po.t.isAlive()) { it.remove(); break; }
            }
        }
        if (!KIM_UNTIL.isEmpty() && now % 20 == 0) KIM_UNTIL.values().removeIf(end -> end <= now);
    }

    // ------------------------------------------------------------------ tiện ích (copy từ TranRuntime, xem DRY-debt)

    // DRY-debt: aim/ground/around trùng TranRuntime; gộp vào một chỗ khi có caller thứ 3 (AP9).
    static List<LivingEntity> around(ServerLevel lv, ServerPlayer p, Vec3 c, double r) {
        return lv.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 4, r),
                e -> PhapRuntime.valid(p, e) && e.position().distanceTo(c) <= r + e.getBbWidth() / 2);
    }

    /** Điểm ngắm: mục tiêu đầu tiên trên tia nhìn; không có thì điểm chạm khối; không có nữa thì cuối tia, hạ xuống đất. */
    static Vec3 aim(ServerLevel lv, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(range));
        var hit = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        for (LivingEntity e : around(lv, p, eye.add(look.scale(range / 2)), range / 2 + 1)) {
            if (e.getBoundingBox().inflate(0.3).clip(eye, end).isPresent()) return e.position();
        }
        return ground(lv, hit.getType() == HitResult.Type.MISS ? end : hit.getLocation());
    }

    static Vec3 ground(ServerLevel lv, Vec3 v) {
        // Không truyền Entity null vào CollisionContext (NPE, xem PhapSkills.ground)
        var r = lv.clip(new ClipContext(v, v.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return r.getType() == HitResult.Type.MISS ? v : r.getLocation();
    }

    // ------------------------------------------------------------------ dọn khi tắt server

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        POISON.clear();
        POOLS.clear();
        KIM_UNTIL.clear();
    }
}
