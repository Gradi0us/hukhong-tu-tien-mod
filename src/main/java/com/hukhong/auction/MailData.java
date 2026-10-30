package com.hukhong.auction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Phi Kiem Truyen Thu: hom thu moi nguoi (SavedData). Thu = tien (Ha) + vat pham; tien vao vi ngay khi nhan, vat pham vao tui neu du cho. */
public class MailData extends SavedData {
    private static final String NAME = "hukhong_mail";
    public static final int MAX_PER_PLAYER = 40;

    /** claimed: da nhan het (giu lai lam lich su). */
    public static final class Mail {
        public final String subjectKey, arg;
        public long ha;
        public ItemStack item;
        public final long timeMs;
        public boolean claimed;

        public Mail(String subjectKey, String arg, long ha, ItemStack item, long timeMs) {
            this.subjectKey = subjectKey; this.arg = arg; this.ha = ha; this.item = item; this.timeMs = timeMs;
        }

        public boolean pending() { return ha > 0 || !item.isEmpty(); }
    }

    private final Map<UUID, List<Mail>> boxes = new LinkedHashMap<>();

    public static MailData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MailData::new, MailData::load, null), NAME);
    }

    public List<Mail> box(UUID p) { return boxes.computeIfAbsent(p, k -> new ArrayList<>()); }

    public List<Mail> peek(UUID p) { return boxes.getOrDefault(p, List.of()); }

    public int pendingCount(UUID p) {
        int n = 0;
        for (Mail m : peek(p)) if (m.pending()) n++;
        return n;
    }

    public void trim(UUID p) {
        List<Mail> b = box(p);
        while (b.size() > MAX_PER_PLAYER) {
            int i = 0;
            while (i < b.size() && b.get(i).pending()) i++;
            if (i >= b.size()) break;
            b.remove(i);
        }
    }

    public static MailData load(CompoundTag tag, HolderLookup.Provider reg) {
        MailData d = new MailData();
        CompoundTag all = tag.getCompound("boxes");
        for (String k : all.getAllKeys()) {
            UUID id;
            try { id = UUID.fromString(k); } catch (IllegalArgumentException e) { continue; }
            List<Mail> l = new ArrayList<>();
            for (Tag t : all.getList(k, Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                ItemStack s = c.contains("item") ? ItemStack.parse(reg, c.getCompound("item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
                Mail m = new Mail(c.getString("sub"), c.getString("arg"), c.getLong("ha"), s, c.getLong("t"));
                m.claimed = c.getBoolean("done");
                l.add(m);
            }
            d.boxes.put(id, l);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        CompoundTag all = new CompoundTag();
        boxes.forEach((k, v) -> {
            ListTag l = new ListTag();
            for (Mail m : v) {
                CompoundTag c = new CompoundTag();
                c.putString("sub", m.subjectKey);
                c.putString("arg", m.arg);
                c.putLong("ha", m.ha);
                if (!m.item.isEmpty()) c.put("item", m.item.save(reg));
                c.putLong("t", m.timeMs);
                c.putBoolean("done", m.claimed);
                l.add(c);
            }
            all.put(k.toString(), l);
        });
        tag.put("boxes", all);
        return tag;
    }
}
