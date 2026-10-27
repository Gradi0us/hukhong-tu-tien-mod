package com.hukhong.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Vi tri Hiep Hoi o moi thanh / cang (id CityDef -> vi tri le tan). Chi o Overworld.
 * "current" = da dat theo quy tac moi (ben phai Ấn Hồn Đài); muc cu thieu co nay se bi go va dung lai khi nguoi choi toi gan.
 */
public class GuildHalls extends SavedData {
    private static final String NAME = "hukhong_guild_halls";
    private final Map<String, BlockPos> halls = new LinkedHashMap<>();
    private final Set<String> current = new HashSet<>();

    public static GuildHalls get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(GuildHalls::new, GuildHalls::load, null), NAME);
    }

    public static GuildHalls load(CompoundTag tag, HolderLookup.Provider reg) {
        GuildHalls d = new GuildHalls();
        CompoundTag h = tag.getCompound("halls");
        for (String k : h.getAllKeys()) {
            int[] a = h.getIntArray(k);
            if (a.length == 3) d.halls.put(k, new BlockPos(a[0], a[1], a[2]));
        }
        ListTag cur = tag.getList("current", Tag.TAG_STRING);
        for (int i = 0; i < cur.size(); i++) d.current.add(cur.getString(i));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        CompoundTag h = new CompoundTag();
        halls.forEach((k, v) -> h.putIntArray(k, new int[]{v.getX(), v.getY(), v.getZ()}));
        tag.put("halls", h);
        ListTag cur = new ListTag();
        for (String k : current) cur.add(StringTag.valueOf(k));
        tag.put("current", cur);
        return tag;
    }

    public BlockPos get(String cityId) { return halls.get(cityId); }
    public boolean has(String cityId) { return halls.containsKey(cityId); }
    /** Da dat theo quy tac vi tri moi. */
    public boolean isCurrent(String cityId) { return current.contains(cityId); }
    public Map<String, BlockPos> all() { return halls; }
    public void put(String cityId, BlockPos p) { halls.put(cityId, p); current.add(cityId); setDirty(); }
}
