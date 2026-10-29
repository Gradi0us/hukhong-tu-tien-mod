package com.hukhong.pvp;

import com.hukhong.core.CultivationService;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Giai Sinh Tu hang tuan (su kien lich "pvp_bracket"): dang ky 2 phut, 8 slot (nguoi that truoc, con lai NPC tan tu), loai truc tiep
 * tu ket 4 -> ban ket 2 -> chung ket. Phi dang ky = tien cuoc toi thieu theo cap; nha vo dich nhan quy tru 5% dot (NPC thang: hoan phi tru dot).
 * Tran NPC vs NPC giai bang may rui; hiep rut ngan 90 giay de kip thoi luong su kien.
 */
public final class Bracket {
    private Bracket() {}

    public static final int REGISTER_TICKS = 2400, ROUND_TICKS = 1800;

    private enum State { REGISTER, RUNNING }

    /** id null = NPC tan tu (so thu tu filler). */
    record Slot(UUID id, int filler) {
        boolean human() { return id != null; }
    }

    private static State state;
    private static ServerLevel level;
    private static long registerUntil, endsAt;
    private static final Map<UUID, Long> FEES = new LinkedHashMap<>();
    private static List<Slot> round = new ArrayList<>();
    private static List<Slot> next = new ArrayList<>();
    private static int pairIdx;
    private static boolean matchRunning;

    public static boolean running() { return state != null && state == State.RUNNING; }
    public static boolean registering() { return state == State.REGISTER; }

    public static void open(ServerLevel ow, long endsAtTick) {
        if (state != null) return;
        if (!DuelManager.open(ow)) { broadcast(ow, Component.translatable("hukhong.pvp.bracket.closed")); return; }
        level = ow;
        state = State.REGISTER;
        registerUntil = ow.getGameTime() + REGISTER_TICKS;
        endsAt = endsAtTick;
        FEES.clear();
        broadcast(ow, Component.translatable("hukhong.pvp.bracket.open", REGISTER_TICKS / 1200));
    }

    public static String join(ServerPlayer p) {
        if (state != State.REGISTER) return "hukhong.pvp.err.bracket_closed";
        if (FEES.containsKey(p.getUUID())) return "hukhong.pvp.err.bracket_joined";
        if (FEES.size() >= 8) return "hukhong.pvp.err.bracket_full";
        BlockPos arena = PvpData.get(level).arena();
        if (p.level() != level || p.distanceToSqr(arena.getX(), arena.getY(), arena.getZ()) > (double) DuelManager.NEAR_RANGE * DuelManager.NEAR_RANGE)
            return "hukhong.pvp.err.far";
        long fee = PvpMath.minStake(CultivationService.get(p).level());
        if (!Wallet.pay(p, fee)) return "hukhong.pvp.err.funds";
        FEES.put(p.getUUID(), fee);
        p.sendSystemMessage(Component.translatable("hukhong.pvp.bracket.joined", TuTinhText.of(fee), FEES.size()));
        return null;
    }

    static void tick(ServerLevel ow) {
        if (state == null) return;
        long now = ow.getGameTime();
        if (state == State.REGISTER) {
            if (now >= registerUntil) startBracket(ow);
            return;
        }
        if (now > endsAt) { abort(ow); return; }
        if (!DuelManager.busy() && !matchRunning && now % 20 == 0) nextMatch(ow);
    }

    private static void startBracket(ServerLevel ow) {
        List<UUID> humans = new ArrayList<>();
        for (UUID u : FEES.keySet()) if (ow.getServer().getPlayerList().getPlayer(u) != null) humans.add(u);
        if (humans.isEmpty()) { abort(ow); return; }
        Collections.shuffle(humans, new java.util.Random(ow.getGameTime()));
        round = new ArrayList<>();
        int f = 0;
        for (UUID u : PvpMath.seedBracket(humans)) round.add(u != null ? new Slot(u, 0) : new Slot(null, ++f));
        // xen ke nguoi that va NPC cho de co tran (khong ep hai nguoi that vao nhau neu con NPC)
        next = new ArrayList<>();
        pairIdx = 0;
        state = State.RUNNING;
        broadcast(ow, Component.translatable("hukhong.pvp.bracket.start", humans.size(), 8 - humans.size()));
    }

