package com.hukhong.bicanh;

import com.hukhong.bicanh.BiCanhLayout.Kind;
import com.hukhong.bicanh.BiCanhLayout.Room;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

/**
 * Dựng / dọn chuỗi phòng của một instance trong dimension Bí Cảnh (rỗng, void).
 * Mỗi cấp có chủ đề riêng (vật liệu, ánh sáng, banner); mỗi phòng một bố cục (vòng nghi lễ, hành lang trụ, đài cao,
 * hồ góc, hốc bên) chọn theo seed; phòng boss luôn là đài cao trên vòng nghi lễ.
 */
public final class BiCanhArena {
    private BiCanhArena() {}

    private static final int FLAGS = Block.UPDATE_CLIENTS;
    /** Cửa song sắt: rộng |z| <= 2, cao 4 hàng. */
    public static final int DOOR_ROWS = 4, DOOR_HALF = 2;
    /** Lề z thêm cho hốc bên. */
    private static final int Z_MARGIN = 4;

    private record Theme(Block wall, Block floor, Block floor2, Block accent, Block pillar, Block cap, Block light,
                         Block banner, Block liquid) {}

    private static Theme theme(BiCanhTier t) {
        return switch (t) {
            case BACH -> new Theme(Blocks.QUARTZ_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.POLISHED_DIORITE, Blocks.LIGHT_BLUE_CONCRETE,
                    Blocks.QUARTZ_PILLAR, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.SEA_LANTERN, Blocks.LIGHT_BLUE_WALL_BANNER, Blocks.WATER);
            case HOANG -> new Theme(Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.ORANGE_TERRACOTTA,
                    Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.GLOWSTONE, Blocks.ORANGE_WALL_BANNER, Blocks.WATER);
            case TU -> new Theme(Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.AMETHYST_BLOCK,
                    Blocks.PURPUR_PILLAR, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.PEARLESCENT_FROGLIGHT, Blocks.PURPLE_WALL_BANNER, Blocks.WATER);
            case HAC -> new Theme(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.GILDED_BLACKSTONE,
                    Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.VERDANT_FROGLIGHT, Blocks.BLACK_WALL_BANNER, Blocks.LAVA);
            case HONG -> new Theme(Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.SHROOMLIGHT,
                    Blocks.NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.SHROOMLIGHT, Blocks.RED_WALL_BANNER, Blocks.LAVA);
        };
    }

