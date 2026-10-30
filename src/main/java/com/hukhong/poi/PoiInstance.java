package com.hukhong.poi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Mot dia diem da dung trong the gioi. acts = diem tuong tac (kind: stele, offer, tablet, lever:N, vault, grave, chest, claim, pillar:N, lore_tablet...)
 * va diem sinh quai (sp_room, sp_boss). state: READY -> ACTIVE (quai dang canh) -> CLEARED (cho hoi sinh sau respawnTicks).
 * extra: du lieu phu (cooldown nguoi choi "cd:uuid:kind", so lan nghe tin don "rumor:uuid", "claimed:uuid"...).
 */
public final class PoiInstance {
    public static final int READY = 0, ACTIVE = 1, CLEARED = 2;

    public record Act(String kind, int x, int y, int z) {
        public BlockPos pos() { return new BlockPos(x, y, z); }
        public long key() { return BlockPos.asLong(x, y, z); }
    }

    public final String key, defId;
    public final int x, y, z, seed;
    public final List<Act> acts = new ArrayList<>();
    public int state = READY;
    public long stateTick;
    public boolean solved;
    public int progress;
    public CompoundTag extra = new CompoundTag();

    public PoiInstance(String defId, int x, int y, int z, int seed) {
        this.defId = defId; this.x = x; this.y = y; this.z = z; this.seed = seed;
        this.key = keyOf(defId, x, z);
    }

    public static String keyOf(String defId, int x, int z) { return defId + "@" + x + "," + z; }

    public static String defOfKey(String key) { int i = key.indexOf('@'); return i < 0 ? key : key.substring(0, i); }

    public BlockPos origin() { return new BlockPos(x, y, z); }

    public Act firstAct(String kind) { for (Act a : acts) if (a.kind().equals(kind)) return a; return null; }

    public List<Act> actsOf(String kind) {
        List<Act> l = new ArrayList<>();
        for (Act a : acts) if (a.kind().equals(kind)) l.add(a);
        return l;
    }

    public double dist2(double px, double pz) { double dx = px - x, dz = pz - z; return dx * dx + dz * dz; }

    // ---- NBT ----
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("def", defId);
        t.putInt("x", x); t.putInt("y", y); t.putInt("z", z); t.putInt("seed", seed);
        t.putInt("state", state); t.putLong("tick", stateTick);
        t.putBoolean("solved", solved); t.putInt("progress", progress);
        t.put("extra", extra.copy());
        ListTag l = new ListTag();
        for (Act a : acts) {
            CompoundTag c = new CompoundTag();
            c.putString("k", a.kind()); c.putInt("x", a.x()); c.putInt("y", a.y()); c.putInt("z", a.z());
            l.add(c);
        }
        t.put("acts", l);
        return t;
    }

    public static PoiInstance load(CompoundTag t) {
        PoiInstance i = new PoiInstance(t.getString("def"), t.getInt("x"), t.getInt("y"), t.getInt("z"), t.getInt("seed"));
        i.state = t.getInt("state"); i.stateTick = t.getLong("tick");
        i.solved = t.getBoolean("solved"); i.progress = t.getInt("progress");
        i.extra = t.getCompound("extra");
        for (Tag g : t.getList("acts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) g;
            i.acts.add(new Act(c.getString("k"), c.getInt("x"), c.getInt("y"), c.getInt("z")));
        }
        return i;
    }
}
