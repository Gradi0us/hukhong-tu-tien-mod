package com.hukhong.living;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SavedData NPC song: thien cam (-100..100) theo phe va theo tung NPC, nhiem vu ngau nhien dang lam, thoi gian cho (moi nguoi choi). */
public class LivingData extends SavedData {
    private static final String NAME = "hukhong_living";
    public static final int MAX_NPC_KEYS = 64, MAX_QUESTS = 3;

    public static final class Life {
        /** Khoa f:PHE hoac n:uuid -> thien cam. Thu tu chen (cu nhat truoc) de cat bot khoa NPC. */
        public final LinkedHashMap<String, Integer> aff = new LinkedHashMap<>();
        public final List<Quest> quests = new ArrayList<>();
        /** Khoa t:uuid (noi chuyen) / q:uuid (de nghi nhiem vu) -> gameTime lan cuoi. */
        public final LinkedHashMap<String, Long> cd = new LinkedHashMap<>();
        public int done;
    }

    private final Map<UUID, Life> lives = new LinkedHashMap<>();

    public static LivingData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LivingData::new, LivingData::load, null), NAME);
    }

    public Life of(UUID p) { return lives.computeIfAbsent(p, k -> new Life()); }

    public static LivingData load(CompoundTag tag, HolderLookup.Provider reg) {
        LivingData d = new LivingData();
        CompoundTag all = tag.getCompound("lives");
        for (String k : all.getAllKeys()) {
            UUID id;
            try { id = UUID.fromString(k); } catch (IllegalArgumentException e) { continue; }
            CompoundTag c = all.getCompound(k);
            Life l = d.of(id);
            CompoundTag a = c.getCompound("aff");
            for (String ak : a.getAllKeys()) l.aff.put(ak, a.getInt(ak));
            CompoundTag cd = c.getCompound("cd");
            for (String ck : cd.getAllKeys()) l.cd.put(ck, cd.getLong(ck));
            for (Tag t : c.getList("q", Tag.TAG_COMPOUND)) { Quest q = Quest.load((CompoundTag) t); if (q != null) l.quests.add(q); }
            l.done = c.getInt("done");
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        CompoundTag all = new CompoundTag();
        lives.forEach((k, l) -> {
            CompoundTag c = new CompoundTag();
            CompoundTag a = new CompoundTag();
            l.aff.forEach(a::putInt);
            c.put("aff", a);
            CompoundTag cd = new CompoundTag();
            l.cd.forEach(cd::putLong);
            c.put("cd", cd);
            ListTag q = new ListTag();
            for (Quest x : l.quests) q.add(x.save());
            c.put("q", q);
            c.putInt("done", l.done);
            all.put(k.toString(), c);
        });
        tag.put("lives", all);
        return tag;
    }
}
