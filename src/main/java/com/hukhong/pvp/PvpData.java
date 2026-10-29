package com.hukhong.pvp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Du lieu ben vung cua Dau Truong: tam san (da dung), tien chua tra cho nguoi offline, dem tran theo cap/ngay Lich. */
public class PvpData extends SavedData {
    private static final String NAME = "hukhong_pvp";

    private BlockPos arena;                      // tam san (y = mat san)
    private final Map<UUID, Long> owed = new HashMap<>();
    private final Map<String, Integer> pairs = new HashMap<>();

    public static PvpData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(PvpData::new, PvpData::load, null), NAME);
    }

    public static PvpData load(CompoundTag tag, HolderLookup.Provider reg) {
        PvpData d = new PvpData();
        if (tag.contains("ax")) d.arena = new BlockPos(tag.getInt("ax"), tag.getInt("ay"), tag.getInt("az"));
        CompoundTag o = tag.getCompound("owed");
        for (String k : o.getAllKeys()) d.owed.put(UUID.fromString(k), o.getLong(k));
        CompoundTag p = tag.getCompound("pairs");
        for (String k : p.getAllKeys()) d.pairs.put(k, p.getInt(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        if (arena != null) { tag.putInt("ax", arena.getX()); tag.putInt("ay", arena.getY()); tag.putInt("az", arena.getZ()); }
        CompoundTag o = new CompoundTag();
        owed.forEach((k, v) -> o.putLong(k.toString(), v));
        tag.put("owed", o);
        CompoundTag p = new CompoundTag();
        pairs.forEach(p::putInt);
        tag.put("pairs", p);
        return tag;
    }

    public BlockPos arena() { return arena; }
    public void setArena(BlockPos p) { arena = p; setDirty(); }

    public int pairCount(String key) { return pairs.getOrDefault(key, 0); }
    public void bumpPair(String key, long gameTime) {
        PvpMath.bump(pairs, key);
        long day = Math.floorDiv(gameTime, PvpMath.DAY_TICKS);
        pairs.keySet().removeIf(k -> { int i = k.lastIndexOf('@'); return i > 0 && Long.parseLong(k.substring(i + 1)) < day - 1; });
        setDirty();
    }

    /** Tra tien cho nguoi choi: online -> vao vi ngay; offline -> ghi no, tra khi dang nhap. */
    public static void credit(MinecraftServer server, UUID who, long amount) {
        if (amount <= 0) return;
        ServerPlayer p = server.getPlayerList().getPlayer(who);
        if (p != null) { com.hukhong.currency.Wallet.give(p, amount); return; }
        PvpData d = get(server.overworld());
        d.owed.merge(who, amount, Long::sum);
        d.setDirty();
    }

    /** Tra no ton (khi dang nhap). @return so tien da tra. */
    public long settleOwed(ServerPlayer p) {
        Long v = owed.remove(p.getUUID());
        if (v == null) return 0;
        com.hukhong.currency.Wallet.give(p, v);
        setDirty();
        return v;
    }
}
