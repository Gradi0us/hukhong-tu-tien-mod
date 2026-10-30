package com.hukhong.auction;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import com.hukhong.honcot.Bone;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.honcot.HonCotRegistry;
import com.hukhong.item.ModItems;
import com.hukhong.skillbook.SkillBookRegistry;
import com.hukhong.skillbook.SkillBookRewards;
import com.hukhong.skillbook.SkillGrade;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Logic san Dau Gia Hanh (thuan server; thoi gian truyen vao). Tien dat gia bi GIU (rut khoi vi) va hoan bang thu khi bi tra cao hon;
 * phi ban 5% bi DOT (ghi vao AuctionData.burned), phi dang 1% cung bi dot. Moi ham tra Component loi (null = thanh cong).
 */
public final class AuctionService {
    private AuctionService() {}

    public static final String NPC_NAME = "Thương Hội Ký Gửi";

    private static Component err(String key, String vi, String en, Object... a) { return Tx.t("hukhong.auction.err." + key, vi, en, a); }

    // ---------------------------------------------------------------- dang ban

    /** Dang ban nguyen chong stack (da kiem tra do nguoi goi: stack la ban sao cua thu se bi lay khoi tay). */
    public static Component list(ServerPlayer sp, ItemStack stack, long start, long buyNow, int hoursIdx, long now) {
        AuctionConfig c = AuctionConfig.get();
        AuctionData d = AuctionData.get(sp.server);
        if (!AuctionPricing.tradeable(stack)) return err("untradeable", "Vật phẩm này không thể đưa lên sàn.", "This item cannot be auctioned.");
        if (d.countOf(sp.getUUID()) >= c.maxLotsPerPlayer) return err("cap", "Bạn đã đăng tối đa %s lô.", "You already have %s lots listed.", c.maxLotsPerPlayer);
        long floor = AuctionPricing.floor(stack);
        if (!AuctionMath.pricesValid(start, buyNow, floor)) return err("price", "Giá khởi điểm tối thiểu là %s (mua ngay phải không thấp hơn giá khởi điểm).", "Minimum start price is %s (buy-now must not be below start).", TuTinhText.of(floor));
        long fee = AuctionMath.listingFee(start, c.listingFeePct);
        if (!Wallet.pay(sp, fee)) return err("fee", "Không đủ Tử Tinh trả phí đăng (%s).", "Not enough Tu Tinh for the listing fee (%s).", TuTinhText.of(fee));
        d.burned += fee;
        Lot l = new Lot(d.newId(), sp.getUUID(), sp.getGameProfile().getName(), stack.copy(), start, buyNow, now, now + AuctionMath.durationMs(c.hours(hoursIdx)));
        d.lots.put(l.id, l);
        d.setDirty();
        return null;
    }

    /** Dang vat pham dang cam tren tay chinh. */
    public static Component listFromHand(ServerPlayer sp, long start, long buyNow, int hoursIdx, long now) {
        ItemStack held = sp.getMainHandItem();
        Component e = list(sp, held, start, buyNow, hoursIdx, now);
        if (e == null) sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return e;
    }

    // ---------------------------------------------------------------- tra gia / mua ngay

    public static Component bid(ServerPlayer sp, long lotId, long amount, long now) {
        AuctionData d = AuctionData.get(sp.server);
        Lot l = d.lots.get(lotId);
        if (l == null || now >= l.endMs) return err("gone", "Lô này đã kết thúc hoặc không còn.", "This lot has ended or is gone.");
        if (sp.getUUID().equals(l.seller)) return err("self", "Không thể tự mua đồ của mình.", "You cannot buy your own lot.");
        if (l.buyNow > 0 && amount >= l.buyNow) return buy(sp, lotId, now);
        long min = AuctionMath.minNextBid(l.startPrice, l.bid, l.hasBid());
        if (amount < min) return err("low", "Giá phải từ %s trở lên.", "Bid must be at least %s.", TuTinhText.of(min));
        boolean raise = sp.getUUID().equals(l.bidder);
        long need = raise ? amount - l.bid : amount;
        if (!Wallet.pay(sp, need)) return err("funds", "Không đủ Tử Tinh (cần %s).", "Not enough Tu Tinh (need %s).", TuTinhText.of(need));
        if (!raise) refundBidder(sp.server, l);
        l.bid = amount;
        l.bidder = sp.getUUID();
        l.bidderName = sp.getGameProfile().getName();
        d.setDirty();
        return null;
    }

