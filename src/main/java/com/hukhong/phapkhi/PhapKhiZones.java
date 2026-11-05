package com.hukhong.phapkhi;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.gates.DamageRules;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Trận Pháp Khí (Trận Bàn): vùng trận tồn tại N giây tại điểm đặt. Runtime nhỏ tự chứa: danh sách vùng trong bộ nhớ,
 * chủ giữ theo tham chiếu (ServerPlayer; null nếu mất), dọn ở ServerStoppingEvent. Vẽ vòng phù văn bằng decal Fx
 * làm tươi mỗi vài tick (giống tran/TranVfx.zoneDecal).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhapKhiZones {
    private PhapKhiZones() {}

    public static final double[] RADIUS = {4, 5, 6, 7, 8};
    public static final int[] SECONDS = {20, 25, 30, 40, 60};
    /** Tụ Linh: % linh lực tối đa / giây và % máu tối đa / giây. */
    public static final double[] RESOURCE_PCT = {0.02, 0.03, 0.04, 0.05, 0.07};
    public static final double[] HP_PCT = {0.01, 0.015, 0.02, 0.03, 0.04};
    /** Sát: hệ số x lực (max Công/Phép lúc đặt) mỗi giây. */
    public static final double[] SAT_COEF = {0.4, 0.6, 0.9, 1.3, 1.8};
    /** Hộ Trận: giảm sát thương cho người trong trận. */
    public static final double[] HO_PCT = {0.25, 0.30, 0.35, 0.40, 0.50};
    /** Khốn: chậm 50% (modifier) + trói 1 giây khi vào. */
    public static final double KHON_SLOW = 0.5;
    public static final int KHON_ROOT_TICKS = 20;
    public static final int MAX_PER_OWNER = 3;
    private static final ResourceLocation SLOW_ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "phapkhi_khon_slow");

    private static final class Zone {
        PhapKhiKind kind;
        int tier;
        ResourceKey<Level> dim;
        Vec3 c;
        double r;
        long start, end;
        ServerPlayer owner;      // tham chiếu, có thể không còn hợp lệ
        double power;            // lực của chủ lúc đặt (Sát)
        final Set<Integer> inside = new HashSet<>(); // Khốn: id địch đang trong trận
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    public static int count() { return ZONES.size(); }

    /** Đặt trận tại điểm nhìn trúng (tầm 8) hoặc dưới chân. */
    static boolean place(ServerPlayer p, PhapKhiKind kind, PhapKhiTier tier) {
        if (!(p.level() instanceof ServerLevel lv)) return false;
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(8));
        var hit = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 c = hit.getType() == HitResult.Type.MISS ? PhapKhiAttack.ground(lv, p.position()) : hit.getLocation();
        if (hit.getType() != HitResult.Type.MISS) c = PhapKhiAttack.ground(lv, c.add(0, 0.2, 0));
        place(lv, p, kind, tier, c);
        return true;
    }

    /** Đặt trận ở điểm c (GameTest và mã khác gọi trực tiếp). */
    static void place(ServerLevel lv, ServerPlayer owner, PhapKhiKind kind, PhapKhiTier tier, Vec3 c) {
        List<Zone> mine = new ArrayList<>();
        for (Zone o : ZONES) if (o.owner == owner) mine.add(o);
        while (mine.size() >= MAX_PER_OWNER) { // trận cũ nhất của chủ bị thay
            Zone old = mine.remove(0);
            release(lv.getServer(), old);
            ZONES.remove(old);
        }
        Zone z = new Zone();
        z.kind = kind;
        z.tier = tier.ordinal();
        z.dim = lv.dimension();
        z.c = c;
        z.r = RADIUS[z.tier];
        z.start = lv.getGameTime();
        z.end = z.start + SECONDS[z.tier] * 20L;
        z.owner = owner;
        z.power = PhapKhiAttack.power(owner);
        ZONES.add(z);
        placeFx(lv, z);
    }

    private static void placeFx(ServerLevel lv, Zone z) {
        int rgb = z.kind.rgb;
        Vec3 g = z.c.add(0, 0.05, 0);
        Fx.ribbon(lv, z.owner, z.c.add(0, 10, 0), z.c.add(0, 0.2, 0), 0, 6, 0, 0.35f, rgb);
        Fx.groundRing(lv, z.owner, g, (float) z.r * 1.15f, 0xFFFFFF, 30);
        Fx.shake(lv, z.c, 0.4f, 6, 14);
        drawRune(lv, z);
        lv.playSound(null, z.c.x, z.c.y, z.c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.3f);
        lv.playSound(null, z.c.x, z.c.y, z.c.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
    }

    /** Vòng phù văn bền: hai lớp rune (số điểm khác nhau) + vòng đất viền + vài hạt sáng ở mép. */
    private static void drawRune(ServerLevel lv, Zone z) {
        int rgb = z.kind.rgb;
        Vec3 g = z.c.add(0, 0.05, 0);
        Fx.rune(lv, z.owner, g, (float) z.r, rgb, 16, 6);
        Fx.rune(lv, z.owner, g.add(0, 0.01, 0), (float) z.r * 0.6f, 0xFFFFFF, 16, 4);
        Fx.groundRing(lv, z.owner, g, (float) z.r, rgb, 14);
        double rot = lv.getGameTime() * 0.05;
        for (int i = 0; i < 8; i++) {
            double a = rot + Math.PI * 2 * i / 8;
            lv.sendParticles(ParticleTypes.END_ROD, z.c.x + Math.cos(a) * z.r, z.c.y + 0.2, z.c.z + Math.sin(a) * z.r, 1, 0, 0.3, 0, 0.01);
        }
    }

    private static boolean in(Zone z, Entity e) {
        double dx = e.getX() - z.c.x, dz = e.getZ() - z.c.z, dy = e.getY() - z.c.y;
        return dx * dx + dz * dz <= z.r * z.r && dy > -5 && dy < 6;
    }

    private static boolean enemy(LivingEntity e) {
        return e instanceof Enemy && !(e instanceof Player) && e.isAlive();
    }

    /** Hệ số sát thương (<1) mà các Hộ Trận đang bao quanh người chơi áp lên. */
    public static double damageFactor(Player p) {
        double f = 1.0;
        for (Zone z : ZONES) {
            if (z.kind != PhapKhiKind.HO_TRAN || !z.dim.equals(p.level().dimension()) || !in(z, p)) continue;
            f = Math.min(f, 1.0 - HO_PCT[z.tier]);
        }
        return f;
    }

    static String debug(ServerPlayer p) {
        StringBuilder b = new StringBuilder("zones=" + ZONES.size() + " owner=" + p.position());
        for (Zone z : ZONES) b.append(" [").append(z.kind).append(" c=").append(z.c).append(" r=").append(z.r).append(" end=").append(z.end).append(']');
        return b.toString();
    }

    /** Địch đang bị Khốn Trận làm chậm 50%? (GameTest) */
    static boolean isSlowed(LivingEntity m) {
        AttributeInstance sp = m.getAttribute(Attributes.MOVEMENT_SPEED);
        return sp != null && sp.hasModifier(SLOW_ID);
    }

    /** Vùng loại kind đang chứa e? (GameTest). */
    static boolean hasZone(PhapKhiKind kind, Entity e) {
        for (Zone z : ZONES) if (z.kind == kind && z.dim.equals(e.level().dimension()) && in(z, e)) return true;
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (ZONES.isEmpty()) return;
        MinecraftServer srv = e.getServer();
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            ServerLevel lv = srv.getLevel(z.dim);
            if (lv == null) { it.remove(); continue; }
            long now = lv.getGameTime();
            if (now >= z.end) {
                release(srv, z);
                Fx.groundRing(lv, z.owner, z.c.add(0, 0.05, 0), (float) z.r * 1.2f, 0xFFFFFF, 14);
                lv.playSound(null, z.c.x, z.c.y, z.c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
                it.remove();
                continue;
            }
            long age = now - z.start;
            if (age % 8 == 0) drawRune(lv, z);
            if (age % 5 == 0 && z.kind == PhapKhiKind.KHON) tickKhon(lv, z);
            if (age > 0 && age % 20 == 0) {
                switch (z.kind) {
                    case TU_LINH -> tickTuLinh(lv, z);
                    case SAT -> tickSat(lv, z);
                    default -> { }
                }
            }
        }
    }

    private static List<LivingEntity> inside(ServerLevel lv, Zone z) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(z.c, z.c).inflate(z.r, 4, z.r)))
            if (in(z, e)) out.add(e);
        if (z.owner != null && !out.contains(z.owner) && z.owner.level() == lv && in(z, z.owner)) out.add(z.owner); // chủ giữ theo tham chiếu
        return out;
    }

    private static void tickKhon(ServerLevel lv, Zone z) {
        Set<Integer> now = new HashSet<>();
        for (LivingEntity m : inside(lv, z)) {
            if (!enemy(m)) continue;
            now.add(m.getId());
            AttributeInstance sp = m.getAttribute(Attributes.MOVEMENT_SPEED);
            if (sp != null && !sp.hasModifier(SLOW_ID))
                sp.addTransientModifier(new AttributeModifier(SLOW_ID, -KHON_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (z.inside.add(m.getId())) { // vừa vào: trói 1 giây
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, KHON_ROOT_TICKS, 7, false, true));
                lv.sendParticles(ParticleTypes.ENCHANT, m.getX(), m.getY() + 1, m.getZ(), 20, 0.4, 0.6, 0.4, 0.5);
                Fx.groundRing(lv, z.owner, m.position().add(0, 0.06, 0), 1.2f, z.kind.rgb, 20);
                lv.playSound(null, m.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 0.8f);
            }
        }
        for (Iterator<Integer> it = z.inside.iterator(); it.hasNext(); ) {
            int id = it.next();
            if (now.contains(id)) continue;
            it.remove();
            if (lv.getEntity(id) instanceof LivingEntity m) clearSlow(m);
        }
    }

    private static void clearSlow(LivingEntity m) {
        AttributeInstance sp = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.removeModifier(SLOW_ID);
    }

    private static void release(MinecraftServer srv, Zone z) {
        ServerLevel lv = srv == null ? null : srv.getLevel(z.dim);
        if (lv == null) return;
        for (int id : z.inside) if (lv.getEntity(id) instanceof LivingEntity m) clearSlow(m);
        z.inside.clear();
    }

    private static void tickTuLinh(ServerLevel lv, Zone z) {
        for (LivingEntity m : inside(lv, z)) {
            if (!(m instanceof ServerPlayer sp)) continue;
            CultivationData d = sp.getData(ModAttachments.CULTIVATION);
            int add = Math.max(1, (int) Math.round(d.resourceMax() * RESOURCE_PCT[z.tier]));
            if (d.resource() < d.resourceMax()) sp.setData(ModAttachments.CULTIVATION, d.withResource(Math.min(d.resourceMax(), d.resource() + add)));
            sp.heal((float) (sp.getMaxHealth() * HP_PCT[z.tier]));
            lv.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1, sp.getZ(), 12, 0.4, 0.7, 0.4, 0.4);
            lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, sp.getX(), sp.getY() + 1, sp.getZ(), 4, 0.4, 0.7, 0.4, 0.05);
        }
    }

    private static void tickSat(ServerLevel lv, Zone z) {
        for (LivingEntity m : inside(lv, z)) {
            if (!enemy(m)) continue;
            float dmg = (float) (SAT_COEF[z.tier] * z.power);
            m.invulnerableTime = 0;
            if (z.owner != null && z.owner.isAlive() && !z.owner.hasDisconnected()) DamageRules.hurtBySkill(z.owner, m, dmg);
            else m.hurt(lv.damageSources().magic(), dmg);
            lv.sendParticles(ParticleTypes.CRIT, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 12, 0.3, 0.4, 0.3, 0.4);
            Fx.ribbon(lv, z.owner, m.position().add(0, 6, 0), m.position().add(0, 0.5, 0), 0, 4, 0, 0.12f, z.kind.rgb);
        }
        lv.playSound(null, z.c.x, z.c.y, z.c.z, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.3f, 1.8f);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        for (Zone z : ZONES) release(e.getServer(), z);
        ZONES.clear();
    }
}
