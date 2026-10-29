package com.hukhong.linhbao;

import com.hukhong.HuKhong;
import com.hukhong.gates.DamageRules;
import com.hukhong.skill.SkillAttachments;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Vòng đời Linh Bảo ở server: tia Đạo Linh, hào quang chiến đấu Thể/Ma, bùng nổ khi lên giai đoạn. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LinhBaoEvents {
    private LinhBaoEvents() {}

    private static final long TARGET_MEMORY = 100, COMBAT_WINDOW = 100;

    private record Tgt(UUID id, long tick) {}

    private static final Map<UUID, Tgt> LAST_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_BOLT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BOLT_COUNT = new ConcurrentHashMap<>();
    private static final Map<UUID, int[]> LAST_STAGE = new ConcurrentHashMap<>();

    /** Nhớ mục tiêu gần nhất của người chơi (đòn thường hoặc kỹ năng đều tính). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHit(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && e.getEntity() != p && !e.isCanceled()) {
            LAST_TARGET.put(p.getUUID(), new Tgt(e.getEntity().getUUID(), p.level().getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        LAST_TARGET.remove(id);
        NEXT_BOLT.remove(id);
        BOLT_COUNT.remove(id);
        LAST_STAGE.remove(id);
        LinhBaoCombat.forget(id);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lv)) return;
        if (!p.isAlive() || p.isSpectator()) return;
        boolean dao = LinhBao.has(p, LinhBaoType.DAO_LINH), the = LinhBao.has(p, LinhBaoType.LINH_THE), ma = LinhBao.has(p, LinhBaoType.MA_CONG);
        if (!dao && !the && !ma) return;
        long now = lv.getGameTime();
        long lastCombat = p.getData(SkillAttachments.STATE).lastCombat;
        boolean inCombat = now - lastCombat <= COMBAT_WINDOW;

        if (p.tickCount % 10 == 3) stageWatch(p, lv, dao, the, ma);
        if (dao && inCombat) daoTick(p, lv, now);
        if (now - lastCombat <= 80) {
            if (the) theAura(p, lv, now);
            if (ma) maAura(p, lv, now);
        }
    }

    // ------------------------------------------------------------------ Đạo Linh

    static boolean validTarget(ServerPlayer p, LivingEntity t) {
        return t != p && t.isAlive() && !t.isSpectator() && !(t instanceof Player) && !(t instanceof ArmorStand)
                && !(t instanceof Npc) && !(t instanceof TamableAnimal ta && p.getUUID().equals(ta.getOwnerUUID()));
    }

    static LivingEntity currentTarget(ServerPlayer p, ServerLevel lv, long now) {
        Tgt t = LAST_TARGET.get(p.getUUID());
        if (t == null || now - t.tick() > TARGET_MEMORY) return null;
        if (!(lv.getEntity(t.id()) instanceof LivingEntity le) || !validTarget(p, le)) return null;
        return p.distanceToSqr(le) <= LinhBaoMath.DAO_BOLT_RANGE * LinhBaoMath.DAO_BOLT_RANGE ? le : null;
    }

    private static void daoTick(ServerPlayer p, ServerLevel lv, long now) {
        Long nb = NEXT_BOLT.get(p.getUUID());
        if (nb != null && now < nb) return;
        LivingEntity t = currentTarget(p, lv, now);
        if (t == null) return;
        NEXT_BOLT.put(p.getUUID(), now + LinhBaoMath.DAO_BOLT_INTERVAL_TICKS);
        fireBolt(p, lv, t);
    }

    /** Công khai để GameTest gọi. Trả sát thương đã gây (trước giáp). */
    public static float fireBolt(ServerPlayer p, ServerLevel lv, LivingEntity t) {
        int st = LinhBao.stage(p, LinhBaoType.DAO_LINH);
        float dmg = (float) LinhBaoMath.daoBolt(StatService.spellPower(p), st);
        Vec3 orb = orbPos(p, lv.getGameTime());
        Vec3 to = t.position().add(0, t.getBbHeight() * 0.55, 0);
        int flight = Math.max(3, Math.min(8, (int) Math.ceil(orb.distanceTo(to) / 3.0)));
        int col = LinhBaoMath.daoColor(st);
        Fx.ribbon(lv, p, orb, to, 0.5, flight, 0, 0.14f, col);
        Fx.ribbon(lv, p, orb, to, -0.3, flight, 0, 0.07f, 0xFFFFFF);
        lv.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 5 + st, 0.2, 0.25, 0.2, 0.06);
        if (st >= 3) Fx.rune(lv, p, t.position().add(0, 0.08, 0), 0.8f + 0.05f * st, col, 14, 6);
        lv.playSound(null, orb.x, orb.y, orb.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.5f, 1.6f + 0.05f * st);
        t.invulnerableTime = 0; // tia tự động không bị khung bất tử của đòn trước nuốt (cùng cách PassiveService)
        DamageRules.hurtBySkill(p, t, dmg);
        int n = BOLT_COUNT.merge(p.getUUID(), 1, Integer::sum);
        if (st >= LinhBaoMath.DAO_HEAL_MIN_STAGE && n % LinhBaoMath.DAO_HEAL_EVERY == 0) healAlly(p, lv, orb, st);
        return dmg;
    }

    /** Chữa đồng đội (người chơi khác / thú cưng của mình) thấp máu nhất trong tầm; không có thì chữa nửa lượng cho bản thân. */
    static void healAlly(ServerPlayer p, ServerLevel lv, Vec3 orb, int st) {
        double r = LinhBaoMath.DAO_HEAL_RANGE;
        List<LivingEntity> c = new ArrayList<>();
        for (Player o : lv.players())
            if (o != p && o.isAlive() && !o.isSpectator() && o.distanceToSqr(p) <= r * r && o.getHealth() < o.getMaxHealth()) c.add(o);
        for (TamableAnimal a : lv.getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(r),
                a -> p.getUUID().equals(a.getOwnerUUID()) && a.isAlive() && a.getHealth() < a.getMaxHealth())) c.add(a);
        float amt = (float) LinhBaoMath.daoHeal(StatService.spellPower(p), st);
        LivingEntity best = null;
        for (LivingEntity x : c) if (best == null || x.getHealth() / x.getMaxHealth() < best.getHealth() / best.getMaxHealth()) best = x;
        if (best == null) {
            if (p.getHealth() >= p.getMaxHealth()) return;
            p.heal(amt * 0.5f);
            return;
        }
        best.heal(amt);
        Vec3 to = best.position().add(0, best.getBbHeight() * 0.6, 0);
        Fx.ribbon(lv, p, orb, to, 0.8, 6, 0, 0.1f, 0xFFF4C8);
        lv.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 6, 0.25, 0.3, 0.25, 0.03);
    }

    public static Vec3 orbPos(ServerPlayer p, long now) {
        double[] o = LinhBaoMath.orbOffset(p.yBodyRot, now);
        return p.position().add(o[0], o[1], o[2]);
    }

    // ------------------------------------------------------------------ hào quang chiến đấu

    private static void theAura(ServerPlayer p, ServerLevel lv, long now) {
        int st = LinhBao.stage(p, LinhBaoType.LINH_THE);
        if (now % Math.max(12, 30 - 3 * st) != 0) return;
        // gân vàng mờ bám thân, đậm/rộng dần theo giai đoạn
        Fx.shell(lv, p, p, 0.95f + 0.04f * st, LinhBaoMath.theColor(st), 14 + st);
    }

    private static void maAura(ServerPlayer p, ServerLevel lv, long now) {
        int st = LinhBao.stage(p, LinhBaoType.MA_CONG);
        if (now % 40 == 0) Fx.rune(lv, p, p.position().add(0, 0.06, 0), 1.2f + 0.08f * st, LinhBaoMath.maColor(st), 36, 5 + st / 3);
        if (st >= 3 && now % 80 == 20) Fx.groundRing(lv, p, p.position().add(0, 0.05, 0), 1.6f + 0.1f * st, LinhBaoMath.maColor(st), 24);
    }

    // ------------------------------------------------------------------ lên giai đoạn

    private static void stageWatch(ServerPlayer p, ServerLevel lv, boolean dao, boolean the, boolean ma) {
        int[] cur = {dao ? LinhBao.stage(p, LinhBaoType.DAO_LINH) : -1, the ? LinhBao.stage(p, LinhBaoType.LINH_THE) : -1,
                ma ? LinhBao.stage(p, LinhBaoType.MA_CONG) : -1};
        int[] prev = LAST_STAGE.put(p.getUUID(), cur);
        if (prev == null) return; // lần đầu thấy (đăng nhập): không bùng nổ
        if (cur[0] > prev[0] && prev[0] >= 0) {
            Fx.upgradeBurst(p, LinhBaoMath.daoColor(cur[0]));
            Fx.rune(lv, p, p.position().add(0, 2.4, 0), 1.5f, 0xFFD866, 40, 8);
        }
        if (cur[1] > prev[1] && prev[1] >= 0) { // rèn thân: vỏ sáng + vòng nở dưới chân + tiếng búa
            Fx.upgradeBurst(p, LinhBaoMath.theColor(cur[1]));
            Fx.shell(lv, p, p, 1.5f, LinhBaoMath.theColor(cur[1]), 30);
            Fx.groundRing(lv, p, p.position().add(0, 0.05, 0), 2.4f, LinhBaoMath.theColor(cur[1]), 26);
            lv.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.7f);
        }
        if (cur[2] > prev[2] && prev[2] >= 0) {
            Fx.upgradeBurst(p, LinhBaoMath.maColor(cur[2]));
            Fx.rune(lv, p, p.position().add(0, 0.06, 0), 2.2f, LinhBaoMath.maColor(cur[2]), 50, 7);
            lv.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.3f, 1.4f);
        }
    }
}