    public static Component buy(ServerPlayer sp, long lotId, long now) {
        AuctionData d = AuctionData.get(sp.server);
        Lot l = d.lots.get(lotId);
        if (l == null || now >= l.endMs) return err("gone", "Lô này đã kết thúc hoặc không còn.", "This lot has ended or is gone.");
        if (l.buyNow <= 0) return err("nobuy", "Lô này không có giá mua ngay.", "This lot has no buy-now price.");
        if (sp.getUUID().equals(l.seller)) return err("self", "Không thể tự mua đồ của mình.", "You cannot buy your own lot.");
        boolean mine = sp.getUUID().equals(l.bidder);
        long need = mine ? l.buyNow - l.bid : l.buyNow;
        if (!Wallet.pay(sp, need)) return err("funds", "Không đủ Tử Tinh (cần %s).", "Not enough Tu Tinh (need %s).", TuTinhText.of(need));
        if (!mine) refundBidder(sp.server, l);
        l.bid = l.buyNow;
        l.bidder = sp.getUUID();
        l.bidderName = sp.getGameProfile().getName();
        finish(sp.server, d, l);
        return null;
    }

    public static Component cancel(ServerPlayer sp, long lotId) {
        AuctionData d = AuctionData.get(sp.server);
        Lot l = d.lots.get(lotId);
        if (l == null) return err("gone", "Lô này đã kết thúc hoặc không còn.", "This lot has ended or is gone.");
        if (!sp.getUUID().equals(l.seller)) return err("notmine", "Đây không phải lô của bạn.", "This is not your lot.");
        if (l.hasBid()) return err("hasbid", "Đã có người trả giá, không thể rút lại.", "Bids exist, you cannot withdraw.");
        d.lots.remove(lotId);
        d.setDirty();
        MailService.send(sp.server, l.seller, "hukhong.mail.sub.returned", l.stack.getHoverName().getString(), 0, l.stack);
        return null;
    }

    private static void refundBidder(MinecraftServer s, Lot l) {
        if (l.hasBid() && l.bid > 0)
            MailService.send(s, l.bidder, "hukhong.mail.sub.refund", l.stack.getHoverName().getString(), l.bid, ItemStack.EMPTY);
        l.bidder = null;
        l.bidderName = "";
        l.bid = 0;
    }

    // ---------------------------------------------------------------- ket thuc

    /** Lo co nguoi tra gia thang: nguoi ban nhan (gia - phi), phi bi dot, nguoi mua nhan do. Xoa lo. */
    public static void finish(MinecraftServer s, AuctionData d, Lot l) {
        int pct = AuctionConfig.get().saleFeePct;
        long fee = AuctionMath.saleFee(l.bid, pct);
        d.burned += fee;
        d.lots.remove(l.id);
        d.setDirty();
        String name = l.stack.getHoverName().getString();
        if (!l.npc()) MailService.send(s, l.seller, "hukhong.mail.sub.sold", name, l.bid - fee, ItemStack.EMPTY);
        MailService.send(s, l.bidder, "hukhong.mail.sub.won", name, 0, l.stack);
    }

    /** Xu ly mot lo het han. Tra true neu lo da bi xoa. */
    public static boolean settle(MinecraftServer s, AuctionData d, Lot l, long now) {
        if (now < l.endMs) return false;
        if (l.hasBid()) { finish(s, d, l); return true; }
        d.lots.remove(l.id);
        d.setDirty();
        String name = l.stack.getHoverName().getString();
        if (l.npc()) return true;
        long left = npcBudget(d, now);
        if (l.startPrice <= AuctionPricing.floor(l.stack) && left >= l.startPrice) { // NPC mua lai ton kho o gia san
            d.npcBudgetLeft = left - l.startPrice;
            long fee = AuctionMath.saleFee(l.startPrice, AuctionConfig.get().saleFeePct);
            d.burned += fee;
            MailService.send(s, l.seller, "hukhong.mail.sub.buyback", name, l.startPrice - fee, ItemStack.EMPTY);
            if (d.npcCount() < AuctionConfig.get().npcMaxLots) relistAsNpc(d, l.stack.copy(), now, (long) Math.ceil(AuctionPricing.floor(l.stack) * 1.3));
        } else {
            MailService.send(s, l.seller, "hukhong.mail.sub.returned", name, 0, l.stack);
        }
        return true;
    }