    private static void nextMatch(ServerLevel ow) {
        while (true) {
            if (pairIdx * 2 >= round.size()) {
                round = next;
                next = new ArrayList<>();
                pairIdx = 0;
                if (round.size() <= 1) { finish(ow, round.isEmpty() ? null : round.get(0)); return; }
                broadcast(ow, Component.translatable("hukhong.pvp.bracket.round", round.size() == 2 ? 3 : 2));
            }
            Slot a = round.get(2 * pairIdx), b = round.get(2 * pairIdx + 1);
            pairIdx++;
            if (!a.human() && !b.human()) { next.add(ow.random.nextBoolean() ? a : b); continue; }
            ServerPlayer pa = a.human() ? ow.getServer().getPlayerList().getPlayer(a.id()) : null;
            ServerPlayer pb = b.human() ? ow.getServer().getPlayerList().getPlayer(b.id()) : null;
            boolean okA = !a.human() || usable(ow, pa), okB = !b.human() || usable(ow, pb);
            if (!okA || !okB) { // nguoi that vang mat -> xu thua
                next.add(okA ? a : okB ? b : a);
                continue;
            }
            BlockPos arena = PvpData.get(ow).arena();
            LivingEntity ea = pa != null ? pa : spawnFiller(ow, arena, a.filler(), pb);
            LivingEntity eb = pb != null ? pb : spawnFiller(ow, arena, b.filler(), pa);
            if (ea == null || eb == null) { next.add(pa != null ? a : b); continue; }
            matchRunning = true;
            final Slot sa = a, sb = b;
            DuelManager.start(ow, arena, ea, eb, 0, 0, true, ROUND_TICKS, w -> {
                matchRunning = false;
                next.add(w > 0 ? sa : w < 0 ? sb : (level.random.nextBoolean() ? sa : sb));
            });
            return;
        }
    }

    private static boolean usable(ServerLevel ow, ServerPlayer p) {
        BlockPos arena = PvpData.get(ow).arena();
        return p != null && p.isAlive() && p.level() == ow && !DuelManager.inMatch(p)
                && p.distanceToSqr(arena.getX(), arena.getY(), arena.getZ()) <= 96.0 * 96.0;
    }

    private static LivingEntity spawnFiller(ServerLevel ow, BlockPos arena, int no, LivingEntity opponent) {
        Vindicator v = EntityType.VINDICATOR.create(ow);
        if (v == null) return null;
        double hp = opponent == null ? 40 : Math.max(20, Math.min(600, opponent.getMaxHealth()));
        v.moveTo(arena.getX() + 0.5, arena.getY(), arena.getZ() + 0.5, 0f, 0f);
        v.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        v.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(3, hp * 0.08));
        v.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
        v.setHealth((float) hp);
        v.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        v.setCustomName(Component.translatable("hukhong.pvp.filler", no));
        v.setCustomNameVisible(true);
        v.setPersistenceRequired();
        v.addTag("hk_pvp_filler");
        ow.addFreshEntity(v);
        return v;
    }

    private static void finish(ServerLevel ow, Slot champ) {
        MinecraftServer server = ow.getServer();
        long total = 0;
        for (long f : FEES.values()) total += f;
        if (champ != null && champ.human()) {
            long prize = PvpMath.bracketPrize(total);
            PvpData.credit(server, champ.id(), prize);
            ServerPlayer cp = server.getPlayerList().getPlayer(champ.id());
            broadcast(ow, Component.translatable("hukhong.pvp.bracket.champion", cp != null ? cp.getDisplayName() : Component.literal("?"), TuTinhText.of(prize)));
        } else {
            FEES.forEach((u, f) -> PvpData.credit(server, u, f - PvpMath.burn(f)));
            broadcast(ow, Component.translatable("hukhong.pvp.bracket.filler_won"));
        }
        reset();
    }

    /** Huy giai (het gio / khong ai tham gia): hoan day du phi. */
    static void abort(ServerLevel ow) {
        if (state == null) return;
        FEES.forEach((u, f) -> PvpData.credit(ow.getServer(), u, f));
        broadcast(ow, Component.translatable("hukhong.pvp.bracket.aborted"));
        reset();
    }

    public static void abortIfActive(ServerLevel ow) { abort(ow); }

    static void shutdown(MinecraftServer server) {
        if (state == null) return;
        FEES.forEach((u, f) -> PvpData.credit(server, u, f));
        reset();
    }

    private static void reset() {
        state = null;
        FEES.clear();
        round = new ArrayList<>();
        next = new ArrayList<>();
        pairIdx = 0;
        matchRunning = false;
        level = null;
    }

    private static void broadcast(ServerLevel ow, Component c) {
        for (ServerPlayer p : ow.players()) p.sendSystemMessage(c);
    }
}
