package com.hukhong.bicanh;

import com.hukhong.bicanh.BiCanhLayout.Kind;
import com.hukhong.bicanh.BiCanhLayout.Room;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Dựng / dọn chuỗi phòng của một instance trong dimension Bí Cảnh (rỗng, void). */
public final class BiCanhArena {
    private BiCanhArena() {}

    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private record Palette(Block wall, Block floor) {}

    private static Palette palette(BiCanhTier t) {
        return switch (t) {
            case BACH -> new Palette(Blocks.QUARTZ_BRICKS, Blocks.SMOOTH_QUARTZ);
            case HOANG -> new Palette(Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
            case TU -> new Palette(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR);
            case HAC -> new Palette(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.DEEPSLATE_TILES);
            case HONG -> new Palette(Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS);
        };
    }

    private static void fill(ServerLevel lv, int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) lv.setBlock(p.set(x, y, z), s, FLAGS);
    }

    /** Vùng bao toàn bộ chuỗi: [x1,y1,z1,x2,y2,z2]. */
    public static int[] bounds(BlockPos o, List<Room> rooms) {
        return new int[]{o.getX() - 1, o.getY() - 1, o.getZ() - BiCanhLayout.HALF_Z - 1,
                o.getX() + BiCanhLayout.endX(rooms), o.getY() + BiCanhLayout.HEIGHT, o.getZ() + BiCanhLayout.HALF_Z + 1};
    }

    public static void clear(ServerLevel lv, BlockPos o, List<Room> rooms) {
        int[] b = bounds(o, rooms);
        fill(lv, b[0], b[1], b[2], b[3], b[4], b[5], Blocks.AIR.defaultBlockState());
    }

    /** Đặt / gỡ cửa song sắt ở đầu hành lang phía đông của phòng r. */
    public static void setDoor(ServerLevel lv, BlockPos o, Room r, boolean closed) {
        int x = o.getX() + r.x0() + BiCanhLayout.ROOM_LEN;
        fill(lv, x, o.getY(), o.getZ() - 1, x, o.getY() + 2, o.getZ() + 1,
                (closed ? Blocks.IRON_BARS : Blocks.AIR).defaultBlockState());
    }

    public static void build(ServerLevel lv, BlockPos o, BiCanhTier t, long seed, List<Room> rooms) {
        Palette pal = palette(t);
        BlockState wall = pal.wall().defaultBlockState(), floor = pal.floor().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState(), lamp = Blocks.SEA_LANTERN.defaultBlockState();
        int ox = o.getX(), oy = o.getY(), oz = o.getZ(), h = BiCanhLayout.HEIGHT, L = BiCanhLayout.ROOM_LEN, hz = BiCanhLayout.HALF_Z;
        int[] b = bounds(o, rooms);
        fill(lv, b[0], b[1], b[2], b[3], b[4], b[5], wall);
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            int x0 = ox + r.x0();
            fill(lv, x0, oy, oz - hz, x0 + L - 1, oy + h - 1, oz + hz, air);
            fill(lv, x0, oy - 1, oz - hz, x0 + L - 1, oy - 1, oz + hz, floor);
            if (i < rooms.size() - 1) { // hành lang sang phòng kế
                fill(lv, x0 + L, oy, oz - 1, x0 + BiCanhLayout.PITCH - 1, oy + 2, oz + 1, air);
                fill(lv, x0 + L, oy - 1, oz - 1, x0 + BiCanhLayout.PITCH - 1, oy - 1, oz + 1, floor);
                lv.setBlock(new BlockPos(x0 + L + 2, oy + 3, oz), lamp, FLAGS);
            }
            for (int lx : new int[]{3, 11})
                for (int lz : new int[]{-4, 4}) lv.setBlock(new BlockPos(x0 + lx, oy + h, oz + lz), lamp, FLAGS);
            for (int[] pl : BiCanhLayout.pillars(seed, r))
                fill(lv, ox + pl[0], oy, oz + pl[1], ox + pl[0], oy + h - 2, oz + pl[1], wall);
            if (r.kind() == Kind.WAVE) setDoor(lv, o, r, true);
        }
    }
}
