package com.hukhong.pvp;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.UtilityPillItem;
import com.hukhong.city.CityDef;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import com.hukhong.events.CityStates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Dau Truong Sinh Tu (PLAN-v2 s11.1). Mot tran tai mot thoi diem. Hai ben cuoc bang nhau (giu trong vi), dot 5%, nguoi thang nhan phan con lai;
 * khan gia cuoc pari-mutuel. 3 hiep x 3 phut; con 1 mau la thua hiep (huy chet, khong roi do, khong mat EXP); cam dan hoi phuc;
 * can bang cap tuy chon; toi da 3 tran / cap / ngay Lich. HUD = thanh actionbar tu server (khong co code client).
 * Cac ben co the la nguoi choi hoac NPC tan tu (giai 8 nguoi, {@link Bracket}).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DuelManager {
    private DuelManager() {}

    public static final int CHALLENGE_TTL = 1200, NEAR_RANGE = 64, LEAVE_RANGE = 40;
    public static final long MIN_BET = 10;

    public enum Phase { COUNTDOWN, FIGHT, BREAK }

    private record Challenge(UUID from, long stake, boolean balance, long expires) {}

    static final class Match {
        final ServerLevel level;
        final BlockPos arena;
        final LivingEntity a, b;            // ben +1 (A) va -1 (B)
        final long escrowA, escrowB;
        final boolean balance;
        final int lvA, lvB, roundTicks;
        final List<PvpMath.Bet> bets = new ArrayList<>();
        final IntConsumer onEnd;
        Vec3 retA, retB;
        int winsA, winsB, round, timer;
        double hpSumA, hpSumB;
        Phase phase = Phase.COUNTDOWN;
        Integer pendingLoser; // hiep ket thuc o tick ke tiep (tranh doi mau giua luc dang ap sat thuong)

        Match(ServerLevel l, BlockPos arena, LivingEntity a, LivingEntity b, long eA, long eB, boolean balance, int lvA, int lvB, int roundTicks, IntConsumer onEnd) {
            level = l; this.arena = arena; this.a = a; this.b = b; escrowA = eA; escrowB = eB; this.balance = balance;
            this.lvA = lvA; this.lvB = lvB; this.roundTicks = roundTicks; this.onEnd = onEnd;
        }

        long pot() { long s = escrowA + escrowB; for (PvpMath.Bet x : bets) s += x.amount(); return s; }
    }

    private static final Map<UUID, Challenge> PENDING = new HashMap<>();
    private static Match cur;
    private static boolean allowHeal;

    // ------------------------------------------------------------------ truy van

    public static boolean busy() { return cur != null; }

    public static boolean inMatch(Entity e) { return cur != null && sideOf(cur, e) != 0; }

    /** Dau Truong mo: da dung va Thien Khai Hoang Do khong o trang thai RUIN. */
    public static boolean open(ServerLevel ow) {
        PvpData d = PvpData.get(ow);
        CityDef city = CityDef.ALL.get(ArenaBuilder.CITY_INDEX);
        return d.arena() != null && com.hukhong.city.CityService.builtCenter(ow, city) != null && !CityStates.isRuin(ow, city);
    }

    private static int sideOf(Match m, Entity e) {
        if (e == null) return 0;
        if (e == m.a || e.getUUID().equals(m.a.getUUID())) return 1;
        if (e == m.b || e.getUUID().equals(m.b.getUUID())) return -1;
        return 0;
    }

    private static LivingEntity ent(Match m, int side) { return side > 0 ? m.a : m.b; }

    // ------------------------------------------------------------------ thach dau

    private static boolean nearArena(ServerPlayer p, BlockPos arena) {
        return p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD
                && p.distanceToSqr(arena.getX(), arena.getY(), arena.getZ()) <= (double) NEAR_RANGE * NEAR_RANGE;
    }

    /** @return null neu OK, hoac key loi. */
    public static String challenge(ServerPlayer from, ServerPlayer to, long stake, boolean balance) {
        ServerLevel ow = from.getServer().overworld();
        if (!open(ow)) return "hukhong.pvp.err.closed";
        if (from == to) return "hukhong.pvp.err.self";
        if (cur != null || Bracket.running()) return "hukhong.pvp.err.busy";
        BlockPos arena = PvpData.get(ow).arena();
        if (!nearArena(from, arena) || !nearArena(to, arena)) return "hukhong.pvp.err.far";
        long min = PvpMath.minStake(Math.max(CultivationService.get(from).level(), CultivationService.get(to).level()));
        if (stake < min) return "hukhong.pvp.err.stake_min";
        if (Wallet.count(from) < stake || Wallet.count(to) < stake) return "hukhong.pvp.err.funds";
        if (!PvpMath.pairAllowed(PvpData.get(ow).pairCount(PvpMath.pairKey(from.getUUID(), to.getUUID(), ow.getGameTime())))) return "hukhong.pvp.err.pair";
        PENDING.put(to.getUUID(), new Challenge(from.getUUID(), stake, balance, ow.getGameTime() + CHALLENGE_TTL));
        to.sendSystemMessage(Component.translatable("hukhong.pvp.challenged", from.getDisplayName(), TuTinhText.of(stake)));
        to.sendSystemMessage(Component.translatable("hukhong.pvp.challenge_hint"));
        return null;
    }

    public static String accept(ServerPlayer to) {
        ServerLevel ow = to.getServer().overworld();
        Challenge c = PENDING.remove(to.getUUID());
        if (c == null || c.expires() < ow.getGameTime()) return "hukhong.pvp.err.no_challenge";
        ServerPlayer from = to.getServer().getPlayerList().getPlayer(c.from());
        if (from == null) return "hukhong.pvp.err.no_challenge";
        if (!open(ow)) return "hukhong.pvp.err.closed";
        if (cur != null || Bracket.running()) return "hukhong.pvp.err.busy";
        BlockPos arena = PvpData.get(ow).arena();
        if (!nearArena(from, arena) || !nearArena(to, arena)) return "hukhong.pvp.err.far";
        PvpData pd = PvpData.get(ow);
        String key = PvpMath.pairKey(from.getUUID(), to.getUUID(), ow.getGameTime());
        if (!PvpMath.pairAllowed(pd.pairCount(key))) return "hukhong.pvp.err.pair";
        if (!Wallet.pay(from, c.stake())) return "hukhong.pvp.err.funds";
        if (!Wallet.pay(to, c.stake())) { Wallet.give(from, c.stake()); return "hukhong.pvp.err.funds"; }
        pd.bumpPair(key, ow.getGameTime());
        start(ow, arena, from, to, c.stake(), c.stake(), c.balance(), PvpMath.ROUND_TICKS, null);
        return null;
    }

    public static boolean decline(ServerPlayer to) {
        Challenge c = PENDING.remove(to.getUUID());
        if (c == null) return false;
        ServerPlayer from = to.getServer().getPlayerList().getPlayer(c.from());
        if (from != null) from.sendSystemMessage(Component.translatable("hukhong.pvp.declined", to.getDisplayName()));
        return true;
    }

    /** Khan gia cuoc cho ben A/B (side +1/-1) trong hiep 1. @return null neu OK, hoac key loi. */
    public static String bet(ServerPlayer p, int side, long amount) {
        Match m = cur;
        if (m == null) return "hukhong.pvp.err.no_match";
        if (sideOf(m, p) != 0) return "hukhong.pvp.err.bet_participant";
        if (m.round > 1) return "hukhong.pvp.err.bet_closed";
        if (amount < MIN_BET) return "hukhong.pvp.err.bet_min";
        if (!nearArena(p, m.arena)) return "hukhong.pvp.err.far";
        if (!Wallet.pay(p, amount)) return "hukhong.pvp.err.funds";
        m.bets.add(new PvpMath.Bet(p.getUUID(), side, amount));
        p.sendSystemMessage(Component.translatable("hukhong.pvp.bet_ok", TuTinhText.of(amount), ent(m, side).getDisplayName()));
        return null;
    }

    // ------------------------------------------------------------------ bat dau / hiep

    /** Bat dau tran (da escrow xong). Cung duoc Bracket goi voi escrow 0 + callback ket qua (1 = A thang, -1 = B, 0 = hoa). */
    static void start(ServerLevel ow, BlockPos arena, LivingEntity a, LivingEntity b, long eA, long eB, boolean balance, int roundTicks, IntConsumer onEnd) {
        int lvA = a instanceof ServerPlayer pa ? CultivationService.get(pa).level() : 0;
        int lvB = b instanceof ServerPlayer pb ? CultivationService.get(pb).level() : 0;
        Match m = new Match(ow, arena, a, b, eA, eB, balance && lvA > 0 && lvB > 0, lvA, lvB, roundTicks, onEnd);
        m.retA = a.position();
        m.retB = b.position();
        cur = m;
        if (m.balance && Math.abs(lvA - lvB) > PvpMath.BALANCE_GAP) {
            Component low = (lvA < lvB ? a : b).getDisplayName();
            broadcastNear(m, Component.translatable("hukhong.pvp.balance", low, (int) Math.round((PvpMath.balanceMul(Math.abs(lvA - lvB), true) - 1) * 100)));
        }
        broadcastNear(m, Component.translatable("hukhong.pvp.begin", a.getDisplayName(), b.getDisplayName(), TuTinhText.of(m.escrowA + m.escrowB)));
        beginRound(m);
    }

    private static void beginRound(Match m) {
        m.round++;
        m.phase = Phase.COUNTDOWN;
        m.timer = PvpMath.COUNTDOWN_TICKS;
        place(m.a, ArenaBuilder.spotA(m.arena), -90f);
        place(m.b, ArenaBuilder.spotB(m.arena), 90f);
        allowHeal = true;
        try { m.a.setHealth(m.a.getMaxHealth()); m.b.setHealth(m.b.getMaxHealth()); } finally { allowHeal = false; }
        m.a.setAbsorptionAmount(0f);
        m.b.setAbsorptionAmount(0f);
        title(m, Component.translatable("hukhong.pvp.round", m.round, PvpMath.ROUNDS), Component.translatable("hukhong.pvp.round_sub", m.winsA, m.winsB));
    }

    private static void place(LivingEntity e, BlockPos p, float yaw) {
        if (e instanceof ServerPlayer sp) sp.teleportTo((ServerLevel) sp.level(), p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0f);
        else { e.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0f); if (e instanceof Mob mob) mob.getNavigation().stop(); }
        e.setDeltaMovement(Vec3.ZERO);
        e.fallDistance = 0;
    }

    private static void endRound(Match m, int loserSide) {
        if (m.phase != Phase.FIGHT) return;
        m.hpSumA += ratio(m.a);
        m.hpSumB += ratio(m.b);
        if (loserSide != 0) {
            if (loserSide < 0) m.winsA++; else m.winsB++;
            broadcastNear(m, Component.translatable("hukhong.pvp.round_won", ent(m, -loserSide).getDisplayName(), m.round));
        } else broadcastNear(m, Component.translatable("hukhong.pvp.round_draw", m.round));
        if (m.winsA >= PvpMath.WINS_NEEDED || m.winsB >= PvpMath.WINS_NEEDED || m.round >= PvpMath.ROUNDS) {
            finish(m, PvpMath.matchWinner(m.winsA, m.winsB, m.hpSumA, m.hpSumB));
        } else {
            m.phase = Phase.BREAK;
            m.timer = PvpMath.BREAK_TICKS;
        }
    }

    private static double ratio(LivingEntity e) { return e.getMaxHealth() <= 0 ? 0 : e.getHealth() / e.getMaxHealth(); }

    /** Ket thuc tran: tra tien, dua nguoi ve cho cu. winner: 1 = A, -1 = B, 0 = hoa. */
    private static void finish(Match m, int winner) {
        if (cur != m) return;
        cur = null;
        MinecraftServer server = m.level.getServer();
        long total = m.escrowA + m.escrowB;
        if (winner == 0) {
            if (m.escrowA > 0) PvpData.credit(server, m.a.getUUID(), m.escrowA);
            if (m.escrowB > 0) PvpData.credit(server, m.b.getUUID(), m.escrowB);
            broadcastNear(m, Component.translatable("hukhong.pvp.draw"));
        } else {
            long prize = total - PvpMath.burn(total);
            LivingEntity w = ent(m, winner);
            if (total > 0 && w instanceof ServerPlayer) PvpData.credit(server, w.getUUID(), prize);
            broadcastNear(m, Component.translatable("hukhong.pvp.match_won", w.getDisplayName(), TuTinhText.of(prize), TuTinhText.of(PvpMath.burn(total))));
        }
        PvpMath.Settlement s = PvpMath.settle(m.bets, winner);
        s.pay().forEach((who, amt) -> PvpData.credit(server, who, amt));
        if (!m.bets.isEmpty())
            broadcastNear(m, Component.translatable("hukhong.pvp.bets_settled", TuTinhText.of(s.paid()), TuTinhText.of(s.burned())));
        restore(m.a, m.retA, m);
        restore(m.b, m.retB, m);
        if (m.onEnd != null) m.onEnd.accept(winner);
    }

    private static void restore(LivingEntity e, Vec3 ret, Match m) {
        if (e instanceof ServerPlayer sp) {
            if (sp.isAlive() && !sp.hasDisconnected() && sp.level() == m.level && ret != null) {
                sp.teleportTo(m.level, ret.x, ret.y, ret.z, sp.getYRot(), sp.getXRot());
                allowHeal = true;
                try { sp.setHealth(sp.getMaxHealth()); } finally { allowHeal = false; }
            }
        } else if (!e.isRemoved()) e.discard();
    }

    private static void forfeit(Match m, int side) {
        broadcastNear(m, Component.translatable("hukhong.pvp.forfeit", ent(m, side).getDisplayName()));
        finish(m, -side);
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().overworld();
        if (ow == null) return;
        long t = ow.getGameTime();
        if (t % 40 == 0 && !PENDING.isEmpty()) PENDING.values().removeIf(c -> c.expires() < t);
        if (t % 100 == 59) ensureArena(ow);
        Bracket.tick(ow);
        Match m = cur;
        if (m == null) return;
        for (int side : new int[]{1, -1}) {
            LivingEntity x = ent(m, side);
            boolean gone = x.isRemoved() || !x.isAlive() || x.level() != m.level
                    || (x instanceof ServerPlayer sp && sp.hasDisconnected())
                    || x.distanceToSqr(m.arena.getX(), m.arena.getY(), m.arena.getZ()) > (double) LEAVE_RANGE * LEAVE_RANGE;
            if (gone) { forfeit(m, side); return; }
        }
        if (m.pendingLoser != null) { int l = m.pendingLoser; m.pendingLoser = null; endRound(m, l); if (cur != m) return; }
        if (--m.timer <= 0) {
            switch (m.phase) {
                case COUNTDOWN -> {
                    m.phase = Phase.FIGHT;
                    m.timer = m.roundTicks;
                    title(m, Component.translatable("hukhong.pvp.fight"), Component.empty());
                    aim(m);
                }
                case FIGHT -> endRound(m, -PvpMath.timeoutWinner(ratio(m.a), ratio(m.b)));
                case BREAK -> beginRound(m);
            }
            if (cur != m) return;
        }
        if (m.phase == Phase.FIGHT && t % 20 == 0) aim(m);
        if (t % 10 == 0) hud(m);
    }

    /** NPC tan tu luon nham ben kia. */
    private static void aim(Match m) {
        if (m.a instanceof Mob ma && ma.getTarget() != m.b) ma.setTarget(m.b);
        if (m.b instanceof Mob mb && mb.getTarget() != m.a) mb.setTarget(m.a);
    }

    private static void hud(Match m) {
        int secs = Math.max(0, m.timer / 20);
        Component c = Component.translatable("hukhong.pvp.hud", m.round, PvpMath.ROUNDS, secs,
                m.a.getDisplayName(), (int) Math.round(ratio(m.a) * 100), m.b.getDisplayName(), (int) Math.round(ratio(m.b) * 100),
                TuTinhText.of(m.pot()));
        for (ServerPlayer p : m.level.players())
            if (nearArena(p, m.arena)) p.displayClientMessage(c, true);
    }

    private static void broadcastNear(Match m, Component c) {
        for (ServerPlayer p : m.level.players()) if (nearArena(p, m.arena)) p.sendSystemMessage(c);
    }

    private static void title(Match m, Component t, Component sub) {
        for (ServerPlayer p : m.level.players()) {
            if (!nearArena(p, m.arena)) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 10));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.connection.send(new ClientboundSetTitleTextPacket(t));
        }
    }

    // ------------------------------------------------------------------ san + NPC

    private static void ensureArena(ServerLevel ow) {
        PvpData d = PvpData.get(ow);
        CityDef city = CityDef.ALL.get(ArenaBuilder.CITY_INDEX);
        BlockPos cc = com.hukhong.city.CityService.builtCenter(ow, city);
        if (cc == null) return;
        int[] xz = ArenaBuilder.siteXZ(cc);
        if (d.arena() == null) {
            boolean near = false;
            for (ServerPlayer p : ow.players()) if (Math.abs(p.getX() - xz[0]) < 96 && Math.abs(p.getZ() - xz[1]) < 96) { near = true; break; }
            if (!near) return;
            d.setArena(ArenaBuilder.build(ow, cc));
            ArenaMaster.ensure(ow, d.arena());
        } else if (ow.getGameTime() % 600 == 59) {
            for (ServerPlayer p : ow.players()) if (nearArena(p, d.arena())) { ArenaMaster.ensure(ow, d.arena()); break; }
        }
    }

    // ------------------------------------------------------------------ su kien

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        Match m = cur;
        if (m == null) return;
        LivingEntity v = e.getEntity();
        int vs = sideOf(m, v);
        int as = sideOf(m, e.getSource().getEntity());
        if (vs != 0) {
            if (m.phase != Phase.FIGHT || as != -vs) { e.setCanceled(true); return; }
            // can bang cap: ben thap hon gay them sat thuong
            if (m.balance) {
                int diff = Math.abs(m.lvA - m.lvB);
                boolean attackerLow = (as > 0 ? m.lvA : m.lvB) < (as > 0 ? m.lvB : m.lvA);
                if (attackerLow) e.setAmount((float) (e.getAmount() * PvpMath.balanceMul(diff, true)));
            }
        } else if (as != 0) e.setCanceled(true); // nguoi dau khong duoc danh ngoai cuoc
    }

    /** Sat thuong cuoi cung: khong bao gio de ve duoi 1 mau trong tran; con 1 mau = thua hiep. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPre(LivingDamageEvent.Pre e) {
        Match m = cur;
        if (m == null) return;
        int vs = sideOf(m, e.getEntity());
        if (vs == 0 || m.phase != Phase.FIGHT) return;
        if (m.pendingLoser != null) { e.setNewDamage(0f); return; }
        float hp = e.getEntity().getHealth();
        if (e.getNewDamage() >= hp - 1f) {
            e.setNewDamage(Math.max(0f, hp - 1f));
            m.pendingLoser = vs;
        }
    }

    /** Du phong: moi nguon khac (vd /kill, sat thuong bo qua) dan toi chet -> huy, thua hiep. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        Match m = cur;
        if (m == null) return;
        int vs = sideOf(m, e.getEntity());
        if (vs == 0) return;
        e.setCanceled(true);
        e.getEntity().setHealth(1f);
        if (m.phase == Phase.FIGHT && m.pendingLoser == null) m.pendingLoser = vs;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHeal(LivingHealEvent e) {
        if (allowHeal || cur == null) return;
        if (sideOf(cur, e.getEntity()) != 0) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (cur == null || !(e.getEntity() instanceof ServerPlayer p) || sideOf(cur, p) == 0) return;
        if (e.getItemStack().getItem() instanceof UtilityPillItem) {
            e.setCanceled(true);
            p.displayClientMessage(Component.translatable("hukhong.pvp.pill_blocked"), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        PENDING.remove(e.getEntity().getUUID());
        Match m = cur;
        if (m != null && e.getEntity() instanceof ServerPlayer p) {
            int s = sideOf(m, p);
            if (s != 0) forfeit(m, s);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getServer() != null) {
            long paid = PvpData.get(p.getServer().overworld()).settleOwed(p);
            if (paid > 0) p.sendSystemMessage(Component.translatable("hukhong.pvp.owed_paid", TuTinhText.of(paid)));
        }
    }

    /** Dong server: hoan het tien dang giu (cuoc + khan gia) de khong mat. */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        Match m = cur;
        cur = null;
        PENDING.clear();
        if (m != null) {
            MinecraftServer server = e.getServer();
            if (m.escrowA > 0) PvpData.credit(server, m.a.getUUID(), m.escrowA);
            if (m.escrowB > 0) PvpData.credit(server, m.b.getUUID(), m.escrowB);
            for (PvpMath.Bet b : m.bets) PvpData.credit(server, b.who(), b.amount());
            if (m.a instanceof Mob) m.a.discard();
            if (m.b instanceof Mob) m.b.discard();
        }
        Bracket.shutdown(e.getServer());
    }

    /** Tien ich cho test / Bracket. */
    static Match current() { return cur; }

    /** Cho test: bat tran khong escrow giua hai LivingEntity bat ky. */
    static void startForTest(ServerLevel ow, BlockPos arena, LivingEntity a, LivingEntity b, long eA, long eB, boolean balance, IntConsumer onEnd) {
        start(ow, arena, a, b, eA, eB, balance, PvpMath.ROUND_TICKS, onEnd);
    }

    static void endRoundForTest(int loserSide) { if (cur != null) endRound(cur, loserSide); }
    static void setFightForTest() { if (cur != null) { cur.phase = Phase.FIGHT; cur.timer = cur.roundTicks; } }
    static Integer pendingLoserForTest() { return cur == null ? null : cur.pendingLoser; }
    static int[] winsForTest() { return cur == null ? new int[]{-1, -1} : new int[]{cur.winsA, cur.winsB}; }
    static void processPendingForTest() {
        Match m = cur;
        if (m != null && m.pendingLoser != null) { int l = m.pendingLoser; m.pendingLoser = null; endRound(m, l); }
    }
    static void clearForTest() { cur = null; PENDING.clear(); }
}
