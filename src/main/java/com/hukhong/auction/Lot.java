package com.hukhong.auction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Mot lo tren san Dau Gia Hanh. seller == NIL -> NPC ky gui. bidder == null -> chua ai tra gia. Thoi gian = ms thuc. */
public final class Lot {
    public static final UUID NIL = new UUID(0, 0);

    public final long id;
    public final UUID seller;
    public final String sellerName;
    public final ItemStack stack;
    public final long startPrice, buyNow, listedMs, endMs;
    public long bid;
    public UUID bidder;
    public String bidderName = "";

    public Lot(long id, UUID seller, String sellerName, ItemStack stack, long startPrice, long buyNow, long listedMs, long endMs) {
        this.id = id; this.seller = seller; this.sellerName = sellerName; this.stack = stack;
        this.startPrice = startPrice; this.buyNow = buyNow; this.listedMs = listedMs; this.endMs = endMs;
    }

    public boolean npc() { return NIL.equals(seller); }
    public boolean hasBid() { return bidder != null; }
    public long price() { return hasBid() ? bid : startPrice; }

    public CompoundTag save(HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        t.putLong("id", id);
        t.putUUID("seller", seller);
        t.putString("sn", sellerName);
        t.put("item", stack.save(reg));
        t.putLong("start", startPrice);
        t.putLong("buy", buyNow);
        t.putLong("listed", listedMs);
        t.putLong("end", endMs);
        t.putLong("bid", bid);
        if (bidder != null) { t.putUUID("bidder", bidder); t.putString("bn", bidderName); }
        return t;
    }

    public static Lot load(CompoundTag t, HolderLookup.Provider reg) {
        ItemStack s = ItemStack.parse(reg, t.getCompound("item")).orElse(ItemStack.EMPTY);
        if (s.isEmpty()) return null;
        Lot l = new Lot(t.getLong("id"), t.getUUID("seller"), t.getString("sn"), s, t.getLong("start"), t.getLong("buy"),
                t.getLong("listed"), t.getLong("end"));
        l.bid = t.getLong("bid");
        if (t.hasUUID("bidder")) { l.bidder = t.getUUID("bidder"); l.bidderName = t.getString("bn"); }
        return l;
    }
}
