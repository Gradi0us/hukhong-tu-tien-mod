package com.hukhong.dongphu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Dong Phu (2026-11-06): moi nguoi choi 1 Trận Bàn; luu o overworld. Chu so huu -> vi tri, tang, moc dot kich gan nhat. */
public final class DongPhuData extends SavedData {
    static final String NAME = "hukhong_dongphu";

    /** Mot Động Phủ. dim = id chieu (vd minecraft:overworld); lastRaid = game-time overworld cua dot kich gan nhat. */
    public record Claim(UUID owner, ResourceLocation dim, BlockPos pos, int tier, long lastRaid) {
        public Claim withTier(int t) { return new Claim(owner, dim, pos, t, lastRaid); }
        public Claim withLastRaid(long t) { return new Claim(owner, dim, pos, tier, t); }
    }

    private final Map<UUID, Claim> claims = new HashMap<>();

    public static DongPhuData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DongPhuData::new, DongPhuData::load, null), NAME);
    }

    public Claim of(UUID owner) { return claims.get(owner); }

    public Collection<Claim> all() { return new ArrayList<>(claims.values()); }

    public void put(Claim c) { claims.put(c.owner(), c); setDirty(); }

    public void remove(UUID owner) { if (claims.remove(owner) != null) setDirty(); }

    public static DongPhuData load(CompoundTag tag, HolderLookup.Provider reg) {
        DongPhuData d = new DongPhuData();
        ListTag list = tag.getList("claims", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            UUID owner = c.getUUID("owner");
            ResourceLocation dim = ResourceLocation.parse(c.getString("dim"));
            d.claims.put(owner, new Claim(owner, dim, new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z")),
                    c.getInt("tier"), c.getLong("last")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        for (Claim c : claims.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("owner", c.owner());
            t.putString("dim", c.dim().toString());
            t.putInt("x", c.pos().getX());
            t.putInt("y", c.pos().getY());
            t.putInt("z", c.pos().getZ());
            t.putInt("tier", c.tier());
            t.putLong("last", c.lastRaid());
            list.add(t);
        }
        tag.put("claims", list);
        return tag;
    }
}
