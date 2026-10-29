package com.hukhong.world.calendar;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Trang thai lich: su kien dang dien ra, da bat dau, da bao truoc, nguoi da dung nut "Den". Key lan xuat hien = id@start. */
public class CalendarData extends SavedData {
    private static final String NAME = "hukhong_calendar";

    public record Act(String key, String defId, long start, long end, int city, int x, int z) {}

    private final Map<String, Act> active = new LinkedHashMap<>();
    private final Set<String> done = new LinkedHashSet<>();
    private final Map<String, Integer> announced = new LinkedHashMap<>();
    private final Set<String> used = new LinkedHashSet<>();

    public static CalendarData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CalendarData::new, CalendarData::load, null), NAME);
    }

    public Map<String, Act> active() { return active; }
    public boolean isDone(String key) { return done.contains(key); }
    public void markDone(String key) { if (done.add(key)) setDirty(); }
    public int announced(String key) { return announced.getOrDefault(key, 0); }
    public void setAnnounced(String key, int bits) { announced.put(key, bits); setDirty(); }
    public boolean isUsed(String key, UUID p) { return used.contains(key + "|" + p); }
    public void markUsed(String key, UUID p) { used.add(key + "|" + p); setDirty(); }
    public void addActive(Act a) { active.put(a.key(), a); setDirty(); }
    public void removeActive(String key) { if (active.remove(key) != null) setDirty(); }

    public static long startOf(String key) {
        int i = key.indexOf('@');
        if (i < 0) return 0;
        int j = key.indexOf('|', i);
        try { return Long.parseLong(key.substring(i + 1, j < 0 ? key.length() : j)); } catch (NumberFormatException e) { return 0; }
    }

    /** Xoa ban ghi cu hon {@code before} tick. */
    public void prune(long before) {
        boolean ch = done.removeIf(k -> startOf(k) < before);
        ch |= announced.keySet().removeIf(k -> startOf(k) < before);
        ch |= used.removeIf(k -> startOf(k) < before);
        if (ch) setDirty();
    }

    public static CalendarData load(CompoundTag tag, HolderLookup.Provider reg) {
        CalendarData d = new CalendarData();
        for (Tag t : tag.getList("done", Tag.TAG_STRING)) d.done.add(t.getAsString());
        for (Tag t : tag.getList("used", Tag.TAG_STRING)) d.used.add(t.getAsString());
        for (Tag t : tag.getList("ann", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.announced.put(c.getString("k"), c.getInt("v"));
        }
        for (Tag t : tag.getList("active", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Act a = new Act(c.getString("key"), c.getString("def"), c.getLong("start"), c.getLong("end"), c.getInt("city"), c.getInt("x"), c.getInt("z"));
            d.active.put(a.key(), a);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag l = new ListTag();
        for (String s : done) l.add(StringTag.valueOf(s));
        tag.put("done", l);
        l = new ListTag();
        for (String s : used) l.add(StringTag.valueOf(s));
        tag.put("used", l);
        l = new ListTag();
        for (var e : announced.entrySet()) { CompoundTag c = new CompoundTag(); c.putString("k", e.getKey()); c.putInt("v", e.getValue()); l.add(c); }
        tag.put("ann", l);
        l = new ListTag();
        for (Act a : active.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("key", a.key()); c.putString("def", a.defId()); c.putLong("start", a.start()); c.putLong("end", a.end());
            c.putInt("city", a.city()); c.putInt("x", a.x()); c.putInt("z", a.z());
            l.add(c);
        }
        tag.put("active", l);
        return tag;
    }
}
