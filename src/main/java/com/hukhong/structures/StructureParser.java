package com.hukhong.structures;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Doc vanilla structure template (.nbt) va Sponge schematic v2/v3 (.schem). Khong phu thuoc mod ngoai. */
public final class StructureParser {
    private StructureParser() {}

    /** Gioi han kich thuoc (xem docs/GUIDE-v1-import-structures.md). */
    public static final int MAX_XZ = 256, MAX_Y = 192;

    public static LoadedStructure parse(String name, InputStream in) throws IOException {
        CompoundTag root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        if (root.contains("palette") || root.contains("palettes")) return vanilla(name, root);
        CompoundTag sch = root.contains("Schematic", Tag.TAG_COMPOUND) ? root.getCompound("Schematic") : root;
        if (sch.contains("Width")) return sponge(name, sch);
        throw new IOException("Unknown structure format: " + name);
    }

    private static void checkSize(String name, int x, int y, int z) throws IOException {
        if (x <= 0 || y <= 0 || z <= 0 || x > MAX_XZ || z > MAX_XZ || y > MAX_Y)
            throw new IOException("Structure " + name + " size " + x + "x" + y + "x" + z + " exceeds limit " + MAX_XZ + "x" + MAX_Y + "x" + MAX_XZ);
    }

    private static boolean skip(BlockState s) {
        return s.isAir() || s.is(Blocks.STRUCTURE_VOID) || s.is(Blocks.JIGSAW);
    }

    // ---- vanilla ----
    private static LoadedStructure vanilla(String name, CompoundTag root) throws IOException {
        ListTag size = root.getList("size", Tag.TAG_INT);
        int sx = size.getInt(0), sy = size.getInt(1), sz = size.getInt(2);
        checkSize(name, sx, sy, sz);
        ListTag pal = root.contains("palette") ? root.getList("palette", Tag.TAG_COMPOUND)
                : root.getList("palettes", Tag.TAG_LIST).getList(0);
        BlockState[] palette = new BlockState[pal.size()];
        var lookup = BuiltInRegistries.BLOCK.asLookup();
        for (int i = 0; i < palette.length; i++) {
            try {
                palette[i] = NbtUtils.readBlockState(lookup, pal.getCompound(i));
            } catch (RuntimeException e) {
                palette[i] = Blocks.AIR.defaultBlockState();
            }
        }
        LoadedStructure st = new LoadedStructure(name, sx, sy, sz);
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            ListTag p = b.getList("pos", Tag.TAG_INT);
            int x = p.getInt(0), y = p.getInt(1), z = p.getInt(2);
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) continue;
            int si = b.getInt("state");
            if (si < 0 || si >= palette.length) continue;
            BlockState s = palette[si];
            if (s.is(Blocks.STRUCTURE_BLOCK)) {
                String meta = b.getCompound("nbt").getString("metadata");
                if (!meta.isEmpty()) st.markers.add(new LoadedStructure.Marker(x, y, z, meta));
                continue;
            }
            if (skip(s)) continue;
            st.states[st.idx(x, y, z)] = s;
            if (b.contains("nbt", Tag.TAG_COMPOUND)) st.blockEntities.add(new LoadedStructure.Be(x, y, z, b.getCompound("nbt")));
        }
        return st;
    }

    // ---- sponge v2 / v3 ----
    private static LoadedStructure sponge(String name, CompoundTag sch) throws IOException {
        int sx = sch.getShort("Width") & 0xFFFF, sy = sch.getShort("Height") & 0xFFFF, sz = sch.getShort("Length") & 0xFFFF;
        checkSize(name, sx, sy, sz);
        boolean v3 = sch.getInt("Version") >= 3 || sch.contains("Blocks", Tag.TAG_COMPOUND);
        CompoundTag blocks = v3 ? sch.getCompound("Blocks") : sch;
        CompoundTag palTag = blocks.getCompound("Palette");
        byte[] data = v3 ? blocks.getByteArray("Data") : blocks.getByteArray("BlockData");
        Map<Integer, BlockState> palette = new HashMap<>();
        var lookup = BuiltInRegistries.BLOCK.asLookup();
        for (String key : palTag.getAllKeys()) {
            BlockState s;
            try {
                s = BlockStateParser.parseForBlock(lookup, key, false).blockState();
            } catch (CommandSyntaxException | RuntimeException e) {
                s = Blocks.AIR.defaultBlockState();
            }
            palette.put(palTag.getInt(key), s);
        }
        LoadedStructure st = new LoadedStructure(name, sx, sy, sz);
        int p = 0, i = 0, total = sx * sy * sz;
        while (p < data.length && i < total) {
            int v = 0, shift = 0, b;
            do {
                b = data[p++];
                v |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0 && p < data.length);
            // Sponge: index = x + z*Width + y*Width*Length (trung idx cua LoadedStructure)
            BlockState s = palette.get(v);
            if (s != null && !skip(s)) st.states[i] = s;
            i++;
        }
        for (Tag t : blocks.getList("BlockEntities", Tag.TAG_COMPOUND)) {
            CompoundTag be = (CompoundTag) t;
            int[] pos = be.getIntArray("Pos");
            if (pos.length < 3) continue;
            int x = pos[0], y = pos[1], z = pos[2];
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) continue;
            CompoundTag tag = v3 && be.contains("Data", Tag.TAG_COMPOUND) ? be.getCompound("Data").copy() : be.copy();
            tag.remove("Pos");
            tag.remove("Data");
            String id = be.getString("Id");
            if (!id.isEmpty()) tag.putString("id", id);
            BlockState s = st.states[st.idx(x, y, z)];
            if (s != null && s.is(Blocks.STRUCTURE_BLOCK)) {
                String meta = tag.getString("metadata");
                if (!meta.isEmpty()) st.markers.add(new LoadedStructure.Marker(x, y, z, meta));
                st.states[st.idx(x, y, z)] = null;
                continue;
            }
            st.blockEntities.add(new LoadedStructure.Be(x, y, z, tag));
        }
        for (int k = 0; k < st.states.length; k++)
            if (st.states[k] != null && st.states[k].is(Blocks.STRUCTURE_BLOCK)) st.states[k] = null;
        return st;
    }
}
