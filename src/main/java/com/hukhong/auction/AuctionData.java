package com.hukhong.auction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** SavedData Dau Gia (overworld): lo, tong Tu Tinh da dot, toa do hoi truong, ngan sach NPC, tien dang giu trong Dau Gia Hoi. */
public class AuctionData extends SavedData {
    private static final String NAME = "hukhong_auction";

    public final Map<Long, Lot> lots = new LinkedHashMap<>();
    public long nextId = 1, burned;
    /** cityId -> vi tri NPC Dau Gia Su (y = chan). */
    public final Map<String, BlockPos> halls = new LinkedHashMap<>();
    public String lastLiveKey = "";
    public long npcBudgetLeft = -1, npcBudgetDay = -1, lastConsignMs;
    /** Tien nguoi choi dang giu trong Dau Gia Hoi (hoan lai bang thu neu may chu dung giua chung). */
    public final Map<UUID, Long> liveEscrow = new LinkedHashMap<>();

    public static AuctionData get(MinecraftServer s) { return get(s.overworld()); }

    public static AuctionData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(AuctionData::new, AuctionData::load, null), NAME);
    }

    public long newId() { setDirty(); return nextId++; }

    public int countOf(UUID seller) {
        int n = 0;
        for (Lot l : lots.values()) if (seller.equals(l.seller)) n++;
        return n;
    }

    public int npcCount() {
        int n = 0;
        for (Lot l : lots.values()) if (l.npc()) n++;
        return n;
    }

    public static AuctionData load(CompoundTag tag, HolderLookup.Provider reg) {
        AuctionData d = new AuctionData();
        d.nextId = Math.max(1, tag.getLong("next"));
        d.burned = tag.getLong("burned");
        d.lastLiveKey = tag.getString("liveKey");
        d.npcBudgetLeft = tag.contains("npcLeft") ? tag.getLong("npcLeft") : -1;
        d.npcBudgetDay = tag.contains("npcDay") ? tag.getLong("npcDay") : -1;
        d.lastConsignMs = tag.getLong("consign");
        for (Tag t : tag.getList("lots", Tag.TAG_COMPOUND)) {
            Lot l = Lot.load((CompoundTag) t, reg);
            if (l != null) d.lots.put(l.id, l);
        }
        CompoundTag h = tag.getCompound("halls");
        for (String k : h.getAllKeys()) { int[] a = h.getIntArray(k); if (a.length == 3) d.halls.put(k, new BlockPos(a[0], a[1], a[2])); }
        CompoundTag e = tag.getCompound("escrow");
        for (String k : e.getAllKeys()) { try { d.liveEscrow.put(UUID.fromString(k), e.getLong(k)); } catch (IllegalArgumentException ignored) { } }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putLong("next", nextId);
        tag.putLong("burned", burned);
        tag.putString("liveKey", lastLiveKey);
        tag.putLong("npcLeft", npcBudgetLeft);
        tag.putLong("npcDay", npcBudgetDay);
        tag.putLong("consign", lastConsignMs);
        ListTag l = new ListTag();
        for (Lot x : lots.values()) l.add(x.save(reg));
        tag.put("lots", l);
        CompoundTag h = new CompoundTag();
        halls.forEach((k, v) -> h.putIntArray(k, new int[]{v.getX(), v.getY(), v.getZ()}));
        tag.put("halls", h);
        CompoundTag e = new CompoundTag();
        liveEscrow.forEach((k, v) -> e.putLong(k.toString(), v));
        tag.put("escrow", e);
        return tag;
    }
}
