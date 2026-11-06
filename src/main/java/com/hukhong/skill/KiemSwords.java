package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kiếm Tu (2026-11-06):
 * <ul>
 *   <li>Q — Thiên Kiếm Trấn: tối đa 5 kẻ địch trước mặt (quạt 10 ô), mỗi kẻ bị một đinh kiếm từ trời giáng xuống găm vào:
 *   sát thương = hệ số Q cũ x Công, giam cầm 3 giây (đứng yên tại chỗ, kiếm cắm cạnh, vòng xích ánh sáng).</li>
 *   <li>E — Thủ Hộ Chi Kiếm: 6 thanh kiếm xoay quanh người trong 10 giây. Mỗi lần bị đánh: tiêu 1 kiếm, giảm 50% sát thương đòn đó,
 *   thanh kiếm bay thẳng vào kẻ tấn công gây hệ số E x Công.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class KiemSwords {
    private KiemSwords() {}

    public static final int PIN_MAX = 5, PIN_TICKS = 60, PIN_IMPACT = 8;
    public static final double PIN_RANGE = 10, PIN_COS = 0.5;
    public static final int GUARD_SWORDS = 6, GUARD_TICKS = 200;
    public static final double GUARD_BLOCK = 0.5;
    private static final int SWORD_RGB = 0xBFE4FF;
    private static final DustParticleOptions CHAIN = new DustParticleOptions(new Vector3f(0.75f, 0.9f, 1f), 0.8f);

    // ---------------------------------------------------------------- Q: Thiên Kiếm Trấn

    private record Pin(LivingEntity e, Vec3 at, long until) {}

    private static final List<Pin> PINS = new ArrayList<>();

    public static boolean pinned(LivingEntity e) {
        for (Pin p : PINS) if (p.e == e && e.level().getGameTime() < p.until) return true;
        return false;
    }

    /** Chọn tối đa 5 mục tiêu gần nhất trong quạt trước mặt. */
    static List<LivingEntity> pinTargets(ServerLevel lv, ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize(), eye = p.position();
        List<LivingEntity> l = lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(PIN_RANGE, 3, PIN_RANGE), e -> {
            if (!SkillEffects.validTarget(p, e)) return false;
            Vec3 c = e.position().subtract(eye).multiply(1, 0, 1);
            double d = c.length();
            return d <= PIN_RANGE + e.getBbWidth() / 2 && (d < 1 || c.normalize().dot(look) >= PIN_COS);
        });
        l.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return l.size() > PIN_MAX ? new ArrayList<>(l.subList(0, PIN_MAX)) : l;
    }

    public static void thienKiemTran(ServerLevel lv, ServerPlayer p) {
        List<LivingEntity> ts = pinTargets(lv, p);
        lv.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1f, 1.4f);
        if (ts.isEmpty()) { // không có mục tiêu: vẫn cắm 1 thanh trước mặt
            Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(5));
            Fx.giantSword(lv, p, at, 1.4f, SWORD_RGB, PIN_IMPACT, 30);
            return;
        }
        int i = 0;
        for (LivingEntity e : ts) {
            int delay = i++ * 2;
            SkillScheduler.later(delay, () -> {
                if (!e.isAlive() || !(e.level() instanceof ServerLevel l2)) return;
                Fx.giantSword(l2, p, e.position(), 1.2f + e.getBbHeight() * 0.25f, SWORD_RGB, PIN_IMPACT, PIN_IMPACT + PIN_TICKS);
                Fx.telegraphCircle(l2, p, e.position().add(0, 0.05, 0), Math.max(0.8f, e.getBbWidth()), PIN_IMPACT, SWORD_RGB);
            });
            SkillScheduler.later(delay + PIN_IMPACT, () -> {
                if (!e.isAlive() || p.isRemoved() || !(e.level() instanceof ServerLevel l2)) return;
                SkillEffects.slashFor(p, e, Skill.KIEM_1_COEF);
                pin(l2, e);
                l2.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 0.3, e.getZ(), 14, 0.4, 0.1, 0.4, 0.25);
                Fx.groundRing(l2, p, e.position().add(0, 0.05, 0), Math.max(1f, e.getBbWidth() + 0.4f), SWORD_RGB, 14);
                l2.playSound(null, e.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.6f);
            });
        }
    }

    /** Giam cầm e 3 giây tại vị trí hiện tại (gọi tay trong test). */
    public static void pin(ServerLevel lv, LivingEntity e) {
        PINS.removeIf(x -> x.e == e);
        PINS.add(new Pin(e, e.position(), lv.getGameTime() + PIN_TICKS));
        e.setDeltaMovement(Vec3.ZERO);
        if (e instanceof net.minecraft.world.entity.Mob m) m.getNavigation().stop();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post ev) {
        if (PINS.isEmpty()) return;
        for (Iterator<Pin> it = PINS.iterator(); it.hasNext(); ) {
            Pin p = it.next();
            LivingEntity e = p.e;
            if (!e.isAlive() || e.isRemoved() || e.level().getGameTime() >= p.until) { it.remove(); continue; }
            if (e.position().distanceToSqr(p.at) > 0.04) {
                if (e instanceof ServerPlayer sp) sp.connection.teleport(p.at.x, p.at.y, p.at.z, sp.getYRot(), sp.getXRot());
                else e.teleportTo(p.at.x, p.at.y, p.at.z);
            }
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            if (e instanceof net.minecraft.world.entity.Mob m) m.getNavigation().stop();
            long t = e.level().getGameTime();
            if (t % 5 == 0 && e.level() instanceof ServerLevel lv) { // vòng xích ánh sáng quanh người
                double r = e.getBbWidth() * 0.7 + 0.3;
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + t * 0.1;
                    lv.sendParticles(CHAIN, e.getX() + Math.cos(a) * r, e.getY() + 0.2 + (k % 2) * e.getBbHeight() * 0.4, e.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    // ---------------------------------------------------------------- E: Thủ Hộ Chi Kiếm

    private static final class Guard { int swords; long until; }

    private static final Map<UUID, Guard> GUARDS = new HashMap<>();

    public static int guardSwords(Player p) {
        Guard g = GUARDS.get(p.getUUID());
        return g == null || p.level().getGameTime() >= g.until ? 0 : g.swords;
    }

    public static void thuHoKiem(ServerLevel lv, ServerPlayer p) {
        Guard g = new Guard();
        g.swords = GUARD_SWORDS;
        g.until = lv.getGameTime() + GUARD_TICKS;
        GUARDS.put(p.getUUID(), g);
        Fx.shell(lv, p, p, 1.4f, SWORD_RGB, GUARD_TICKS);
        lv.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1f, 1.3f);
        lv.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
        p.displayClientMessage(Component.translatable("hukhong.kiem.guard.on", GUARD_SWORDS).withColor(SWORD_RGB), true);
    }

    /** Vị trí thanh kiếm thứ i đang xoay quanh người. */
    private static Vec3 orbit(ServerPlayer p, int i, int n, long t) {
        double a = t * 0.12 + i * Math.PI * 2 / Math.max(1, n);
        return p.position().add(Math.cos(a) * 1.3, 1.0 + Math.sin(t * 0.08 + i) * 0.15, Math.sin(a) * 1.3);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lv)) return;
        Guard g = GUARDS.get(p.getUUID());
        if (g == null) return;
        long t = lv.getGameTime();
        if (t >= g.until || g.swords <= 0) { GUARDS.remove(p.getUUID()); return; }
        if (t % 2 == 0) for (int i = 0; i < g.swords; i++) { // mũi kiếm sáng xoay quanh người
            Vec3 o = orbit(p, i, g.swords, t);
            lv.sendParticles(CHAIN, o.x, o.y, o.z, 1, 0, 0.15, 0, 0);
            lv.sendParticles(ParticleTypes.END_ROD, o.x, o.y + 0.25, o.z, 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lv)) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity a) || a == p) return;
        block(lv, p, a, e);
    }

    /** Đỡ 1 đòn: trả hệ số sát thương còn lại (1 nếu không có kiếm). Gọi tay được trong test (e = null). */
    public static double block(ServerLevel lv, ServerPlayer p, LivingEntity attacker, LivingIncomingDamageEvent e) {
        Guard g = GUARDS.get(p.getUUID());
        if (g == null || lv.getGameTime() >= g.until || g.swords <= 0) return 1;
        Vec3 from = orbit(p, g.swords - 1, g.swords, lv.getGameTime());
        g.swords--;
        if (e != null) e.setAmount((float) (e.getAmount() * (1 - GUARD_BLOCK)));
        lv.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9f, 1.4f);
        lv.sendParticles(ParticleTypes.CRIT, from.x, from.y, from.z, 8, 0.15, 0.15, 0.15, 0.2);
        Vec3 to = attacker.getBoundingBox().getCenter();
        Fx.swordStab(lv, from, to, 5, 0, 1.0f);
        if (attacker instanceof Player) return 1 - GUARD_BLOCK; // không phản PvP
        SkillScheduler.later(5, () -> {
            if (!attacker.isAlive() || p.isRemoved()) return;
            SkillEffects.slashFor(p, attacker, Skill.KIEM_2_COEF);
            lv.sendParticles(ParticleTypes.SWEEP_ATTACK, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        });
        if (g.swords == 0) p.displayClientMessage(Component.translatable("hukhong.kiem.guard.off").withColor(0x9AA8B8), true);
        return 1 - GUARD_BLOCK;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { GUARDS.remove(e.getEntity().getUUID()); }
}
