package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.gates.Resources;
import com.hukhong.gates.DamageRules;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thổ Tháp cấp 3 - Thái Sơn Thần Tháp: E khi đã có trụ Hợp Thể -> hợp mọi trụ thành MỘT trụ khổng lồ (mỗi người 1 trụ).
 * Tự động: trừ 1/2 Pháp Lực hiện tại, Pháp Lực không tự hồi tới khi trụ vỡ / hết giờ. E lần nữa: nhập vào trụ điều khiển
 * (đứng yên trên đỉnh, trụ bắn theo tâm ngắm, Q/R/T thành 3 kỹ năng riêng của trụ). Trụ vỡ khi đang nhập: xuyên tường + bất tử 3s.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ThoTitan {
    private ThoTitan() {}

    static final int COL = PhapElement.THO.rgb, FIRE = 0xFF8A2A, GOLD = 0xFFD27A;
    private static final BlockParticleOption STONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE_BRICKS.defaultBlockState());

    static final class St {
        UUID titan;
        boolean control;
        PhapTurret ref;
    }

    private static final Map<UUID, St> STATES = new ConcurrentHashMap<>();

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

    /** Trụ cấp 3 đang ở chế độ tự động (chặn hồi Pháp Lực). */
    public static boolean autoActive(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && !s.control && titan(p) != null;
    }

    /** E của Thổ: có trụ cấp 3 -> bật/tắt nhập trụ; có trụ Hợp Thể -> nâng cấp 3. Trả true nếu đã xử lý (không dựng trụ thường). */
    public static boolean onE(ServerLevel sl, ServerPlayer p) {
        if (titan(p) != null) { toggle(sl, p); return true; }
        PhapTurret merged = null;
        for (PhapTurret t : PhapRuntime.turretsOf(sl, p)) if (t.isMerged()) merged = t;
        if (merged == null) return false;
        create(sl, p, merged);
        return true;
    }

    static void create(ServerLevel sl, ServerPlayer p, PhapTurret core) {
        float hp = 0;
        int life = 0;
        for (PhapTurret t : PhapRuntime.turretsOf(sl, p)) {
            hp += t.getMaxHealth();
            life = Math.max(life, t.lifeLeft());
            if (t != core) {
                Vec3 a = t.position().add(0, 1.5, 0), b = core.position().add(0, 2.5, 0);
                Fx.ribbonWave(sl, p, a, b, 0.6, 0.8, 10, 0, 0.35f, GOLD);
                Fx.ribbon(sl, p, a, b, 0.6, 10, 0, 0.6f, 0x5A4632);
                sl.sendParticles(STONE, t.getX(), t.getY() + 1, t.getZ(), 30, 0.5, 0.8, 0.5, 0.2);
                t.discardQuiet();
            }
        }
        PhapRuntime.clearTurrets(p);
        core.makeTitan(hp * (float) PhapMath.THO_TITAN_HP_MULT, Math.max(life, PhapMath.THO_TITAN_LIFE_TICKS));
        St s = STATES.computeIfAbsent(p.getUUID(), k -> new St());
        s.titan = core.getUUID();
        s.ref = core;
        s.control = false;
        halveMana(p);
        Vec3 c = core.position();
        Fx.groundCrack(sl, p, c.add(0, 0.06, 0), 7f, COL, 12, 70);
        Fx.rune(sl, p, c.add(0, 0.08, 0), 6f, GOLD, 60, 8);
        Fx.groundRing(sl, p, c.add(0, 0.06, 0), 8f, GOLD, 24);
        Fx.elem(sl, p, Fx.E_SPIKES, c, Vec3.ZERO, 3.2f, COL, 40);
        Fx.shake(sl, c, 1.2f, 24, 40f);
        for (int i = 0; i < 6; i++) {
            int d = i * 3;
            SkillScheduler.later(d + 1, () -> sl.sendParticles(STONE, c.x, c.y + d * 0.5, c.z, 40, 1.6, 0.4, 1.6, 0.3));
        }
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_EMERGE, net.minecraft.sounds.SoundSource.PLAYERS, 1.6f, 0.7f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.4f);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan.create").withColor(GOLD), true);
    }

    private static void halveMana(ServerPlayer p) {
        int cur = p.getData(ModAttachments.CULTIVATION).resource();
        if (cur > 0) Resources.add(p, -(cur / 2));
    }

    /** Dịch chuyển (người chơi giả không nhận teleportTo thì đặt thẳng vị trí). */
    private static void place(ServerPlayer p, Vec3 v) {
        p.teleportTo(v.x, v.y, v.z);
        if (p.position().distanceToSqr(v) > 0.01) p.moveTo(v.x, v.y, v.z, p.getYRot(), p.getXRot());
    }

    static Vec3 seat(PhapTurret t) { return t.position().add(0, PhapMath.THO_TITAN_SEAT_Y, 0); }

    static void toggle(ServerLevel sl, ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        PhapTurret t = titan(p);
        if (s == null || t == null) return;
        if (!s.control) {
            if (p.distanceToSqr(t) > PhapMath.THO_TITAN_ENTER_RANGE * PhapMath.THO_TITAN_ENTER_RANGE) {
                p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan.far").withColor(0x999999), true);
                return;
            }
            s.control = true;
            t.setControlled(true);
            ThoService.endPhase(p, false);
            Vec3 st = seat(t);
            Fx.ribbon(sl, p, p.position().add(0, 1, 0), st, 0.5, 6, 0, 0.4f, GOLD);
            p.setInvisible(true);
            p.setNoGravity(true);
            place(p, st);
            sl.playSound(null, st.x, st.y, st.z, SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
            p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan.enter").withColor(GOLD), true);
        } else {
            exit(sl, p, s, t);
            halveMana(p); // về tự động: lại trừ 1/2 Pháp Lực
            p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan.auto").withColor(GOLD), true);
        }
    }

    private static void exit(ServerLevel sl, ServerPlayer p, St s, PhapTurret t) {
        s.control = false;
        if (t != null) t.setControlled(false);
        p.setInvisible(false);
        p.setNoGravity(false);
        p.fallDistance = 0;
        Vec3 out = t == null ? p.position() : PhapSkills.ground(sl, t.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(3.5)).add(0, 2, 0));
        place(p, out);
    }

    /** Trụ cấp 3 bị gỡ (vỡ hoặc hết giờ). destroyed: bị phá. */
    static void onEnd(ServerLevel sl, ServerPlayer p, boolean destroyed) {
        St s = STATES.remove(p.getUUID());
        if (s == null) return;
        if (s.control) {
            exit(sl, p, s, null);
            p.teleportTo(p.getX(), p.getY(), p.getZ());
            if (destroyed) {
                ThoService.freePhase(p, PhapMath.THO_TITAN_ESCAPE_TICKS);
                ThiemBoService.grantImmunity(p);
            }
        }
        p.displayClientMessage(Component.translatable(destroyed ? "hukhong.phap.tho.titan.broken" : "hukhong.phap.tho.titan.expired").withColor(0x999999), true);
    }

    /** Mỗi tick khi đang nhập trụ: giữ người chơi trên đỉnh trụ. */
    static void lockSeat(ServerPlayer p, PhapTurret t) {
        Vec3 st = seat(t);
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        if (p.position().distanceToSqr(st) > 0.04) place(p, st);
    }

    /** Điểm/mục tiêu theo tâm ngắm của người điều khiển (thực thể trên tia, hoặc điểm chạm khối). */
    static Vec3 aimPoint(ServerLevel sl, ServerPlayer p, PhapTurret t, LivingEntity[] hitOut) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(PhapMath.THO_TITAN_RANGE));
        var bh = sl.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        Vec3 stop = bh.getLocation();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(eye, stop).inflate(1.5), x -> t.isEnemy(x))) {
            var hit = e.getBoundingBox().inflate(0.8).clip(eye, stop);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bd) { bd = eye.distanceToSqr(hit.get()); best = e; }
        }
        hitOut[0] = best;
        return best != null ? best.getBoundingBox().getCenter() : stop;
    }

    /** Đạn đá lớn của trụ cấp 3: bay từ đỉnh trụ, nổ lan bán kính r. */
    static void cannon(ServerLevel sl, ServerPlayer o, PhapTurret t, Vec3 to, float dmg, double r, float size) {
        Vec3 from = t.position().add(0, PhapMath.THO_TITAN_SEAT_Y - 0.6, 0);
        Vec3 d = to.subtract(from);
        int flight = Math.max(3, (int) Math.round(d.length() / 3.0));
        Fx.qi(sl, from.add(d.normalize().scale(1.4)), d.normalize(), 1.1f * size, GOLD, 0, 6);
        Fx.elem(sl, o, Fx.E_SHOT, from, d, 2.2f * size, COL, flight);
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.5f);
        SkillScheduler.later(flight, () -> {
            if (!o.isAlive()) return;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(r + 1, r, r + 1),
                    x -> t.isEnemy(x) && x.distanceToSqr(to) <= (r + x.getBbWidth() / 2) * (r + x.getBbWidth() / 2))) {
                e.invulnerableTime = 0;
                PhapTurret.hitNoKnock(o, e, dmg, 30, 2);
            }
            Vec3 g = PhapSkills.ground(sl, to.add(0, 0.5, 0));
            if (to.y - g.y < 3) {
                Fx.groundCrack(sl, o, g.add(0, 0.06, 0), (float) r + 0.5f, COL, 8, 34);
                Fx.groundRing(sl, o, g.add(0, 0.06, 0), (float) r, FIRE, 12);
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1 + (int) size, r * 0.3, 0.3, r * 0.3, 0);
            sl.sendParticles(STONE, to.x, to.y, to.z, 24, r * 0.3, 0.4, r * 0.3, 0.25);
            sl.sendParticles(ParticleTypes.LAVA, to.x, to.y, to.z, 4, 0.4, 0.3, 0.4, 0);
            sl.playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.8f);
        });
    }

    // ------------------------------------------------------------------ 3 kỹ năng khi nhập trụ (Q / R / T)

    /** Q/R/T khi đang nhập trụ (slot 0/2/3). c = hệ số sát thương kỹ năng tương ứng. */
    static void skill(ServerLevel sl, ServerPlayer p, int slot, double c) {
        PhapTurret t = titan(p);
        if (t == null) return;
        LivingEntity[] h = new LivingEntity[1];
        Vec3 aim = aimPoint(sl, p, t, h);
        switch (slot) {
            case 0 -> { // Địa Long Pháo: 3 phát đạn đá khổng lồ liên tiếp vào tâm ngắm
                for (int i = 0; i < 3; i++) {
                    int d = i * 5;
                    SkillScheduler.later(d + 1, () -> {
                        LivingEntity[] hh = new LivingEntity[1];
                        cannon(sl, p, t, aimPoint(sl, p, t, hh), (float) (c * PhapMath.THO_TITAN_Q_COEF), 3.5, 1.6f);
                    });
                }
                p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan.q").withColor(GOLD), true);
            }
            case 2 -> { // Chấn Sơn: sóng chấn động quanh trụ, choáng + hất tung
                Vec3 c0 = t.position();
                double r = PhapMath.THO_TITAN_R_RADIUS;
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(r, 4, r),
                        x -> t.isEnemy(x) && Math.hypot(x.getX() - c0.x, x.getZ() - c0.z) <= r)) {
                    e.invulnerableTime = 0;
                    DamageRules.hurtBySkill(p, e, (float) (c * PhapMath.THO_TITAN_R_COEF));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PhapMath.THO_TITAN_R_STUN_TICKS, 9, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PhapMath.THO_TITAN_R_STUN_TICKS, 4, false, false, true));
                    e.setDeltaMovement(0, 0.7, 0);
                    e.hurtMarked = true;
                }
                for (int i = 0; i < 3; i++) {
                    int d = i * 4;
                    float rr = (float) r * (0.45f + 0.28f * i);
                    SkillScheduler.later(d + 1, () -> {
                        Fx.groundRing(sl, p, c0.add(0, 0.06, 0), rr, i2c(d), 14);
                        Fx.elem(sl, p, Fx.E_SPIKES, c0, Vec3.ZERO, rr / 1.9f, COL, 30);
                        sl.sendParticles(STONE, c0.x, c0.y + 0.3, c0.z, 40, rr * 0.5, 0.2, rr * 0.5, 0.25);
                    });
                }
                Fx.groundCrack(sl, p, c0.add(0, 0.06, 0), (float) r, COL, 14, 60);
                Fx.shake(sl, c0, 1.3f, 18, 30f);
                sl.playSound(null, c0.x, c0.y, c0.z, SoundEvents.WARDEN_SONIC_BOOM, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.5f);
                p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan.r").withColor(GOLD), true);
            }
            default -> { // Thiên Thạch Vũ: 10 thiên thạch dội xuống quanh tâm ngắm trong 2s
                Vec3 g = PhapSkills.ground(sl, aim.add(0, 1, 0));
                Fx.rune(sl, p, g.add(0, 0.08, 0), (float) PhapMath.THO_TITAN_T_AREA, FIRE, 60, 9);
                Fx.telegraphCircle(sl, p, g.add(0, 0.05, 0), (float) PhapMath.THO_TITAN_T_AREA, 20, FIRE);
                var rnd = sl.random;
                for (int i = 0; i < PhapMath.THO_TITAN_T_METEORS; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2, rr = Math.sqrt(rnd.nextDouble()) * PhapMath.THO_TITAN_T_AREA;
                    Vec3 pt = PhapSkills.ground(sl, g.add(Math.cos(a) * rr, 2, Math.sin(a) * rr));
                    int d = 2 + i * 4;
                    SkillScheduler.later(d, () -> ThoFx.meteor(sl, p, pt, 1.6f, 10, 16));
                    SkillScheduler.later(d + 10, () -> {
                        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pt, pt).inflate(3, 3, 3),
                                x -> t.isEnemy(x) && x.distanceToSqr(pt) <= 9)) {
                            e.invulnerableTime = 0;
                            DamageRules.hurtBySkill(p, e, (float) (c * PhapMath.THO_TITAN_T_COEF));
                        }
                        Fx.groundCrack(sl, p, pt.add(0, 0.06, 0), 2.6f, COL, 6, 30);
                        sl.sendParticles(ParticleTypes.EXPLOSION, pt.x, pt.y + 0.5, pt.z, 1, 0, 0, 0, 0);
                        sl.playSound(null, pt.x, pt.y, pt.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.6f);
                    });
                }
                p.displayClientMessage(Component.translatable("hukhong.phap.tho.titan.t").withColor(GOLD), true);
            }
        }
    }

    private static int i2c(int d) { return d == 0 ? GOLD : d == 4 ? FIRE : COL; }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level() instanceof ServerLevel sl) {
            PhapTurret t = titan(p);
            St s = STATES.get(p.getUUID());
            if (s != null && s.control) exit(sl, p, s, t);
            STATES.remove(p.getUUID());
            if (t != null) t.discardQuiet();
        }
    }
}
