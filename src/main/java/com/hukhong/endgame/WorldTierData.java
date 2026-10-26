package com.hukhong.endgame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Trạng thái cấp thế giới (lưu ở Overworld): Tầng Thế Giới, vị trí cổng Hư Không Bản Nguyên, đảo đã dựng chưa. */
public class WorldTierData extends SavedData {
    private static final String NAME = "hukhong_world_tier";
    private int tier = EndgameKit.MIN_TIER;
    private boolean hasGateway, arenaBuilt;
    private long gateway;

    public static WorldTierData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldTierData::new, WorldTierData::load, null), NAME);
    }

    public static WorldTierData load(CompoundTag tag, HolderLookup.Provider reg) {
        WorldTierData d = new WorldTierData();
        d.tier = EndgameKit.clampTier(tag.contains("tier") ? tag.getInt("tier") : 1);
        d.hasGateway = tag.getBoolean("hasGateway");
        d.gateway = tag.getLong("gateway");
        d.arenaBuilt = tag.getBoolean("arenaBuilt");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putInt("tier", tier);
        tag.putBoolean("hasGateway", hasGateway);
        tag.putLong("gateway", gateway);
        tag.putBoolean("arenaBuilt", arenaBuilt);
        return tag;
    }

    public int tier() { return tier; }
    public void setTier(int t) { tier = EndgameKit.clampTier(t); setDirty(); }
    public BlockPos gateway() { return hasGateway ? BlockPos.of(gateway) : null; }
    public void setGateway(BlockPos p) { hasGateway = true; gateway = p.asLong(); setDirty(); }
    public boolean arenaBuilt() { return arenaBuilt; }
    public void setArenaBuilt() { arenaBuilt = true; setDirty(); }
}
