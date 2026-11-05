package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thổ Tháp cấp 3 - Thái Sơn Thần Tháp.
 * E khi có trụ Hợp Thể: hợp mọi trụ thành MỘT trụ cấp 3. Tự động: trừ 1/2 Pháp Lực, ngừng hồi; sống 20s; tự bắn đạn nổ lớn + chậm nhẹ.
 * Trong 5s đầu, đứng cách trụ <= 4 ô nhấn E: hợp thể (3 phút 1 lần, mỗi trụ 1 lần). Trụ sống 60s, vẫn tự bắn, Q/R/T thành kỹ năng trụ:
 * Q cường hoá bắn (3 phát liên tục, choáng, vùng chậm 3s), R gai đẩy lùi có máu riêng, T Tia Diệt Thế (1 lần).
 * Đang hợp thể giữ E: tự huỷ 7s (hút quái 15 ô, kéo cứng trong 4 ô) rồi nổ cực mạnh. Trụ vỡ khi đang nhập: xuyên tường + bất tử 3s.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ThoTitan {
    private ThoTitan() {}

    static final int COL = PhapElement.THO.rgb, FIRE = 0xFF8A2A, GOLD = 0xFFD27A, LAVA = 0xFF5A14;
    private static final BlockParticleOption STONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE_BRICKS.defaultBlockState());

    public static final int AUTO_LIFE = 400, MERGED_LIFE = 1200, ENTER_WINDOW = 100, MERGE_CD = 3600;
    public static final double ENTER_RANGE = 4.0;
    public static final int AUTO_TICKS = 14, Q_TICKS = 120, Q_CD = 240, Q_VOLLEY = 8, R_CD = 300, SPIKE_LIFE = 200, T_TICKS = 100, DESTRUCT_TICKS = 140;
    /** Sát thương = hệ số x một phát trụ thường. */
    public static final double AUTO_MULT = 3.5, AUTO_R = 3.5, Q_MULT = 3.0, Q_R = 3.0, SPIKE_MULT = 1.5, T_MULT = 4.0, NUKE_MULT = 60, NUKE_R = 14;
    public static final double PULL_R = 15, HARD_PULL_R = 4;
    public static final double SEAT_Y = 6.9, CORE_Y = 7.8;

    static final class Zone { Vec3 pos; long until; }

    static final class St {
        UUID titan;
        PhapTurret ref;
        boolean control, entered, windowMsg, tUsed;
        long createdAt, qEnd, qCdEnd, rCdEnd;
        int laserLeft, destructLeft;
        final List<Zone> zones = new ArrayList<>();
    }

    private static final Map<UUID, St> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MERGE = new ConcurrentHashMap<>();

    public static PhapTurret titan(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || s.titan == null) return null;
        PhapTurret t = p.level() instanceof ServerLevel sl && sl.getEntity(s.titan) instanceof PhapTurret x ? x : s.ref;
        return t != null && t.isAlive() && t.isTitan() ? t : null;
    }

    public static boolean controlling(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.control && titan(p) != null;
    }

    /** Trụ cấp 3 ở chế độ tự động (chặn hồi Pháp Lực). */
    public static boolean autoActive(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && !s.control && titan(p) != null;
    }

    public static boolean selfDestructing(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.destructLeft > 0;
    }

    public static boolean laserActive(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.laserLeft > 0;
    }

    private static void msg(ServerPlayer p, String key, int col, Object... args) {
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan." + key, args).withColor(col), true);
    }

    private static long now(ServerPlayer p) { return p.level().getGameTime(); }

    // ------------------------------------------------------------------ phím

    /** SkillService.cast: đang hợp thể thì Q/R/T là kỹ năng trụ (không tốn mana, hồi chiêu riêng), E bị bỏ qua. Trả true nếu đã xử lý. */
    public static boolean interceptCast(ServerPlayer p, int slot) {
        if (!controlling(p)) return false;
        if (slot != 1 && p.level() instanceof ServerLevel sl) skillKey(sl, p, slot);
        return true;
    }

    /** Bấm nhanh E (trước SkillService, bỏ qua hồi chiêu E): có trụ cấp 3 thì thử hợp thể. Trả true nếu đã xử lý. */
    public static boolean onTap(ServerPlayer p) {
        if (titan(p) == null) return false;
        if (!controlling(p) && p.level() instanceof ServerLevel sl) tryEnter(sl, p);
        return true;
    }

    /** E qua SkillService (PhapSkills): có trụ Hợp Thể thì nâng cấp 3. Trả true nếu đã xử lý (không dựng trụ thường). */
    public static boolean onE(ServerLevel sl, ServerPlayer p) {
        if (titan(p) != null) { if (!controlling(p)) tryEnter(sl, p); return true; }
        PhapTurret merged = null;
        for (PhapTurret t : PhapRuntime.turretsOf(sl, p)) if (t.isMerged()) merged = t;
        if (merged == null) return false;
        create(sl, p, merged);
        return true;
    }

    /** Giữ E khi đang hợp thể: bắt đầu tự huỷ. */
    public static void selfDestruct(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        PhapTurret t = titan(p);
        if (s == null || t == null || !s.control || s.destructLeft > 0 || !(p.level() instanceof ServerLevel sl)) return;
        s.destructLeft = DESTRUCT_TICKS;
        s.laserLeft = 0;
        Vec3 c = t.position();
        Fx.telegraphCircle(sl, p, c.add(0, 0.05, 0), (float) PULL_R, DESTRUCT_TICKS, LAVA);
        Fx.rune(sl, p, c.add(0, 0.08, 0), (float) HARD_PULL_R + 1, LAVA, DESTRUCT_TICKS, 12);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2f, 0.4f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2f, 0.5f);
        msg(p, "destruct", LAVA, DESTRUCT_TICKS / 20);
    }

    // ------------------------------------------------------------------ tạo / hợp thể / kết thúc

    static void create(ServerLevel sl, ServerPlayer p, PhapTurret core) {
        float hp = 0;
        for (PhapTurret t : PhapRuntime.turretsOf(sl, p)) {
            hp += t.getMaxHealth();
            if (t != core) {
                Vec3 a = t.position().add(0, 1.5, 0), b = core.position().add(0, 2.5, 0);
                Fx.ribbonWave(sl, p, a, b, 0.6, 0.8, 10, 0, 0.35f, GOLD);
                Fx.ribbon(sl, p, a, b, 0.6, 10, 0, 0.6f, 0x5A4632);
                sl.sendParticles(STONE, t.getX(), t.getY() + 1, t.getZ(), 30, 0.5, 0.8, 0.5, 0.2);
                t.discardQuiet();
            }
        }
        PhapRuntime.clearTurrets(p);
        core.makeTitan(hp * (float) PhapMath.THO_TITAN_HP_MULT, AUTO_LIFE);
        St s = new St();
        s.titan = core.getUUID();
        s.ref = core;
        s.createdAt = now(p);
        STATES.put(p.getUUID(), s);
        halveMana(p);
        Vec3 c = core.position();
        Fx.groundCrack(sl, p, c.add(0, 0.06, 0), 7f, COL, 12, 70);
        Fx.rune(sl, p, c.add(0, 0.08, 0), 6f, GOLD, 60, 8);
        Fx.groundRing(sl, p, c.add(0, 0.06, 0), 8f, GOLD, 24);
        Fx.telegraphCircle(sl, p, c.add(0, 0.05, 0), (float) (ENTER_RANGE + core.getBbWidth() / 2), ENTER_WINDOW, GOLD); // vòng hợp thể 5s
        Fx.elem(sl, p, Fx.E_SPIKES, c, Vec3.ZERO, 3.2f, COL, 40);
        Fx.shake(sl, c, 1.2f, 24, 40f);
        for (int i = 0; i < 6; i++) {
            int d = i * 3;
            SkillScheduler.later(d + 1, () -> sl.sendParticles(STONE, c.x, c.y + d * 0.5, c.z, 40, 1.6, 0.4, 1.6, 0.3));
        }
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.6f, 0.7f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.4f, 0.4f);
        msg(p, "create", GOLD);
    }

    private static void halveMana(ServerPlayer p) {
        int cur = p.getData(ModAttachments.CULTIVATION).resource();
        if (cur > 0) Resources.add(p, -(cur / 2));
    }

    static Vec3 seat(PhapTurret t) { return t.position().add(0, SEAT_Y, 0); }

    static Vec3 core(PhapTurret t) { return t.position().add(0, CORE_Y, 0); }

    private static void place(ServerPlayer p, Vec3 v) {
        p.teleportTo(v.x, v.y, v.z);
        if (p.position().distanceToSqr(v) > 0.01) p.moveTo(v.x, v.y, v.z, p.getYRot(), p.getXRot());
    }

    /** Thời gian hồi hợp thể còn lại (tick), 0 nếu dùng được. */
    public static long mergeCooldown(ServerPlayer p) {
        Long last = LAST_MERGE.get(p.getUUID());
        return last == null ? 0 : Math.max(0, last + MERGE_CD - now(p));
    }

    static void tryEnter(ServerLevel sl, ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        PhapTurret t = titan(p);
        if (s == null || t == null || s.control) return;
        if (s.entered || now(p) - s.createdAt > ENTER_WINDOW) { msg(p, "window_closed", 0x999999); return; }
        long cd = mergeCooldown(p);
        if (cd > 0) { msg(p, "merge_cd", 0x999999, (cd + 19) / 20); return; }
        double d = Math.hypot(p.getX() - t.getX(), p.getZ() - t.getZ());
        if (d > ENTER_RANGE + t.getBbWidth() / 2) { msg(p, "far", 0x999999); return; }
        s.control = true;
        s.entered = true;
        LAST_MERGE.put(p.getUUID(), now(p));
        t.setControlled(true);
        t.setLife(MERGED_LIFE);
        ThoService.endPhase(p, false);
        Vec3 st = seat(t);
        Fx.ribbon(sl, p, p.position().add(0, 1, 0), st, 0.5, 6, 0, 0.5f, GOLD);
        Fx.ribbonHelix(sl, p, t.position(), st.add(0, 1, 0), 2.2, 0, 10, 0, 0.18f, FIRE);
        Fx.groundRing(sl, p, t.position().add(0, 0.06, 0), 6f, FIRE, 20);
        Fx.shake(sl, t.position(), 0.8f, 12, 24f);
        p.setInvisible(true);
        p.setNoGravity(true);
        place(p, st);
        PacketDistributor.sendToPlayer(p, new ThoStatePayload(ThoStatePayload.TITAN_ON, MERGED_LIFE));
        sl.playSound(null, st.x, st.y, st.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.4f, 0.6f);
        sl.playSound(null, st.x, st.y, st.z, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 0.8f);
        msg(p, "enter", GOLD);
    }

    private static void eject(ServerLevel sl, ServerPlayer p, PhapTurret t) {
        p.setInvisible(false);
        p.setNoGravity(false);
        p.fallDistance = 0;
        if (t != null) {
            Vec3 dir = p.getLookAngle().multiply(1, 0, 1);
            if (dir.lengthSqr() < 1e-4) dir = new Vec3(1, 0, 0);
            place(p, PhapSkills.ground(sl, t.position().add(dir.normalize().scale(t.getBbWidth() / 2 + 2)).add(0, 3, 0)));
        }
        PacketDistributor.sendToPlayer(p, new ThoStatePayload(ThoStatePayload.TITAN_OFF, 0));
    }

    /** Trụ cấp 3 bị gỡ (vỡ / tự huỷ / hết giờ). destroyed: bị phá hoặc tự huỷ. */
    static void onEnd(ServerLevel sl, ServerPlayer p, PhapTurret t, boolean destroyed) {
        St s = STATES.remove(p.getUUID());
        if (s == null) return;
        if (s.control) {
            eject(sl, p, t);
            if (destroyed) {
                ThoService.freePhase(p, PhapMath.THO_TITAN_ESCAPE_TICKS);
                ThiemBoService.grantImmunity(p);
            }
        }
        msg(p, destroyed ? "broken" : "expired", 0x999999);
    }

    // ------------------------------------------------------------------ mỗi tick (gọi từ PhapTurret)

    static void tick(ServerLevel sl, ServerPlayer o, PhapTurret t, float shot) {
        St s = STATES.get(o.getUUID());
        if (s == null) return;
        long now = sl.getGameTime();
        if (s.control) lockSeat(o, t);
        else if (!s.entered && !s.windowMsg && now - s.createdAt > ENTER_WINDOW) { s.windowMsg = true; msg(o, "window_closed", 0x999999); }
        if (s.destructLeft > 0) { tickDestruct(sl, o, t, s, shot); return; }
        boolean qOn = s.control && now < s.qEnd;
        if (qOn) { // Q: 3 phát liên tục mỗi loạt
            if (t.tickCount % Q_VOLLEY == 0) {
                List<LivingEntity> ts = targets(sl, t, 3);
                for (int i = 0; i < 3 && !ts.isEmpty(); i++) {
                    LivingEntity e = ts.get(i % ts.size());
                    SkillScheduler.later(i * 2 + 1, () -> { if (e.isAlive()) shell(sl, o, t, e.getBoundingBox().getCenter(), (float) (shot * Q_MULT), Q_R, 1.3f, true, s); });
                }
            }
        } else if (t.tickCount % AUTO_TICKS == 0) { // tự bắn (cả khi hợp thể)
            List<LivingEntity> ts = targets(sl, t, 1);
            if (!ts.isEmpty()) shell(sl, o, t, ts.get(0).getBoundingBox().getCenter(), (float) (shot * AUTO_MULT), AUTO_R, 1.6f, false, null);
        }
        tickZones(sl, t, s, now);
        if (s.laserLeft > 0) tickLaser(sl, o, t, s, shot);
        if (t.tickCount % 30 == 0) {
            Fx.groundRing(sl, null, t.position().add(0, 0.06, 0), 4.5f, s.control ? FIRE : GOLD, 20);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, t.getX(), t.getY() + 0.3, t.getZ(), 3, 2.0, 0.1, 2.0, 0.01);
        }
        if (qOn && t.tickCount % 4 == 0) { // hào quang cường hoá quanh lõi
            Vec3 c = core(t);
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 6, 1.2, 0.8, 1.2, 0.03);
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 1, 0.8, 0.5, 0.8, 0);
        }
    }

    static void lockSeat(ServerPlayer p, PhapTurret t) {
        Vec3 st = seat(t);
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        if (p.position().distanceToSqr(st) > 0.04) place(p, st);
    }

    private static List<LivingEntity> targets(ServerLevel sl, PhapTurret t, int n) {
        double r = PhapMath.THO_TITAN_RANGE * 0.7;
        return sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(r, 8, r), t::isEnemy).stream()
                .filter(e -> e.distanceToSqr(t) <= r * r)
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(t))).limit(n).toList();
    }

    /** Đạn nổ cỡ lớn: bay từ lõi, nổ bán kính r. Tự động: chậm nhẹ. Cường hoá Q: choáng + vùng chậm 3s. */
    static void shell(ServerLevel sl, ServerPlayer o, PhapTurret t, Vec3 to, float dmg, double r, float size, boolean stun, St s) {
        Vec3 from = core(t);
        Vec3 d = to.subtract(from);
        int flight = Math.max(3, (int) Math.round(d.length() / 3.0));
        Fx.qi(sl, from.add(d.normalize().scale(1.6)), d.normalize(), 1.2f * size, stun ? FIRE : GOLD, 0, 6);
        Fx.elem(sl, o, Fx.E_SHOT, from, d, 2.4f * size, COL, flight);
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, stun ? 0.7f : 0.5f);
        SkillScheduler.later(flight, () -> {
            if (!o.isAlive()) return;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(r + 1, r, r + 1),
                    x -> t.isEnemy(x) && x.distanceToSqr(to) <= (r + x.getBbWidth() / 2) * (r + x.getBbWidth() / 2))) {
                e.invulnerableTime = 0;
                PhapTurret.hitNoKnock(o, e, dmg, 40, stun ? 9 : 1); // amp 1 = chậm nhẹ; amp 9 = choáng
                if (stun) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 3, false, false, true));
            }
            Vec3 g = PhapSkills.ground(sl, to.add(0, 0.5, 0));
            boolean low = to.y - g.y < 3;
            if (low) {
                Fx.groundCrack(sl, o, g.add(0, 0.06, 0), (float) r + 0.6f, COL, 8, 34);
                Fx.groundRing(sl, o, g.add(0, 0.06, 0), (float) r, FIRE, 12);
            }
            Fx.play(sl, FxType.SHOCKWAVE, to, Vec3.ZERO, (float) r, FIRE, 12);
            if (stun && s != null) {
                Zone z = new Zone();
                z.pos = low ? g : to;
                z.until = sl.getGameTime() + 60;
                s.zones.add(z);
                Fx.pool(sl, o, z.pos.add(0, 0.05, 0), (float) r, 0x7A3A14, 60);
                Fx.rune(sl, o, z.pos.add(0, 0.07, 0), (float) r * 0.8f, LAVA, 60, 6);
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 2 + (int) size, r * 0.3, 0.3, r * 0.3, 0);
            sl.sendParticles(STONE, to.x, to.y, to.z, 28, r * 0.3, 0.4, r * 0.3, 0.3);
            sl.sendParticles(ParticleTypes.LAVA, to.x, to.y, to.z, 5, 0.5, 0.3, 0.5, 0);
            sl.playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        });
    }

    private static void tickZones(ServerLevel sl, PhapTurret t, St s, long now) {
        s.zones.removeIf(z -> now >= z.until);
        if (t.tickCount % 10 != 0) return;
        for (Zone z : s.zones) {
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(z.pos, z.pos).inflate(Q_R, 2, Q_R),
                    x -> t.isEnemy(x) && x.distanceToSqr(z.pos) <= Q_R * Q_R + 4))
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false, true));
            sl.sendParticles(ParticleTypes.SMALL_FLAME, z.pos.x, z.pos.y + 0.2, z.pos.z, 4, Q_R * 0.5, 0.05, Q_R * 0.5, 0.01);
        }
    }

    // ------------------------------------------------------------------ 3 kỹ năng trụ

    static void skillKey(ServerLevel sl, ServerPlayer p, int slot) {
        St s = STATES.get(p.getUUID());
        PhapTurret t = titan(p);
        if (s == null || t == null || s.destructLeft > 0) return;
        long now = sl.getGameTime();
        switch (slot) {
            case 0 -> { // Q: Liệt Hoả Liên Pháo
                if (now < s.qCdEnd) { msg(p, "cd", 0x999999, (s.qCdEnd - now + 19) / 20); return; }
                s.qEnd = now + Q_TICKS;
                s.qCdEnd = now + Q_CD;
                Vec3 c = core(t);
                Fx.qi(sl, c, p.getLookAngle(), 2.4f, FIRE, 0, 10);
                Fx.ribbonHelix(sl, p, t.position(), c, 3.0, 0, 8, 0, 0.22f, FIRE);
                Fx.groundRing(sl, p, t.position().add(0, 0.06, 0), 5.5f, FIRE, 16);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5f, 0.5f);
                msg(p, "q", FIRE);
            }
            case 2 -> { // R: Địa Thứ Hộ Tháp
                if (now < s.rCdEnd) { msg(p, "cd", 0x999999, (s.rCdEnd - now + 19) / 20); return; }
                s.rCdEnd = now + R_CD;
                spikes(sl, p, t);
                msg(p, "r", GOLD);
            }
            default -> { // T: Tia Diệt Thế (1 lần mỗi trụ cấp 3)
                if (s.tUsed) { msg(p, "t_used", 0x999999); return; }
                s.tUsed = true;
                s.laserLeft = T_TICKS;
                Vec3 c = core(t);
                Fx.qi(sl, c, p.getLookAngle(), 3.0f, 0xFFFFFF, 0, 12);
                Fx.shake(sl, c, 1.0f, T_TICKS, 40f);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2f, 0.5f);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.6f);
                msg(p, "t", 0xFFFFFF);
            }
        }
    }

    /** 6 gai đá có máu riêng quanh chân trụ: đẩy lùi + gây sát thương. */
    static void spikes(ServerLevel sl, ServerPlayer p, PhapTurret t) {
        int n = 6;
        double rr = t.getBbWidth() / 2 + 2.2;
        float hp = t.getMaxHealth() * 0.12f;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 at = PhapSkills.ground(sl, t.position().add(Math.cos(a) * rr, 2, Math.sin(a) * rr));
            PhapTurret sp = PhapTuRegistry.TURRET.get().create(sl);
            if (sp == null) continue;
            sp.moveTo(at.x, at.y, at.z, (float) Math.toDegrees(a), 0);
            sp.makeSpike(p, hp, t.shotDamage() * (float) SPIKE_MULT, SPIKE_LIFE, t.position());
            sl.addFreshEntity(sp);
            SkillScheduler.later(i + 1, () -> {
                Fx.elem(sl, p, Fx.E_SPIKES, at, Vec3.ZERO, 0.9f, COL, 26);
                Fx.groundCrack(sl, p, at.add(0, 0.06, 0), 1.6f, COL, 6, 30);
                sl.sendParticles(STONE, at.x, at.y + 0.5, at.z, 20, 0.4, 0.6, 0.4, 0.2);
            });
        }
        Fx.groundCrack(sl, p, t.position().add(0, 0.06, 0), (float) rr + 1.5f, COL, 12, 50);
        Fx.shake(sl, t.position(), 0.9f, 12, 24f);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 1.6f, 0.4f);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.4f, 0.5f);
    }

    /** Tia Diệt Thế: tia khổng lồ từ lõi theo hướng nhìn người điều khiển (di chuột lái tia). */
    private static void tickLaser(ServerLevel sl, ServerPlayer o, PhapTurret t, St s, float shot) {
        s.laserLeft--;
        Vec3 from = core(t), look = o.getLookAngle();
        Vec3 end = from.add(look.scale(48));
        var hit = sl.clip(new ClipContext(from.add(look.scale(2.5)), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, t));
        Vec3 to = hit.getLocation();
        float age = T_TICKS - s.laserLeft;
        float w = Math.min(1f, age / 10f) * (s.laserLeft < 10 ? s.laserLeft / 10f : 1f);
        if (s.laserLeft % 2 == 0) {
            Fx.ribbon(sl, o, from, to, 0, 3, 0, 1.6f * w + 0.1f, LAVA);
            Fx.ribbon(sl, o, from, to, 0, 3, 0, 0.9f * w + 0.05f, GOLD);
            Fx.ribbon(sl, o, from, to, 0, 3, 0, 0.35f * w + 0.03f, 0xFFFFFF);
            Fx.ribbonHelix(sl, o, from, to, 1.4 * w, age * 0.5, 3, 0, 0.12f, FIRE);
            Fx.qi(sl, from, look, 1.8f * w + 0.3f, 0xFFFFFF, 0, 4);
        }
        if (s.laserLeft % 4 == 0) {
            Vec3 seg = to.subtract(from);
            double len = seg.length();
            Vec3 u = seg.scale(1 / Math.max(1e-6, len));
            double rad = 2.0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(rad + 1), t::isEnemy)) {
                Vec3 c = e.getBoundingBox().getCenter().subtract(from);
                double along = Math.max(0, Math.min(len, c.dot(u)));
                if (c.subtract(u.scale(along)).length() > rad + e.getBbWidth() / 2) continue;
                e.invulnerableTime = 0;
                PhapTurret.hitNoKnock(o, e, (float) (shot * T_MULT), 10, 3);
                e.igniteForSeconds(3);
            }
            Fx.groundCrack(sl, o, to.add(0, 0.06, 0), 2.6f, LAVA, 8, 24);
            Fx.pool(sl, o, PhapSkills.ground(sl, to.add(0, 0.5, 0)).add(0, 0.05, 0), 2.2f, 0x5A1E08, 40);
            sl.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 2, 0.8, 0.5, 0.8, 0);
            sl.sendParticles(ParticleTypes.LAVA, to.x, to.y, to.z, 6, 0.8, 0.4, 0.8, 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, to.x, to.y + 1, to.z, 6, 0.8, 0.6, 0.8, 0.04);
            if (s.laserLeft % 12 == 0) sl.playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.5f);
        }
        if (s.laserLeft % 20 == 0) sl.playSound(null, from.x, from.y, from.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2f, 0.5f);
    }

    /** Tự huỷ: hút quái 15 ô, kéo cứng trong 4 ô; hết 7s nổ như bom hạt nhân. */
    private static void tickDestruct(ServerLevel sl, ServerPlayer o, PhapTurret t, St s, float shot) {
        s.destructLeft--;
        Vec3 c = t.position();
        float k = 1 - s.destructLeft / (float) DESTRUCT_TICKS;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(PULL_R, 8, PULL_R),
                x -> t.isEnemy(x) && x.distanceToSqr(t) <= PULL_R * PULL_R)) {
            double dx = c.x - e.getX(), dz = c.z - e.getZ(), d = Math.max(0.5, Math.hypot(dx, dz));
            if (d <= HARD_PULL_R + t.getBbWidth() / 2) { // kéo cứng sát chân trụ
                double keep = t.getBbWidth() / 2 + 0.8, nd = Math.max(keep, d - 0.6);
                if (d > keep) e.setPos(c.x - dx / d * nd, e.getY(), c.z - dz / d * nd);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false, true));
            } else {
                double sp = 0.18 + 0.25 * k;
                e.setDeltaMovement(dx / d * sp, Math.min(0.1, e.getDeltaMovement().y), dz / d * sp);
                e.hurtMarked = true;
            }
            if (t.tickCount % 6 == 0) Fx.ribbon(sl, o, e.getBoundingBox().getCenter(), core(t), 0.3, 6, 0, 0.08f, LAVA);
        }
        if (s.destructLeft % 20 == 0 && s.destructLeft > 0) {
            int sec = s.destructLeft / 20;
            msg(o, "destruct", LAVA, sec);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 2f, 0.5f + 0.15f * (7 - sec));
            Fx.groundRing(sl, o, c.add(0, 0.06, 0), (float) PULL_R * (sec / 7f), LAVA, 20);
        }
        if (t.tickCount % 3 == 0) {
            Vec3 cc = core(t);
            sl.sendParticles(ParticleTypes.FLAME, cc.x, cc.y, cc.z, 8 + (int) (20 * k), 1.5 * (1 - k) + 0.3, 1.0, 1.5 * (1 - k) + 0.3, 0.02);
            sl.sendParticles(ParticleTypes.LAVA, cc.x, cc.y, cc.z, 2, 1, 1, 1, 0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 20, PULL_R * 0.5, 1.5, PULL_R * 0.5, 0.2);
        }
        if (s.destructLeft > 0) return;
        nuke(sl, o, t, shot);
    }

    private static void nuke(ServerLevel sl, ServerPlayer o, PhapTurret t, float shot) {
        Vec3 c = t.position();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(NUKE_R, 10, NUKE_R),
                x -> t.isEnemy(x) && x.distanceToSqr(t) <= NUKE_R * NUKE_R)) {
            double d = Math.sqrt(e.distanceToSqr(t));
            e.invulnerableTime = 0;
            DamageRules.hurtBySkill(o, e, (float) (shot * NUKE_MULT * (1 - 0.5 * d / NUKE_R)));
            e.igniteForSeconds(6);
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) away = away.normalize();
            e.setDeltaMovement(away.x * 1.6, 0.9, away.z * 1.6);
            e.hurtMarked = true;
        }
        Fx.elem(sl, o, Fx.E_NUKE, c, Vec3.ZERO, 9f, LAVA, 90);
        Fx.play(sl, FxType.SHOCKWAVE, c.add(0, 0.5, 0), Vec3.ZERO, (float) NUKE_R * 1.6f, 0xFFE9A0, 26);
        Fx.play(sl, FxType.SCREEN_TINT, c, Vec3.ZERO, 1f, 0xFFF0C0, 14);
        Fx.groundCrack(sl, o, c.add(0, 0.06, 0), (float) NUKE_R + 3, LAVA, 16, 120);
        Fx.pool(sl, o, c.add(0, 0.05, 0), (float) NUKE_R * 0.8f, 0x2A1408, 200);
        Fx.groundRing(sl, o, c.add(0, 0.06, 0), (float) NUKE_R, 0xFFFFFF, 20);
        Fx.shake(sl, c, 2.5f, 50, 80f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 6, 3, 2, 3, 0);
        sl.sendParticles(ParticleTypes.FLASH, c.x, c.y + 3, c.z, 4, 1, 1, 1, 0);
        sl.sendParticles(STONE, c.x, c.y + 2, c.z, 200, 3, 2, 3, 0.9);
        for (int i = 1; i <= 8; i++) {
            int d = i * 4;
            SkillScheduler.later(d, () -> {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x + sl.random.nextGaussian() * 5, c.y + 1 + d * 0.3, c.z + sl.random.nextGaussian() * 5, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, c.x, c.y + 4 + d * 0.4, c.z, 30, 3, 1.5, 3, 0.03);
                sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 2, c.z, 12, 4, 1, 4, 0);
            });
        }
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4f, 0.3f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4f, 0.3f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4f, 0.4f);
        onEnd(sl, o, t, true);
        t.discardQuiet();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level() instanceof ServerLevel sl) {
            PhapTurret t = titan(p);
            St s = STATES.remove(p.getUUID());
            if (s != null && s.control) eject(sl, p, t);
            if (t != null) t.discardQuiet();
        }
    }
}
