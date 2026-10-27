package com.hukhong.structures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Cau truc da doc vao RAM: luoi BlockState (index = x + sx*(z + sz*y)), block entity va marker. */
public final class LoadedStructure {
    public final String name;
    public final int sx, sy, sz;
    public final BlockState[] states; // null = khong dat (air/khong co)
    public final List<Be> blockEntities = new ArrayList<>();
    public final List<Marker> markers = new ArrayList<>();

    public record Be(int x, int y, int z, CompoundTag tag) {}
    public record Marker(int x, int y, int z, String id) {
        public Marker {
            id = id.trim().toLowerCase(java.util.Locale.ROOT);
            if (id.startsWith("hukhong:")) id = id.substring(8);
        }
    }

    public LoadedStructure(String name, int sx, int sy, int sz) {
        this.name = name;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.states = new BlockState[sx * sy * sz];
    }

    public int idx(int x, int y, int z) { return x + sx * (z + sz * y); }

    public BlockState at(int x, int y, int z) { return states[idx(x, y, z)]; }

    /** Marker dau tien theo id ngan ("altar", "board", "npc", "linhkhi", "sect_board"); khong can tien to hukhong:. */
    public Marker marker(String id) {
        for (Marker m : markers) if (m.id().equals(id)) return m;
        return null;
    }

    public List<Marker> markers(String id) {
        List<Marker> out = new ArrayList<>();
        for (Marker m : markers) if (m.id().equals(id)) out.add(m);
        return out;
    }
}
