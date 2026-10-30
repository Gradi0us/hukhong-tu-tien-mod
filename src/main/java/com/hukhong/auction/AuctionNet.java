package com.hukhong.auction;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mang cho Dau Gia Hanh, Phi Kiem Truyen Thu va Dau Gia Hoi. Server tinh lai + kiem tra moi thu; client chi hien thi. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class AuctionNet {
    private AuctionNet() {}

    public static final int PAGE = 7;
    public static final int OP_BID = 0, OP_BUY = 1, OP_CANCEL = 2, OP_LIST = 3, OP_REFRESH = 4;
    private static final Map<UUID, Query> LAST = new HashMap<>();

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, s); }

    // ---------------------------------------------------------------- du lieu

    public record LotView(long id, ItemStack stack, String seller, boolean npc, long start, long buyNow, long bid, String bidder,
                          long remainMs, boolean mine, boolean high, long floor) {
        void write(RegistryFriendlyByteBuf b) {
            b.writeVarLong(id); ItemStack.OPTIONAL_STREAM_CODEC.encode(b, stack); b.writeUtf(seller); b.writeBoolean(npc);
            b.writeVarLong(start); b.writeVarLong(buyNow); b.writeVarLong(bid); b.writeUtf(bidder); b.writeVarLong(remainMs);
            b.writeBoolean(mine); b.writeBoolean(high); b.writeVarLong(floor);
        }

        static LotView read(RegistryFriendlyByteBuf b) {
            return new LotView(b.readVarLong(), ItemStack.OPTIONAL_STREAM_CODEC.decode(b), b.readUtf(), b.readBoolean(), b.readVarLong(),
                    b.readVarLong(), b.readVarLong(), b.readUtf(), b.readVarLong(), b.readBoolean(), b.readBoolean(), b.readVarLong());
        }

        public long price() { return bidder.isEmpty() ? start : bid; }
    }

    public record MailLine(String subjectKey, String arg, long ha, ItemStack item, long ageMs, boolean pending) {}

    // ---------------------------------------------------------------- payload

    public record Query(int filter, int sort, String search, int page) implements CustomPacketPayload {
        public static final Type<Query> TYPE = new Type<>(id("auction_query"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Query> CODEC = StreamCodec.of(
                (b, q) -> { b.writeVarInt(q.filter); b.writeVarInt(q.sort); b.writeUtf(q.search, 40); b.writeVarInt(q.page); },
                b -> new Query(b.readVarInt(), b.readVarInt(), b.readUtf(40), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Act(int op, long lot, long amount, long buyNow, int hoursIdx) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(id("auction_act"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Act> CODEC = StreamCodec.of(
                (b, a) -> { b.writeVarInt(a.op); b.writeVarLong(a.lot); b.writeVarLong(a.amount); b.writeVarLong(a.buyNow); b.writeVarInt(a.hoursIdx); },
                b -> new Act(b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: mo man hinh Dau Gia Hanh. */
    public record OpenAuction() implements CustomPacketPayload {
        public static final Type<OpenAuction> TYPE = new Type<>(id("auction_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenAuction> CODEC = StreamCodec.of((b, o) -> { }, b -> new OpenAuction());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Lots(int filter, int sort, int page, int pages, int myLots, int maxLots, List<LotView> views, List<String> hours) implements CustomPacketPayload {
        public static final Type<Lots> TYPE = new Type<>(id("auction_lots"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Lots> CODEC = StreamCodec.of(
                (b, l) -> {
                    b.writeVarInt(l.filter); b.writeVarInt(l.sort); b.writeVarInt(l.page); b.writeVarInt(l.pages); b.writeVarInt(l.myLots); b.writeVarInt(l.maxLots);
                    b.writeVarInt(l.views.size());
                    for (LotView v : l.views) v.write(b);
                    b.writeVarInt(l.hours.size());
                    for (String h : l.hours) b.writeUtf(h, 8);
                },
                b -> {
                    int f = b.readVarInt(), s = b.readVarInt(), p = b.readVarInt(), ps = b.readVarInt(), my = b.readVarInt(), mx = b.readVarInt();
                    int n = b.readVarInt();
                    List<LotView> v = new ArrayList<>();
                    for (int i = 0; i < n && i < 64; i++) v.add(LotView.read(b));
                    int hn = b.readVarInt();
                    List<String> h = new ArrayList<>();
                    for (int i = 0; i < hn && i < 8; i++) h.add(b.readUtf(8));
                    return new Lots(f, s, p, ps, my, mx, v, h);
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record MailClaim() implements CustomPacketPayload {
        public static final Type<MailClaim> TYPE = new Type<>(id("mail_claim"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MailClaim> CODEC = StreamCodec.of((b, o) -> { }, b -> new MailClaim());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: hom thu (mo man hinh neu chua mo). */
    public record MailView(List<MailLine> lines) implements CustomPacketPayload {
        public static final Type<MailView> TYPE = new Type<>(id("mail_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MailView> CODEC = StreamCodec.of(
                (b, m) -> {
                    b.writeVarInt(m.lines.size());
                    for (MailLine l : m.lines) {
                        b.writeUtf(l.subjectKey, 128); b.writeUtf(l.arg, 128); b.writeVarLong(l.ha); ItemStack.OPTIONAL_STREAM_CODEC.encode(b, l.item);
                        b.writeVarLong(l.ageMs); b.writeBoolean(l.pending);
                    }
                },
                b -> {
                    int n = b.readVarInt();
                    List<MailLine> l = new ArrayList<>();
                    for (int i = 0; i < n && i < 64; i++)
                        l.add(new MailLine(b.readUtf(128), b.readUtf(128), b.readVarLong(), ItemStack.OPTIONAL_STREAM_CODEC.decode(b), b.readVarLong(), b.readBoolean()));
                    return new MailView(l);
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: trang thai Dau Gia Hoi cho mot nguoi (active=false -> tat HUD). */
    public record LiveState(boolean active, int idx, int count, ItemStack stack, boolean mystery, long price, String holder, int secLeft,
                            int phase, long raise5, long raise10, boolean locked, boolean passed, boolean iHold, String title) implements CustomPacketPayload {
        public static final Type<LiveState> TYPE = new Type<>(id("live_state"));
        public static final LiveState OFF = new LiveState(false, 0, 0, ItemStack.EMPTY, false, 0, "", 0, 5, 0, 0, false, false, false, "");
        public static final StreamCodec<RegistryFriendlyByteBuf, LiveState> CODEC = StreamCodec.of(
                (b, s) -> {
                    b.writeBoolean(s.active); b.writeVarInt(s.idx); b.writeVarInt(s.count); ItemStack.OPTIONAL_STREAM_CODEC.encode(b, s.stack);
                    b.writeBoolean(s.mystery); b.writeVarLong(s.price); b.writeUtf(s.holder, 64); b.writeVarInt(s.secLeft); b.writeVarInt(s.phase);
                    b.writeVarLong(s.raise5); b.writeVarLong(s.raise10); b.writeBoolean(s.locked); b.writeBoolean(s.passed); b.writeBoolean(s.iHold);
                    b.writeUtf(s.title, 128);
                },
                b -> new LiveState(b.readBoolean(), b.readVarInt(), b.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(b), b.readBoolean(),
                        b.readVarLong(), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readBoolean(),
                        b.readBoolean(), b.readBoolean(), b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: mo man hinh dau gia truc tiep (nut +5% / +10% / Bo). */
    public record OpenLive() implements CustomPacketPayload {
        public static final Type<OpenLive> TYPE = new Type<>(id("live_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenLive> CODEC = StreamCodec.of((b, o) -> { }, b -> new OpenLive());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** C2S: pct = 5 / 10 gio the nang gia, 0 = bo lo nay. */
    public record LiveBid(int pct) implements CustomPacketPayload {
        public static final Type<LiveBid> TYPE = new Type<>(id("live_bid"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LiveBid> CODEC = StreamCodec.of((b, o) -> b.writeVarInt(o.pct), b -> new LiveBid(b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- dang ky

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(OpenAuction.TYPE, OpenAuction.CODEC, (p, c) -> c.enqueueWork(() -> com.hukhong.client.auction.AuctionClient.open()));
        r.playToClient(Lots.TYPE, Lots.CODEC, (p, c) -> c.enqueueWork(() -> com.hukhong.client.auction.AuctionClient.lots(p)));
        r.playToClient(MailView.TYPE, MailView.CODEC, (p, c) -> c.enqueueWork(() -> com.hukhong.client.auction.AuctionClient.mail(p)));
        r.playToClient(LiveState.TYPE, LiveState.CODEC, (p, c) -> c.enqueueWork(() -> com.hukhong.client.auction.AuctionClient.live(p)));
        r.playToClient(OpenLive.TYPE, OpenLive.CODEC, (p, c) -> c.enqueueWork(() -> com.hukhong.client.auction.AuctionClient.openLive()));
        r.playToServer(Query.TYPE, Query.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) { LAST.put(sp.getUUID(), p); sendLots(sp); }
        }));
        r.playToServer(Act.TYPE, Act.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) act(sp, p);
        }));
        r.playToServer(MailClaim.TYPE, MailClaim.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) { MailService.deliver(sp); sendMail(sp); }
        }));
        r.playToServer(LiveBid.TYPE, LiveBid.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) LiveAuction.playerBid(sp, p.pct());
        }));
    }

    // ---------------------------------------------------------------- server

    private static boolean nearHall(ServerPlayer sp) {
        for (var pos : AuctionData.get(sp.server).halls.values())
            if (sp.level().dimension() == net.minecraft.world.level.Level.OVERWORLD && sp.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 40 * 40) return true;
        return false;
    }

    static void act(ServerPlayer sp, Act a) {
        long now = System.currentTimeMillis();
        if (a.op != OP_REFRESH && !nearHall(sp) && !sp.hasPermissions(2)) { sp.displayClientMessage(Tx.t("hukhong.auction.err.far", "Hãy đến Đấu Giá Hành để giao dịch.", "Visit an Auction House to trade."), true); return; }
        Component err = switch (a.op) {
            case OP_BID -> AuctionService.bid(sp, a.lot, a.amount, now);
            case OP_BUY -> AuctionService.buy(sp, a.lot, now);
            case OP_CANCEL -> AuctionService.cancel(sp, a.lot);
            case OP_LIST -> AuctionService.listFromHand(sp, a.amount, a.buyNow, a.hoursIdx, now);
            default -> null;
        };
        if (err != null) sp.displayClientMessage(err, true);
        else if (a.op != OP_REFRESH) sp.displayClientMessage(Tx.t("hukhong.auction.ok", "Thành công.", "Done."), true);
        sendLots(sp);
    }

    public static void open(ServerPlayer sp) {
        PacketDistributor.sendToPlayer(sp, new OpenAuction());
        sendLots(sp);
    }

    public static void sendLots(ServerPlayer sp) {
        if (sp.connection == null) return;
        AuctionData d = AuctionData.get(sp.server);
        long now = System.currentTimeMillis();
        Query q = LAST.getOrDefault(sp.getUUID(), new Query(0, 0, "", 0));
        List<Lot> all = AuctionService.query(d, sp.getUUID(), q.filter, q.sort, q.search, now);
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        int page = Math.max(0, Math.min(pages - 1, q.page));
        List<LotView> views = new ArrayList<>();
        for (int i = page * PAGE; i < Math.min(all.size(), (page + 1) * PAGE); i++) {
            Lot l = all.get(i);
            views.add(new LotView(l.id, l.stack.copy(), l.sellerName, l.npc(), l.startPrice, l.buyNow, l.bid, l.hasBid() ? l.bidderName : "",
                    Math.max(0, l.endMs - now), sp.getUUID().equals(l.seller), sp.getUUID().equals(l.bidder), AuctionPricing.floor(l.stack)));
        }
        List<String> hours = new ArrayList<>();
        for (int h : AuctionConfig.get().durationsHours) hours.add(String.valueOf(h));
        PacketDistributor.sendToPlayer(sp, new Lots(q.filter, q.sort, page, pages, d.countOf(sp.getUUID()), AuctionConfig.get().maxLotsPerPlayer, views, hours));
    }

    public static void sendMail(ServerPlayer sp) {
        if (sp.connection == null) return;
        long now = System.currentTimeMillis();
        List<MailLine> lines = new ArrayList<>();
        var box = MailData.get(sp.server).peek(sp.getUUID());
        for (int i = box.size() - 1; i >= 0 && lines.size() < 30; i--) {
            var m = box.get(i);
            lines.add(new MailLine(m.subjectKey, m.arg, m.ha, m.item.copy(), now - m.timeMs, m.pending()));
        }
        PacketDistributor.sendToPlayer(sp, new MailView(lines));
    }
}