    /** Ngan sach NPC con lai trong ngay thuc hien tai (reset moi ngay). */
    public static long npcBudget(AuctionData d, long now) {
        long day = now / 86_400_000L;
        if (d.npcBudgetDay != day || d.npcBudgetLeft < 0) { d.npcBudgetDay = day; d.npcBudgetLeft = AuctionConfig.get().npcDailyBudget; d.setDirty(); }
        return d.npcBudgetLeft;
    }

    static void relistAsNpc(AuctionData d, ItemStack stack, long now, long price) {
        Lot n = new Lot(d.newId(), Lot.NIL, NPC_NAME, stack, Math.max(1, price), 0, now, now + AuctionMath.durationMs(24));
        d.lots.put(n.id, n);
        d.setDirty();
    }

    public static void tick(MinecraftServer s, long now) {
        AuctionData d = AuctionData.get(s);
        for (Iterator<Lot> it = new ArrayList<>(d.lots.values()).iterator(); it.hasNext(); ) settle(s, d, it.next(), now);
        AuctionConfig c = AuctionConfig.get();
        if (d.npcCount() < c.npcMaxLots / 2 && now - d.lastConsignMs > 600_000L) { // NPC ky gui them hang du
            d.lastConsignMs = now;
            Random r = new Random(now);
            for (int i = 0; i < 3 && d.npcCount() < c.npcMaxLots; i++) {
                ItemStack st = surplus(r);
                relistAsNpc(d, st, now, (long) Math.ceil(AuctionPricing.floor(st) * (1.2 + r.nextDouble() * 0.5)));
            }
            d.setDirty();
        }
    }

    /** Hang du cua server (NPC ky gui). */
    public static ItemStack surplus(Random r) {
        return switch (r.nextInt(9)) {
            case 0 -> new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8);
            case 1 -> new ItemStack(AlchemyRegistry.THANH_DIEP.get(), 6);
            case 2 -> new ItemStack(AlchemyRegistry.HUYET_THAM.get(), 3);
            case 3 -> new ItemStack(AlchemyRegistry.CHI_HUYET_DAN.get(), 2);
            case 4 -> new ItemStack(AlchemyRegistry.HOI_XUAN_DAN.get(), 2);
            case 5 -> new ItemStack(SkillBookRegistry.UPGRADE_SCROLL.get(), 1);
            case 6 -> SkillBookRewards.stack(r, r.nextInt(3) == 0 ? SkillGrade.HUYEN : SkillGrade.PHAM, null);
            case 7 -> HonCotRegistry.stack(Bone.values()[r.nextInt(Bone.values().length)], HonCotMath.TIER_TU, 40 + r.nextInt(30));
            default -> new ItemStack(ModItems.KHAI_MACH_DAN.get(), 1);
        };
    }

    // ---------------------------------------------------------------- truy van

    public static List<Lot> query(AuctionData d, UUID viewer, int filter, int sort, String search, long now) {
        String q = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
        List<Lot> out = new ArrayList<>();
        for (Lot l : d.lots.values()) {
            if (now >= l.endMs) continue;
            if (filter > 0 && filter < AuctionPricing.FILTERS && AuctionPricing.category(l.stack) != filter) continue;
            if (filter == AuctionPricing.FILTERS) { if (!viewer.equals(l.seller) && !viewer.equals(l.bidder)) continue; } // "Cua toi"
            if (!q.isEmpty() && !l.stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(l);
        }
        out.sort(switch (sort) {
            case 1 -> java.util.Comparator.comparingLong(Lot::price);
            case 2 -> java.util.Comparator.comparingLong(Lot::price).reversed();
            case 3 -> java.util.Comparator.comparingLong((Lot x) -> x.listedMs).reversed();
            default -> java.util.Comparator.comparingLong((Lot x) -> x.endMs);
        });
        return out;
    }
}
