package com.hukhong.living;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** Nhiem vu ngau nhien dang lam cua mot nguoi choi. */
public final class Quest {
    /** 10 mau (PLAN-v1 F3). mult = he so thuong; minutes = han chot (phut thuc ~ tick/1200). */
    public enum Tmpl {
        ESCORT(1.6, 25), DELIVER(1.0, 30), FIND_LOST(1.2, 30), GATHER(0.9, 40), HUNT(1.1, 30),
        ELITE(2.0, 35), RUMOR(0.9, 25), GUARD_NIGHT(1.5, 45), STOLEN(1.7, 35), GUIDE(1.8, 35);

        public final double mult;
        public final int minutes;

        Tmpl(double mult, int minutes) { this.mult = mult; this.minutes = minutes; }

        public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public final String id;
    public final Tmpl tmpl;
    public long expire;
    public BlockPos target = BlockPos.ZERO, origin = BlockPos.ZERO;
    public int need = 1, have, stage;
    public String item = "";
    public UUID entity;
    public long reward;
    public boolean special;
    public int giverName;
    public UUID giver = new UUID(0, 0);

    public Quest(String id, Tmpl tmpl) { this.id = id; this.tmpl = tmpl; }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("id", id);
        t.putString("t", tmpl.name());
        t.putLong("exp", expire);
        t.putIntArray("tg", new int[]{target.getX(), target.getY(), target.getZ()});
        t.putIntArray("or", new int[]{origin.getX(), origin.getY(), origin.getZ()});
        t.putInt("need", need);
        t.putInt("have", have);
        t.putInt("stage", stage);
        t.putString("item", item);
        if (entity != null) t.putUUID("ent", entity);
        t.putLong("rw", reward);
        t.putBoolean("sp", special);
        t.putInt("gn", giverName);
        t.putUUID("giver", giver);
        return t;
    }

    public static Quest load(CompoundTag t) {
        Tmpl m;
        try { m = Tmpl.valueOf(t.getString("t")); } catch (IllegalArgumentException e) { return null; }
        Quest q = new Quest(t.getString("id"), m);
        q.expire = t.getLong("exp");
        int[] a = t.getIntArray("tg"), b = t.getIntArray("or");
        if (a.length == 3) q.target = new BlockPos(a[0], a[1], a[2]);
        if (b.length == 3) q.origin = new BlockPos(b[0], b[1], b[2]);
        q.need = t.getInt("need");
        q.have = t.getInt("have");
        q.stage = t.getInt("stage");
        q.item = t.getString("item");
        if (t.hasUUID("ent")) q.entity = t.getUUID("ent");
        q.reward = t.getLong("rw");
        q.special = t.getBoolean("sp");
        q.giverName = t.getInt("gn");
        if (t.hasUUID("giver")) q.giver = t.getUUID("giver");
        return q;
    }
}