    /** Ngữ cảnh dựng: toạ độ tương đối phòng (lx từ x0 của phòng, lz quanh trục). */
    private static final class C {
        final ServerLevel lv; final int ox, oy, oz; final Theme th;
        int rx; // x tuyệt đối của x0 phòng đang dựng
        C(ServerLevel lv, BlockPos o, Theme th) { this.lv = lv; this.ox = o.getX(); this.oy = o.getY(); this.oz = o.getZ(); this.th = th; }
        void set(int lx, int dy, int lz, Block b) { lv.setBlock(new BlockPos(rx + lx, oy + dy, oz + lz), b.defaultBlockState(), FLAGS); }
        void set(int lx, int dy, int lz, BlockState s) { lv.setBlock(new BlockPos(rx + lx, oy + dy, oz + lz), s, FLAGS); }
        void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
            BlockState s = b.defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++)
                lv.setBlock(p.set(rx + x, oy + y, oz + z), s, FLAGS);
        }
        boolean air(int lx, int dy, int lz) { return lv.getBlockState(new BlockPos(rx + lx, oy + dy, oz + lz)).isAir(); }
    }

    private static void fill(ServerLevel lv, int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) lv.setBlock(p.set(x, y, z), s, FLAGS);
    }

    /** Vùng bao toàn bộ chuỗi: [x1,y1,z1,x2,y2,z2]. */
    public static int[] bounds(BlockPos o, List<Room> rooms) {
        return new int[]{o.getX() - 1, o.getY() - 2, o.getZ() - BiCanhLayout.HALF_Z - Z_MARGIN - 1,
                o.getX() + BiCanhLayout.endX(rooms), o.getY() + BiCanhLayout.HEIGHT, o.getZ() + BiCanhLayout.HALF_Z + Z_MARGIN + 1};
    }

    public static void clear(ServerLevel lv, BlockPos o, List<Room> rooms) {
        int[] b = bounds(o, rooms);
        fill(lv, b[0], b[1], b[2], b[3], b[4], b[5], Blocks.AIR.defaultBlockState());
    }

    private static int doorX(BlockPos o, Room r) { return o.getX() + r.x0() + BiCanhLayout.ROOM_LEN; }

    /** Đặt / gỡ toàn bộ cửa song sắt ở đầu hành lang phía đông của phòng r. */
    public static void setDoor(ServerLevel lv, BlockPos o, Room r, boolean closed) {
        for (int row = 0; row < DOOR_ROWS; row++) setDoorRow(lv, o, r, row, closed);
    }

    /** Một hàng cửa (row 0 = sát sàn). */
    public static void setDoorRow(ServerLevel lv, BlockPos o, Room r, int row, boolean closed) {
        int x = doorX(o, r), y = o.getY() + row;
        fill(lv, x, y, o.getZ() - DOOR_HALF, x, y, o.getZ() + DOOR_HALF,
                (closed ? Blocks.IRON_BARS : Blocks.AIR).defaultBlockState());
    }

    /** Tâm phòng (toạ độ thế giới, mép sàn). */
    public static BlockPos roomCenter(BlockPos o, Room r) { return new BlockPos(o.getX() + r.x0() + BiCanhLayout.ROOM_LEN / 2, o.getY(), o.getZ()); }

    public static void build(ServerLevel lv, BlockPos o, BiCanhTier t, long seed, List<Room> rooms) {
        Theme th = theme(t);
        C c = new C(lv, o, th);
        int[] b = bounds(o, rooms);
        fill(lv, b[0], b[1], b[2], b[3], b[4], b[5], th.wall().defaultBlockState());
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            c.rx = o.getX() + r.x0();
            Random rnd = new Random(seed * 31 + i * 977L);
            int layout = BiCanhLayout.layoutOf(seed, r);
            boolean boss = r.kind() == Kind.BOSS;
            carve(c);
            floor(c, layout == 0 || boss || r.kind() == Kind.ENTRY);
            ceiling(c);
            walls(c, r);
            switch (layout) {
                case 0 -> ritual(c, r.kind() == Kind.ENTRY);
                case 1 -> colonnade(c);
                case 2 -> dais(c, boss);
                case 3 -> pools(c);
                default -> alcoves(c);
            }
            if (i < rooms.size() - 1) corridor(c);
            if (r.kind() == Kind.WAVE) setDoor(lv, o, r, true);
        }
    }

    // ------------------------------------------------------------ khung phòng

    private static void carve(C c) {
        int L = BiCanhLayout.ROOM_LEN, hz = BiCanhLayout.HALF_Z, h = BiCanhLayout.HEIGHT;
        c.fill(0, 0, -hz, L - 1, h - 1, hz, Blocks.AIR);
    }

    private static void floor(C c, boolean circle) {
        int L = BiCanhLayout.ROOM_LEN, hz = BiCanhLayout.HALF_Z, cx = L / 2;
        Theme th = c.th;
        for (int lx = 0; lx < L; lx++) for (int lz = -hz; lz <= hz; lz++) {
            Block b = ((lx + lz) & 1) == 0 ? th.floor() : th.floor2();
            if (Math.abs(lz) == hz) b = th.accent();                 // viền chân tường
            else if (lx == 0) b = th.accent();
            if (circle && Math.abs(lz) != hz) {
                double d = Math.hypot(lx - cx, lz);
                boolean spoke = lz == 0 || lx == cx || Math.abs(lx - cx) == Math.abs(lz);
                if (Math.abs(d - 9.0) < 0.75) b = th.accent();
                else if (Math.abs(d - 5.5) < 0.6) b = th.floor2();
                else if (d <= 3.2) b = th.light();                    // lõi phát sáng
                else if (spoke && d < 9 && d > 3.2) b = ((int) d & 1) == 0 ? th.accent() : th.light(); // tia phù văn
            }
            c.set(lx, -1, lz, b);
        }
    }

    private static void ceiling(C c) {
        int L = BiCanhLayout.ROOM_LEN, hz = BiCanhLayout.HALF_Z, h = BiCanhLayout.HEIGHT;
        Theme th = c.th;
        // xà ngang mỗi 6 ô + đèn trần + đèn chùm treo xà
        for (int lx = 0; lx < L; lx++) {
            if (lx % 6 == 0) for (int lz = -hz; lz <= hz; lz++) c.set(lx, h - 1, lz, th.cap());
        }
        for (int lx : new int[]{3, 9, 15, 21}) for (int lz : new int[]{-6, 0, 6}) c.set(lx, h, lz, th.light());
        for (int lx : new int[]{6, 12, 18}) for (int lz : new int[]{-6, 6}) {
            c.set(lx, h - 2, lz, Blocks.CHAIN);
            c.set(lx, h - 3, lz, Blocks.CHAIN);
            c.set(lx, h - 4, lz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    private static void walls(C c, Room r) {
        int L = BiCanhLayout.ROOM_LEN, hz = BiCanhLayout.HALF_Z;
        Theme th = c.th;
        for (int lx = 3; lx < L - 1; lx += 6) { // banner hai bên + đèn tường
            c.set(lx, 4, -hz, th.banner().defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH));
            c.set(lx, 4, hz, th.banner().defaultBlockState().setValue(WallBannerBlock.FACING, Direction.NORTH));
        }
        for (int lx = 6; lx < L; lx += 6) {
            c.set(lx, 3, -hz - 1, th.light());
            c.set(lx, 3, hz + 1, th.light());
        }
        // chân tường: gờ trụ
        for (int lx = 0; lx < L; lx += 6) for (int h = 0; h < BiCanhLayout.HEIGHT - 1; h++) {
            c.set(lx, h, -hz, h == 0 || h == BiCanhLayout.HEIGHT - 2 ? th.cap() : th.pillar());
            c.set(lx, h, hz, h == 0 || h == BiCanhLayout.HEIGHT - 2 ? th.cap() : th.pillar());
        }
    }

    private static void brazier(C c, int lx, int lz, int baseY, int height) {
        c.set(lx, baseY, lz, c.th.cap());
        for (int h = 1; h < height; h++) c.set(lx, baseY + h, lz, c.th.pillar());
        c.set(lx, baseY + height, lz, c.th.cap());
        c.set(lx, baseY + height + 1, lz, Blocks.SOUL_CAMPFIRE);
    }

    // ------------------------------------------------------------ bố cục

    /** 0: vòng nghi lễ (floor đã vẽ vòng); bốn lư lửa ở góc. */
    private static void ritual(C c, boolean entry) {
        int L = BiCanhLayout.ROOM_LEN, cx = L / 2;
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) brazier(c, cx + sx * 8, sz * 8, 0, 2);
        if (entry) for (int lz : new int[]{-3, 3}) { // cổng chào hai bên lối ra
            brazier(c, L - 3, lz, 0, 3);
        }
    }

    /** 1: hành lang trụ, hai hàng trụ lz=±6, thảm lối đi. */
    private static void colonnade(C c) {
        int L = BiCanhLayout.ROOM_LEN, h = BiCanhLayout.HEIGHT;
        for (int lx = 4; lx <= L - 5; lx += 4) for (int sz = -1; sz <= 1; sz += 2) {
            int lz = sz * 6;
            c.set(lx, 0, lz, c.th.cap());
            for (int y = 1; y <= h - 3; y++) c.set(lx, y, lz, c.th.pillar());
            c.set(lx, h - 2, lz, c.th.cap());
        }
        for (int lx = 2; lx < L - 2; lx++) for (int lz = -1; lz <= 1; lz++)
            c.set(lx, -1, lz, (lx & 1) == 0 ? c.th.accent() : c.th.light());
        for (int lx = 8; lx <= L - 9; lx += 8) for (int sz = -1; sz <= 1; sz += 2) // lư lửa giữa các cột
            c.set(lx, 0, sz * 9, Blocks.SOUL_CAMPFIRE);
    }

    /** 2: đài cao 1 ô giữa phòng + bậc thang bao quanh; phòng boss thêm cột lớn. */
    private static void dais(C c, boolean boss) {
        int L = BiCanhLayout.ROOM_LEN, cx = L / 2;
        Theme th = c.th;
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            int m = Math.max(Math.abs(dx), Math.abs(dz));
            if (m == 5) {
                if (Math.abs(dx) == 5 && Math.abs(dz) == 5) continue;
                Direction f = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                c.set(cx + dx, 0, dz, stairOf(th).defaultBlockState().setValue(StairBlock.FACING, f));
            } else {
                Block b = m == 4 ? th.accent() : ((dx + dz) & 1) == 0 ? th.floor() : th.floor2();
                if (m <= 1) b = th.light();
                c.set(cx + dx, 0, dz, b);
            }
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) brazier(c, cx + sx * 4, sz * 4, 1, 3);
        if (boss) for (int sz = -1; sz <= 1; sz += 2) { // cột lớn hai bên tường sau
            int lz = sz * 8, lx = cx + 6;
            for (int y = 0; y <= BiCanhLayout.HEIGHT - 2; y++) {
                c.set(lx, y, lz, y == 0 || y == BiCanhLayout.HEIGHT - 2 ? th.cap() : th.pillar());
                c.set(lx - 12, y, lz, y == 0 || y == BiCanhLayout.HEIGHT - 2 ? th.cap() : th.pillar());
            }
        }
    }

    private static Block stairOf(Theme th) {
        Block w = th.wall();
        if (w == Blocks.QUARTZ_BRICKS) return Blocks.QUARTZ_STAIRS;
        if (w == Blocks.CUT_SANDSTONE) return Blocks.SANDSTONE_STAIRS;
        if (w == Blocks.PURPUR_BLOCK) return Blocks.PURPUR_STAIRS;
        if (w == Blocks.POLISHED_BLACKSTONE_BRICKS) return Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS;
        return Blocks.NETHER_BRICK_STAIRS;
    }

    /** 3: bốn hồ góc (nước/dung nham), vành thấp, tia chữ thập giữa phòng. */
    private static void pools(C c) {
        int L = BiCanhLayout.ROOM_LEN, cx = L / 2;
        Theme th = c.th;
        for (int sx : new int[]{3, L - 8}) for (int sz = -1; sz <= 1; sz += 2) {
            int zc = sz * 7;
            for (int dx = 0; dx < 5; dx++) for (int dz = -2; dz <= 2; dz++) {
                int lx = sx + dx, lz = zc + dz;
                boolean rim = dx == 0 || dx == 4 || Math.abs(dz) == 2;
                if (rim) c.set(lx, -1, lz, th.accent());
                else { c.set(lx, -2, lz, Blocks.STONE); c.set(lx, -1, lz, th.liquid()); }
            }
            c.set(sx + 2, 0, zc, th.liquid() == Blocks.WATER ? Blocks.LILY_PAD : Blocks.AIR);
        }
        for (int lx = 2; lx < L - 2; lx++) c.set(lx, -1, 0, ((lx & 1) == 0) ? th.accent() : th.light());
        for (int lz = -4; lz <= 4; lz++) c.set(cx, -1, lz, ((lz & 1) == 0) ? th.accent() : th.light());
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) brazier(c, cx + sx * 3, sz * 3, 0, 2);
    }

    /** 4: ba hốc mỗi bên tường với bệ đá + lư lửa. */
    private static void alcoves(C c) {
        int hz = BiCanhLayout.HALF_Z;
        Theme th = c.th;
        for (int ax : new int[]{5, 12, 19}) for (int sz = -1; sz <= 1; sz += 2) {
            for (int dz = 1; dz <= 3; dz++) {
                int lz = sz * (hz + dz);
                for (int lx = ax - 1; lx <= ax + 1; lx++) {
                    for (int y = 0; y <= 3; y++) c.set(lx, y, lz, Blocks.AIR);
                    c.set(lx, -1, lz, ((lx + dz) & 1) == 0 ? th.floor() : th.floor2());
                }
            }
            for (int lx = ax - 1; lx <= ax + 1; lx++) c.set(lx, 3, sz * (hz + 3), th.light());
            c.set(ax, 0, sz * (hz + 3), th.cap());
            c.set(ax, 1, sz * (hz + 3), Blocks.SOUL_LANTERN);
            c.set(ax - 1, 0, sz * (hz + 2), th.pillar());
            c.set(ax + 1, 0, sz * (hz + 2), th.pillar());
        }
        int L = BiCanhLayout.ROOM_LEN;
        for (int lx = 2; lx < L - 2; lx++) for (int lz = -1; lz <= 1; lz++) c.set(lx, -1, lz, lz == 0 ? th.light() : th.accent());
    }

    // ------------------------------------------------------------ hành lang

    private static void corridor(C c) {
        int L = BiCanhLayout.ROOM_LEN, P = BiCanhLayout.PITCH;
        Theme th = c.th;
        c.fill(L, 0, -DOOR_HALF, P - 1, DOOR_ROWS - 1, DOOR_HALF, Blocks.AIR);
        for (int lx = L; lx < P; lx++) {
            for (int lz = -DOOR_HALF; lz <= DOOR_HALF; lz++)
                c.set(lx, -1, lz, Math.abs(lz) == DOOR_HALF ? th.accent() : (lx & 1) == 0 ? th.floor() : th.floor2());
            c.set(lx, -1, 0, (lx & 1) == 0 ? th.light() : th.accent());
        }
        for (int lx = L + 2; lx < P; lx += 3) c.set(lx, DOOR_ROWS, 0, th.light());
        for (int sz = -1; sz <= 1; sz += 2) { // khung vòm đầu hành lang
            for (int y = 0; y <= DOOR_ROWS; y++) c.set(L, y, sz * (DOOR_HALF + 1), th.pillar());
            c.set(L, DOOR_ROWS + 1, sz * (DOOR_HALF + 1), th.cap());
        }
        for (int lz = -DOOR_HALF - 1; lz <= DOOR_HALF + 1; lz++) c.set(L, DOOR_ROWS, lz, th.cap());
        for (int lx = L + 3; lx < P - 1; lx += 3) for (int sz = -1; sz <= 1; sz += 2)
            c.set(lx, 1, sz * DOOR_HALF, Blocks.SOUL_LANTERN);
    }
}
