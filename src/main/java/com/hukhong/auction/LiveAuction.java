package com.hukhong.auction;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import com.hukhong.living.MaTu;
import com.hukhong.story.StoryService;
import com.hukhong.story.SuDo;
import com.hukhong.story.objective.Objectives;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Dau Gia Hoi (B7): su kien THOI GIAN THUC (mac dinh thu 4 va thu 7 luc 20:00 gio may chu) tai hoi truong Dau Gia Hanh cua mot thanh.
 * Dau Gia Su ho gia 6-10 lo; moi lan tra gia reset dong ho {@link AuctionConfig#bidSeconds} giay; nguoi choi bam +5% / +10% / Bo;
 * 5 NPC tan tu co ngan sach cung tra gia. Lo cuoi = bao vat bi an. Doi khi co nguoi tra gia BI AN (Su Do cai trang): thang lo bi phuc kich
 * khi roi hoi truong; thua lo nhan nhiem vu truy nguoi mua (Objectives). Tien dat gia bi giu (AuctionData.liveEscrow), hoan khi bi vuot/dung may chu.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LiveAuction {
    private LiveAuction() {}

    public static final String[] RIVALS = {"Tán tu Mạc Phong", "Hàn Nguyệt Cô Cô", "Lục Trường Quy", "Thạch Đại Lực", "Diệp Thanh Vân"};
    public static final String MYSTERY_NAME = "Người đeo mặt nạ";
    public static final int P_COUNT = 0, P_INTRO = 1, P_BID = 2, P_SOLD = 3, P_UNSOLD = 4, P_DONE = 5;
    private static final int INTRO_SEC = 4, SOLD_SEC = 4;

    static final class Rival {
        final String name; long budget; final double eager; final boolean mysterious;
        Rival(String n, long b, double e, boolean m) { name = n; budget = b; eager = e; mysterious = m; }
    }

    static final class State {
        String key, cityId;
        BlockPos host;
        List<LiveLots.Entry> lots;
        int idx = -1, phase = P_COUNT, sec;
        List<Rival> rivals = new ArrayList<>();
        // lo hien tai
        long cur;
        UUID holderPlayer;
        int holderRival = -1;
        boolean mysteryLot;
        Set<UUID> passed = new HashSet<>(), bidders = new HashSet<>();
        UUID lastPlayerHolder;
        final Set<UUID> shown = new HashSet<>();
        final Random rnd;
        State(long seed) { rnd = new Random(seed); }
    }

    private static State live;
    private static boolean warnedNoHall;
    private record Ambush(UUID player, BlockPos host, long expire) {}
    private static final List<Ambush> AMBUSHES = new ArrayList<>();

    public static boolean running() { return live != null; }

    public static BlockPos hostPos() { return live == null ? null : live.host; }

    // ---------------------------------------------------------------- lich

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % 20 != 3) return;
        try {
            if (live == null) { checkSchedule(s); flushEscrowIfIdle(s); }
            else second(s);
            ambushTick(s);
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().error("LiveAuction tick failed, aborting event: {}", ex.toString(), ex);
            abort(s);
        }
    }

    static void checkSchedule(MinecraftServer s) {
        AuctionConfig c = AuctionConfig.get();
        AuctionData d = AuctionData.get(s);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = AuctionMath.lastSlot(now, c.days(), c.liveHour, c.liveMinute);
        if (last != null && !AuctionMath.slotKey(last).equals(d.lastLiveKey)
                && java.time.Duration.between(last, now).toMinutes() <= c.liveGraceMinutes) { start(s, AuctionMath.slotKey(last), 5); return; }
        LocalDateTime next = AuctionMath.nextSlot(now, c.days(), c.liveHour, c.liveMinute);
        if (next != null && !AuctionMath.slotKey(next).equals(d.lastLiveKey)) {
            long secs = java.time.Duration.between(now, next).getSeconds();
            if (secs <= c.liveLeadMinutes * 60L) start(s, AuctionMath.slotKey(next), (int) Math.max(5, secs));
        }
    }

    /** Bat dau (op: /daugiahoi start) hoac theo lich. countdownSec = giay cho truoc khi khai mac. false neu khong co hoi truong. */
    public static boolean start(MinecraftServer s, String key, int countdownSec) {
        if (live != null) return false;
        AuctionData d = AuctionData.get(s);
        if (d.halls.isEmpty()) {
            if (!warnedNoHall) { warnedNoHall = true; com.mojang.logging.LogUtils.getLogger().info("Live auction due but no auction hall is built yet"); }
            return false;
        }
        List<String> ids = new ArrayList<>(d.halls.keySet());
        java.util.Collections.sort(ids);
        State st = new State(key.hashCode() * 31L + s.overworld().getSeed());
        st.key = key;
        st.cityId = ids.get(Math.floorMod(key.hashCode(), ids.size()));
        st.host = d.halls.get(st.cityId);
        st.lots = LiveLots.generate(st.rnd, 6 + st.rnd.nextInt(5));
        long avg = 0;
        for (LiveLots.Entry e : st.lots) avg += e.valuation();
        avg /= st.lots.size();
        for (int i = 0; i < RIVALS.length; i++) st.rivals.add(new Rival(RIVALS[i], Math.round(avg * (1.5 + st.rnd.nextDouble() * 2.5)), 0.7 + st.rnd.nextDouble() * 0.6, false));
        st.sec = countdownSec;
        st.phase = P_COUNT;
        live = st;
        d.lastLiveKey = key;
        d.setDirty();
        CityDef cd = CityDef.byId(st.cityId);
        broadcast(s, Tx.t("hukhong.live.announce", "Đấu Giá Hội sắp khai mạc tại %s (còn %s phút). Hãy đến hội trường Đấu Giá Hành!", "The Grand Auction opens soon at %s (in %s min). Head to the auction hall!",
                cd == null ? Component.literal(st.cityId) : Component.translatable(cd.nameKey()), Math.max(1, countdownSec / 60)));
        return true;
    }

    public static void stop(MinecraftServer s) { abort(s); }

    private static void abort(MinecraftServer s) {
        State st = live;
        live = null;
        if (st != null) {
            AuctionData d = AuctionData.get(s);
            refundAll(s, d);
            for (ServerPlayer p : s.getPlayerList().getPlayers()) if (st.shown.contains(p.getUUID())) send(p, AuctionNet.LiveState.OFF);
        }
    }

    // ---------------------------------------------------------------- vong lap moi giay

    private static void second(MinecraftServer s) {
        State st = live;
        ServerLevel ow = s.overworld();
        switch (st.phase) {
            case P_COUNT -> {
                int left = st.sec - 1;
                st.sec = left;
                if (left == 300 || left == 60) broadcast(s, Tx.t("hukhong.live.soon", "Đấu Giá Hội khai mạc sau %s phút.", "The Grand Auction opens in %s min.", left / 60));
                if (left <= 0) nextLot(s, st);
            }
            case P_INTRO -> { if (--st.sec <= 0) { st.phase = P_BID; st.sec = AuctionConfig.get().bidSeconds; } }
            case P_BID -> {
                rivalsBid(s, st);
                if (--st.sec <= 0) resolve(s, st);
            }
            case P_SOLD, P_UNSOLD -> { if (--st.sec <= 0) nextLot(s, st); }
            default -> { }
        }
        if (live == st && st.phase == P_DONE && --st.sec <= 0) { live = null; }
        pushAll(s, ow, st);
    }

    private static void nextLot(MinecraftServer s, State st) {
        st.idx++;
        if (st.idx >= st.lots.size()) {
            st.phase = P_DONE;
            st.sec = 6;
            broadcast(s, Tx.t("hukhong.live.end", "Đấu Giá Hội kết thúc. Cảm tạ chư vị!", "The Grand Auction has ended. Thank you all!"));
            return;
        }
        LiveLots.Entry e = st.lots.get(st.idx);
        st.cur = e.start();
        st.holderPlayer = null;
        st.holderRival = -1;
        st.passed.clear();
        st.bidders.clear();
        st.lastPlayerHolder = null;
        st.rivals.removeIf(r -> r.mysterious);
        st.mysteryLot = !e.mystery() && e.lore() == null && st.rnd.nextInt(100) < 35;
        if (st.mysteryLot) st.rivals.add(new Rival(MYSTERY_NAME, e.valuation() * 4, 2.2, true));
        st.phase = P_INTRO;
        st.sec = INTRO_SEC;
        Component what = e.mystery() ? Tx.t("hukhong.live.mystery", "Bảo Vật Bí Ẩn (chưa giám định)", "Mystery Treasure (unidentified)") : e.stack().getHoverName();
        broadcast(s, Tx.t("hukhong.live.intro", "Đấu Giá Sư: Lô số %s - %s, giá khởi điểm %s!", "Auctioneer: Lot %s - %s, starting at %s!", st.idx + 1, what, TuTinhText.of(e.start())));
        if (st.mysteryLot) broadcast(s, Tx.t("hukhong.live.masked", "Một bóng người đeo mặt nạ ở hàng sau khẽ giơ thẻ...", "A masked figure in the back row raises a paddle..."));
    }

    // ---------------------------------------------------------------- gia

    /** Gia can tra de nang len pct%. Lan dau (chua ai giu): 5% = gia khoi diem, 10% = +10% tren khoi diem. */
    public static long bidPrice(long cur, boolean hasHolder, int pct) {
        if (!hasHolder) return pct <= 5 ? cur : AuctionMath.raise(cur, pct);
        return AuctionMath.raise(cur, pct);
    }

    private static boolean hasHolder(State st) { return st.holderPlayer != null || st.holderRival >= 0; }

    private static void rivalsBid(MinecraftServer s, State st) {
        if (st.sec > AuctionConfig.get().bidSeconds - 2 || st.rnd.nextInt(100) >= 28) return;
        LiveLots.Entry e = st.lots.get(st.idx);
        long next = bidPrice(st.cur, hasHolder(st), 5);
        List<Integer> ok = new ArrayList<>();
        for (int i = 0; i < st.rivals.size(); i++) {
            Rival r = st.rivals.get(i);
            if (i == st.holderRival) continue;
            if (AuctionMath.rivalWilling(next, r.budget, Math.round(e.valuation() * r.eager))) ok.add(i);
        }
        if (ok.isEmpty()) return;
        int pick = ok.get(st.rnd.nextInt(ok.size()));
        refundHolder(s, st);
        st.cur = next;
        st.holderRival = pick;
        st.holderPlayer = null;
        st.sec = AuctionConfig.get().bidSeconds;
        broadcast(s, Tx.t("hukhong.live.rival", "%s trả %s!", "%s bids %s!", st.rivals.get(pick).name, TuTinhText.of(next)));
    }

    private static void refundHolder(MinecraftServer s, State st) {
        if (st.holderPlayer == null) return;
        AuctionData d = AuctionData.get(s);
        Long held = d.liveEscrow.remove(st.holderPlayer);
        d.setDirty();
        if (held != null && held > 0) MailService.send(s, st.holderPlayer, "hukhong.mail.sub.refund", "Đấu Giá Hội", held, ItemStack.EMPTY);
        st.holderPlayer = null;
    }

    /** Nguoi choi ra gia (pct 5/10) hoac bo (0). */
    public static void playerBid(ServerPlayer sp, int pct) {
        State st = live;
        if (st == null || st.phase != P_BID) { sp.displayClientMessage(Tx.t("hukhong.live.err.phase", "Chưa đến lúc ra giá.", "Bidding is not open."), true); return; }
        if (!near(sp, st)) { sp.displayClientMessage(Tx.t("hukhong.live.err.far", "Bạn phải ở trong hội trường.", "You must be inside the hall."), true); return; }
        if (pct == 0) { st.passed.add(sp.getUUID()); push(sp, st); return; }
        if (pct != 5 && pct != 10) return;
        LiveLots.Entry e = st.lots.get(st.idx);
        if (!StoryHooks.allowed(sp, e.lore())) { sp.displayClientMessage(Tx.t("hukhong.live.err.locked", "Lô này chưa mở cho bạn (cốt truyện).", "This lot is locked for you (story)."), true); return; }
        if (sp.getUUID().equals(st.holderPlayer)) { sp.displayClientMessage(Tx.t("hukhong.live.err.holder", "Bạn đang giữ giá cao nhất.", "You already hold the top bid."), true); return; }
        if (st.passed.contains(sp.getUUID())) { sp.displayClientMessage(Tx.t("hukhong.live.err.passed", "Bạn đã bỏ lô này.", "You passed on this lot."), true); return; }
        long price = bidPrice(st.cur, hasHolder(st), pct);
        if (!Wallet.pay(sp, price)) { sp.displayClientMessage(Tx.t("hukhong.live.err.funds", "Không đủ Tử Tinh (cần %s).", "Not enough Tu Tinh (need %s).", TuTinhText.of(price)), true); return; }
        MinecraftServer s = sp.server;
        refundHolder(s, st);
        AuctionData d = AuctionData.get(s);
        d.liveEscrow.put(sp.getUUID(), price);
        d.setDirty();
        st.cur = price;
        st.holderPlayer = sp.getUUID();
        st.holderRival = -1;
        st.lastPlayerHolder = sp.getUUID();
        st.bidders.add(sp.getUUID());
        st.sec = Math.max(st.sec, AuctionConfig.get().bidSeconds);
        broadcast(s, Tx.t("hukhong.live.player", "%s trả %s!", "%s bids %s!", sp.getDisplayName(), TuTinhText.of(price)));
        pushAll(s, s.overworld(), st);
    }

    // ---------------------------------------------------------------- chot lo

    private static void resolve(MinecraftServer s, State st) {
        LiveLots.Entry e = st.lots.get(st.idx);
        Component name = e.stack().getHoverName();
        if (!hasHolder(st)) {
            st.phase = P_UNSOLD;
            st.sec = SOLD_SEC;
            broadcast(s, Tx.t("hukhong.live.unsold", "Lô số %s không ai trả giá, thu hồi.", "Lot %s drew no bids and is withdrawn.", st.idx + 1));
            return;
        }
        st.phase = P_SOLD;
        st.sec = SOLD_SEC;
        AuctionData d = AuctionData.get(s);
        if (st.holderPlayer != null) {
            UUID w = st.holderPlayer;
            d.liveEscrow.remove(w);
            d.burned += st.cur; // hoa hong 100%: ban to chuc giu lai, dot khoi luu thong
            d.setDirty();
            MailService.send(s, w, "hukhong.mail.sub.live_won", e.mystery() ? "?" : name.getString(), 0, e.stack());
            ServerPlayer p = s.getPlayerList().getPlayer(w);
            broadcast(s, Tx.t("hukhong.live.sold_player", "Lô số %s thuộc về %s với giá %s!", "Lot %s goes to %s for %s!", st.idx + 1, p == null ? "?" : p.getDisplayName(), TuTinhText.of(st.cur)));
            if (e.mystery() && p != null) p.sendSystemMessage(Tx.t("hukhong.live.reveal", "Bảo vật bí ẩn hé lộ: %s", "The mystery treasure is revealed: %s", name));
            if (p != null) {
                StoryHooks.complete(p, e.lore());
                if (st.mysteryLot) AMBUSHES.add(new Ambush(w, st.host, s.overworld().getGameTime() + 36_000));
                if (st.mysteryLot) p.sendSystemMessage(Tx.t("hukhong.live.ambush_hint", "Người đeo mặt nạ nhìn bạn rất lâu rồi lặng lẽ rời ghế... Hãy cẩn thận khi rời hội trường.", "The masked figure stares at you, then slips away... Be careful leaving the hall."));
            }
        } else {
            Rival r = st.rivals.get(st.holderRival);
            r.budget = Math.max(0, r.budget - st.cur);
            broadcast(s, Tx.t("hukhong.live.sold_rival", "Lô số %s thuộc về %s với giá %s.", "Lot %s goes to %s for %s.", st.idx + 1, r.name, TuTinhText.of(st.cur)));
            if (r.mysterious && st.lastPlayerHolder != null) {
                ServerPlayer loser = s.getPlayerList().getPlayer(st.lastPlayerHolder);
                if (loser != null) pursuit(s, loser, st, e.stack());
            }
        }
    }

    // ---------------------------------------------------------------- cot truyen: truy duoi / phuc kich

    private static void pursuit(MinecraftServer s, ServerPlayer p, State st, ItemStack lot) {
        if (!StoryHooks.allowed(p, LiveLots.B_DOC_LORE)) return; // chua toi chuong Da Khuyet: chi la thua lo thuong
        ServerLevel sl = s.overworld();
        double ang = st.rnd.nextDouble() * Math.PI * 2, dist = 70 + st.rnd.nextInt(40);
        int x = st.host.getX() + (int) (Math.cos(ang) * dist), z = st.host.getZ() + (int) (Math.sin(ang) * dist);
        BlockPos g = new BlockPos(x, sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (!sl.hasChunkAt(g)) sl.getChunk(g);
        g = new BlockPos(x, sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        AmbientNpc n = MaTu.spawnDisguised(sl, g, p.getUUID(), lot);
        if (n == null) return;
        Objectives.set(p, MaTu.PURSUIT_ID, Tx.t("hukhong.live.pursuit", "Truy tìm kẻ mua bí ẩn đã mang đi lô đấu giá", "Track down the masked buyer who took the lot"), g, Level.OVERWORLD);
        p.sendSystemMessage(Tx.t("hukhong.live.pursuit_msg", "Kẻ đeo mặt nạ đã mua lô đó rồi biến mất. Có người thấy hắn đi về phía được đánh dấu trên bản đồ...", "The masked bidder took the lot and vanished. Someone saw him head toward the marked spot..."));
    }

    private static void ambushTick(MinecraftServer s) {
        if (AMBUSHES.isEmpty()) return;
        long now = s.overworld().getGameTime();
        for (Iterator<Ambush> it = AMBUSHES.iterator(); it.hasNext(); ) {
            Ambush a = it.next();
            ServerPlayer p = s.getPlayerList().getPlayer(a.player());
            if (p == null || now > a.expire()) { it.remove(); continue; }
            if (p.serverLevel().dimension() != Level.OVERWORLD || p.distanceToSqr(a.host().getX() + 0.5, a.host().getY(), a.host().getZ() + 0.5) < 28 * 28) continue;
            it.remove();
            ServerLevel sl = p.serverLevel();
            int lv = Math.max(10, Math.min(StoryService.MAX_LEVEL, CultivationService.get(p).level()));
            for (int i = 0; i < 2; i++) {
                int x = p.getBlockX() + (i == 0 ? 6 : -6), z = p.getBlockZ() + (i == 0 ? 2 : -3);
                BlockPos g = new BlockPos(x, sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                SuDo su = StoryService.spawn(sl, g, i, lv);
                if (su != null) { su.setTarget(p); su.addTag("hk_ambush"); su.setPersistenceRequired(); }
            }
            p.sendSystemMessage(Tx.t("hukhong.live.ambush", "Sứ Đồ Dạ Khuyết phục kích bạn để cướp lại bảo vật!", "Night-Rift envoys ambush you to take the prize back!"));
        }
    }

    // ---------------------------------------------------------------- dong bo

    static boolean near(Player p, State st) {
        return p.level().dimension() == Level.OVERWORLD
                && p.distanceToSqr(st.host.getX() + 0.5, st.host.getY(), st.host.getZ() + 0.5) <= (double) AuctionConfig.get().liveRadius * AuctionConfig.get().liveRadius;
    }

    private static void broadcast(MinecraftServer s, Component msg) {
        State st = live;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (st == null || st.phase == P_COUNT && st.sec > 120 || near(p, st)) p.sendSystemMessage(msg);
        }
    }

    private static void pushAll(MinecraftServer s, ServerLevel ow, State st) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (near(p, st)) push(p, st);
            else if (st.shown.remove(p.getUUID())) send(p, AuctionNet.LiveState.OFF);
        }
    }

    private static void send(ServerPlayer p, AuctionNet.LiveState s) {
        if (p.connection != null) PacketDistributor.sendToPlayer(p, s);
    }

    static void push(ServerPlayer p, State st) {
        st.shown.add(p.getUUID());
        if (st.phase == P_COUNT || st.idx < 0 || st.idx >= st.lots.size()) {
            send(p, new AuctionNet.LiveState(true, 0, st.lots.size(), ItemStack.EMPTY, false, 0, "", st.sec, P_COUNT, 0, 0, false, false, false, st.cityId));
            return;
        }
        LiveLots.Entry e = st.lots.get(st.idx);
        boolean hide = e.mystery() && st.phase < P_SOLD;
        String holder = st.holderPlayer != null
                ? (p.getUUID().equals(st.holderPlayer) ? p.getGameProfile().getName() : playerName(p.server, st.holderPlayer))
                : st.holderRival >= 0 ? st.rivals.get(st.holderRival).name : "";
        boolean h = hasHolder(st);
        send(p, new AuctionNet.LiveState(true, st.idx, st.lots.size(), hide ? ItemStack.EMPTY : e.stack().copy(), e.mystery(), st.cur, holder, st.sec, st.phase,
                bidPrice(st.cur, h, 5), bidPrice(st.cur, h, 10), !StoryHooks.allowed(p, e.lore()), st.passed.contains(p.getUUID()), p.getUUID().equals(st.holderPlayer), st.cityId));
    }

    private static String playerName(MinecraftServer s, UUID id) {
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        return p == null ? "?" : p.getGameProfile().getName();
    }

    /** Nguoi choi bam Dau Gia Su: mo man dau gia truc tiep neu dang dien ra gan hoi truong. */
    public static boolean interact(ServerPlayer sp) {
        State st = live;
        if (st == null || !near(sp, st)) return false;
        PacketDistributor.sendToPlayer(sp, new AuctionNet.OpenLive());
        push(sp, st);
        return true;
    }

    // ---------------------------------------------------------------- hoan tien

    private static void refundAll(MinecraftServer s, AuctionData d) {
        for (var en : new ArrayList<>(d.liveEscrow.entrySet()))
            MailService.send(s, en.getKey(), "hukhong.mail.sub.refund", "Đấu Giá Hội", en.getValue(), ItemStack.EMPTY);
        d.liveEscrow.clear();
        d.setDirty();
    }

    private static void flushEscrowIfIdle(MinecraftServer s) {
        AuctionData d = AuctionData.get(s);
        if (!d.liveEscrow.isEmpty()) refundAll(s, d);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        MinecraftServer s = e.getServer();
        AuctionData d = AuctionData.get(s);
        if (!d.liveEscrow.isEmpty()) refundAll(s, d);
        live = null;
        AMBUSHES.clear();
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        live = null;
        warnedNoHall = false;
        AMBUSHES.clear();
        flushEscrowIfIdle(e.getServer());
    }
}
