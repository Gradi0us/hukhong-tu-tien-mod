package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đạo Tu — Thánh Quang (2026-11-06): tích tầng trên bản thân, tối đa 20.
 * +1 tầng khi: hồi máu cho bản thân / đồng minh gần (tối đa 1 tầng mỗi giây), hạ gục 1 kẻ địch, đánh trúng đủ 10 đòn, dùng đủ 5 kỹ năng.
 * Đủ 20 tầng: đòn kế tiếp lên kẻ địch kích nổ Thánh Quang = 30% máu tối đa mục tiêu (trần 20 x Phép lực), rồi về 0.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DaoThanhQuang {
    private DaoThanhQuang() {}

    public static final int MAX = 20, HITS_PER = 10, CASTS_PER = 5, HEAL_GAP = 20;
    public static final double BURST_PCT = 0.30, BURST_CAP_SP = 20, ALLY_RADIUS = 12;

    private static final class St { int stacks, hits, casts; long lastHeal = -100; }

    private static final Map<UUID, St> STATE = new HashMap<>();
    private static boolean bursting;

    private static St st(Player p) { return STATE.computeIfAbsent(p.getUUID(), k -> new St()); }

    public static int stacks(Player p) { St s = STATE.get(p.getUUID()); return s == null ? 0 : s.stacks; }

    static boolean isDao(Player p) { return p.getData(ModAttachments.CULTIVATION).paths().contains(Path.DAO); }

    /** Cộng n tầng (kẹp MAX); báo khi đủ. */
    public static void add(ServerPlayer p, int n) {
        St s = st(p);
        int before = s.stacks;
        s.stacks = Math.min(MAX, s.stacks + n);
        if (s.stacks == before) return;
        if (s.stacks == MAX) {
            p.displayClientMessage(Component.translatable("hukhong.dao.thanh_quang.full").withColor(0xFFE27A), true);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.6f);
        } else if (s.stacks % 5 == 0) p.displayClientMessage(Component.translatable("hukhong.dao.thanh_quang.stack", s.stacks, MAX).withColor(0xFFE9A0), true);
    }

    /** Gọi từ SkillService sau mỗi lần dùng kỹ năng. */
    public static void onCast(ServerPlayer p) {
        if (!isDao(p)) return;
        St s = st(p);
        if (++s.casts >= CASTS_PER) { s.casts = 0; add(p, 1); }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer healed) || e.getAmount() <= 0 || healed.getHealth() >= healed.getMaxHealth()) return;
        if (!(healed.level() instanceof ServerLevel lv)) return;
        ServerPlayer dao = isDao(healed) ? healed : null;
        if (dao == null) for (ServerPlayer o : lv.players())
            if (o != healed && isDao(o) && o.distanceToSqr(healed) <= ALLY_RADIUS * ALLY_RADIUS) { dao = o; break; }
        if (dao == null) return;
        St s = st(dao);
        long now = lv.getGameTime();
        if (now - s.lastHeal < HEAL_GAP) return;
        s.lastHeal = now;
        add(dao, 1);
    }

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post e) {
        if (bursting || !(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() instanceof Player || !isDao(p)) return;
        if (e.getNewDamage() <= 0) return;
        St s = st(p);
        if (s.stacks >= MAX) { burst(p, e.getEntity()); return; }
        if (++s.hits >= HITS_PER) { s.hits = 0; add(p, 1); }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && !(e.getEntity() instanceof Player) && isDao(p)) add(p, 1);
    }

    /** Kích nổ Thánh Quang lên t (test gọi tay). Trả sát thương. */
    public static float burst(ServerPlayer p, LivingEntity t) {
        St s = st(p);
        if (s.stacks < MAX || !(p.level() instanceof ServerLevel lv) || !t.isAlive()) return 0;
        s.stacks = 0;
        s.hits = 0;
        float dmg = (float) Math.min(t.getMaxHealth() * BURST_PCT, BURST_CAP_SP * com.hukhong.stats.StatService.spellPower(p));
        bursting = true;
        try {
            t.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, t, dmg);
        } finally {
            bursting = false;
        }
        Vec3 c = t.position();
        Fx.play(lv, FxType.LIGHT_PILLAR, c, Vec3.ZERO, Math.max(1.2f, t.getBbWidth() + 0.6f), Fx.GOLD, 24);
        Fx.groundRing(lv, p, c.add(0, 0.05, 0), Math.max(1.5f, t.getBbWidth() + 1f), 0xFFE27A, 20);
        lv.sendParticles(ParticleTypes.END_ROD, c.x, c.y + t.getBbHeight() * 0.5, c.z, 30, 0.4, 0.8, 0.4, 0.12);
        lv.playSound(null, t.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.4f);
        p.displayClientMessage(Component.translatable("hukhong.dao.thanh_quang.burst", Math.round(dmg)).withColor(0xFFE27A), true);
        return dmg;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0 || !(p.level() instanceof ServerLevel lv)) return;
        int n = stacks(p);
        if (n >= MAX) lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 2.1, p.getZ(), 2, 0.3, 0.05, 0.3, 0.01);
        else if (n >= 10 && p.tickCount % 20 == 0) lv.sendParticles(ParticleTypes.WAX_OFF, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0.3, 0.4, 0.3, 0);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { STATE.remove(e.getEntity().getUUID()); }
}
