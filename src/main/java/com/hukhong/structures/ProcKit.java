package com.hukhong.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Bo dung hinh thu tuc dung chung cho CityBuilder / SectBuilder (tuong, thap, nha mai doc, mai hip, tau, hu hong). */
public final class ProcKit {
    private ProcKit() {}

    public static final int FLAGS = 18;

    // ---- primitives ----
    /** Hop cat (minX,minZ,maxX,maxZ) cho che do sua chua: khac null thi set() bo qua o ngoai hop. Chi tren server thread, luon try/finally. */
    public static volatile int[] clip;

    public static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        int[] c = clip;
        if (c != null && (x < c[0] || x > c[2] || z < c[1] || z > c[3])) return;
        l.setBlock(new BlockPos(x, y, z), s, FLAGS);
    }

    public static void set(ServerLevel l, int x, int y, int z, Block b) { set(l, x, y, z, b.defaultBlockState()); }

    public static void air(ServerLevel l, int x, int y, int z) { set(l, x, y, z, Blocks.AIR); }

    public static void fill(ServerLevel l, int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(l, x, y, z, b);
    }

    public static int hash(int x, int y, int z, long salt) {
        return (int) (((BlockPos.asLong(x, y, z) ^ salt) * 0x9E3779B97F4A7C15L) >>> 56) & 0xFF;
    }

    public static void stair(ServerLevel l, int x, int y, int z, Block b, Direction facing) {
        set(l, x, y, z, b.defaultBlockState().setValue(StairBlock.FACING, facing));
    }

    public static void door(ServerLevel l, int x, int y, int z, Block door, Direction facing) {
        set(l, x, y, z, door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(l, x, y + 1, z, door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    public static void hangLantern(ServerLevel l, int x, int y, int z) {
        set(l, x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    // ---- vat lieu nha ----
    public record Mat(Block wall, Block frame, Block floor, Block roofStair, Block roofFull) {}

    public static Mat mat(int k) {
        return switch (Math.floorMod(k, 5)) {
            case 0 -> new Mat(Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS);
            case 1 -> new Mat(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_LOG, Blocks.COBBLESTONE, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS);
            case 2 -> new Mat(Blocks.STONE_BRICKS, Blocks.OAK_LOG, Blocks.STONE_BRICKS, Blocks.BRICK_STAIRS, Blocks.BRICKS);
            case 3 -> new Mat(Blocks.BIRCH_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.OAK_PLANKS, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS);
            default -> new Mat(Blocks.WHITE_TERRACOTTA, Blocks.DARK_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.COBBLESTONE_STAIRS, Blocks.COBBLESTONE);
        };
    }

    /**
     * Nha mai doc (mai doc theo truc Z, hong doc theo X). w,d = nua rong/nua sau (d &gt;= 2), floors 1..2.
     * out = huong cua chinh (cua o giua canh ngoai theo huong do).
     */
    public static void house(ServerLevel l, int hx, int hz, int y0, int w, int d, int floors, Mat m, Direction out) {
        int H = floors == 2 ? 8 : 4;
        for (int dx = -w; dx <= w; dx++) for (int dz = -d; dz <= d; dz++) {
            int x = hx + dx, z = hz + dz;
            boolean ex = Math.abs(dx) == w, ez = Math.abs(dz) == d;
            boolean edge = ex || ez, corner = ex && ez;
            set(l, x, y0, z, m.floor());
            for (int h = 1; h <= H; h++) {
                if (corner) set(l, x, y0 + h, z, m.frame());
                else if (edge) set(l, x, y0 + h, z, m.wall());
            }
            if (!edge) {
                set(l, x, y0 + H, z, m.floor()); // tran
                if (floors == 2) set(l, x, y0 + 4, z, m.floor()); // san tang 2
            }
            if (edge && !corner) { // cua so
                boolean mid = (ex && dz == 0) || (ez && dx == 0);
                if (mid) {
                    set(l, x, y0 + 2, z, Blocks.GLASS_PANE);
                    if (floors == 2) set(l, x, y0 + 6, z, Blocks.GLASS_PANE);
                }
            }
        }
        // cua chinh
        int dxr = hx + out.getStepX() * w, dzr = hz + out.getStepZ() * d;
        air(l, dxr, y0 + 1, dzr);
        air(l, dxr, y0 + 2, dzr);
        door(l, dxr, y0 + 1, dzr, Blocks.OAK_DOOR, out);
        // mai doc
        for (int i = 0; i <= w; i++) {
            int yy = y0 + H + 1 + i;
            for (int z = hz - d - 1; z <= hz + d + 1; z++) {
                stair(l, hx - (w + 1) + i, yy, z, m.roofStair(), Direction.EAST);
                stair(l, hx + (w + 1) - i, yy, z, m.roofStair(), Direction.WEST);
            }
            for (int dx = -(w - i); dx <= w - i; dx++) { // dau hoi
                set(l, hx + dx, yy, hz - d, m.wall());
                set(l, hx + dx, yy, hz + d, m.wall());
            }
        }
        for (int z = hz - d - 1; z <= hz + d + 1; z++) set(l, hx, y0 + H + 2 + w, z, m.roofFull());
        // noi that
        hangLantern(l, hx, y0 + H - 1, hz);
        if (floors == 2) hangLantern(l, hx, y0 + 3, hz);
        set(l, hx + w - 1, y0 + 1, hz + d - 1, Blocks.CRAFTING_TABLE);
        set(l, hx - w + 1, y0 + 1, hz + d - 1, Blocks.FURNACE);
        if (floors == 2) {
            int lx = hx - w + 1, lz = hz - d + 1;
            air(l, lx, y0 + 4, lz);
            for (int h = 1; h <= 7; h++) set(l, lx, y0 + h, lz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        }
    }

    /** Mai hip hinh chu nhat (hx,hz = nua kich thuoc tuong, mai ho 1 o). y = tang mai dau tien. */
    public static void hipRoof(ServerLevel l, int cx, int cz, int y, int hx, int hz, Block stairB, Block full) {
        for (int i = 0; ; i++) {
            int ax = hx + 1 - i, az = hz + 1 - i;
            if (ax < 0 || az < 0) break;
            int yy = y + i;
            for (int dx = -ax; dx <= ax; dx++) for (int dz = -az; dz <= az; dz++) {
                boolean onX = Math.abs(dx) == ax, onZ = Math.abs(dz) == az;
                if (!onX && !onZ) continue;
                if (ax == 0 || az == 0) { set(l, cx + dx, yy, cz + dz, full); continue; }
                Direction f;
                if (onX && !(onZ && Math.abs(dz) > Math.abs(dx))) f = dx > 0 ? Direction.WEST : Direction.EAST;
                else f = dz > 0 ? Direction.NORTH : Direction.SOUTH;
                stair(l, cx + dx, yy, cz + dz, stairB, f);
            }
        }
    }

    /**
     * Thap vuong ban kinh r (tuong 2r+1), cao h. Co cua o huong doorOut, thang day ben trong, san tren dinh.
     * roof=true: mai chop; false: lan can co cua (crenellation).
     */
    public static void tower(ServerLevel l, int tx, int tz, int y0, int r, int h, Block wall, Block roofStair, Block roofFull,
                             Direction doorOut, boolean roof) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int x = tx + dx, z = tz + dz;
            boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
            set(l, x, y0, z, wall);
            if (edge) {
                int top = roof ? h + 2 : h;
                for (int k = 1; k <= top; k++) set(l, x, y0 + k, z, wall);
                if (!roof && ((dx + dz) & 1) == 0) set(l, x, y0 + h + 1, z, wall);
                // khe ten
                if (dx == 0 || dz == 0) {
                    set(l, x, y0 + h / 2, z, Blocks.IRON_BARS);
                    if (roof) air(l, x, y0 + h + 1, z);
                }
            } else {
                for (int k = 1; k < h; k++) air(l, x, y0 + k, z);
                set(l, x, y0 + h, z, Blocks.SPRUCE_PLANKS); // san tren
            }
        }
        int ddx = tx + doorOut.getStepX() * r, ddz = tz + doorOut.getStepZ() * r;
        air(l, ddx, y0 + 1, ddz);
        air(l, ddx, y0 + 2, ddz);
        // thang
        int lx = tx - r + 1, lz = tz - r + 1;
        if (doorOut == Direction.WEST) lz = tz + r - 1;
        for (int k = 1; k <= h; k++) set(l, lx, y0 + k, lz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        air(l, lx, y0 + h, lz);
        set(l, lx, y0 + h, lz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        set(l, tx + (r - 1), y0 + h + 1, tz + (r - 1), Blocks.LANTERN);
        if (roof) hipRoof(l, tx, tz, y0 + h + 3, r, r, roofStair, roofFull);
    }

    /** Tau buom (nuoc o mat y; thuyen nhu len mat nuoc). len le. alongX: truc tau theo X. */
    public static void ship(ServerLevel l, int cx, int cz, int y, int len, boolean alongX) {
        int half = len / 2;
        for (int t = -half; t <= half; t++) for (int a = -1; a <= 1; a++) {
            if (Math.abs(t) == half && a != 0) continue;
            int x = alongX ? cx + t : cx + a, z = alongX ? cz + a : cz + t;
            boolean bow = Math.abs(t) == half;
            if (a == 0) set(l, x, y - 1, z, Blocks.DARK_OAK_PLANKS); // song tau
            set(l, x, y, z, Blocks.SPRUCE_PLANKS); // san tau
            if (Math.abs(a) == 1 || bow) {
                if (bow) {
                    Direction f = alongX ? (t > 0 ? Direction.WEST : Direction.EAST) : (t > 0 ? Direction.NORTH : Direction.SOUTH);
                    stair(l, x, y + 1, z, Blocks.SPRUCE_STAIRS, f.getOpposite());
                } else set(l, x, y + 1, z, Blocks.DARK_OAK_PLANKS);
            }
        }
        for (int k = 1; k <= 7; k++) set(l, cx, y + k, cz, Blocks.OAK_FENCE);
        for (int a = -1; a <= 1; a++) for (int k = 3; k <= 6; k++) {
            int x = alongX ? cx : cx + a, z = alongX ? cz + a : cz;
            if (a != 0) set(l, x, y + k, z, Blocks.WHITE_WOOL);
        }
        set(l, cx, y + 8, cz, Blocks.RED_WOOL);
        set(l, alongX ? cx + 1 : cx, y + 2, alongX ? cz : cz + 1, Blocks.BARREL);
    }

    // ---- hu hong ----
    /** Pha huy / rong reu / mang nhen tren hop (±half) tu y0+1 den y0+height. Tat dinh theo vi tri. */
    public static void decay(ServerLevel l, int cx, int cz, int y0, int half, int height, long salt) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int x = cx + dx, z = cz + dz;
            for (int h = 0; h <= height; h++) {
                int y = y0 + h;
                BlockState s = l.getBlockState(mp.set(x, y, z));
                if (s.isAir() || s.is(Blocks.WATER) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT)) continue;
                int hs = hash(x, y, z, salt);
                int lift = h * 4; // cang cao cang de sup
                BlockState to = null;
                if (s.is(BlockTags.STAIRS) || s.is(BlockTags.SLABS) || s.is(BlockTags.FENCES) || s.is(BlockTags.DOORS)
                        || s.is(BlockTags.PLANKS) || s.is(Blocks.GLASS_PANE) || s.is(Blocks.LANTERN) || s.is(Blocks.LADDER)
                        || s.is(Blocks.WHITE_WOOL) || s.is(Blocks.RED_WOOL) || s.is(Blocks.IRON_BARS) || s.is(BlockTags.WOOL)) {
                    if (hs < 70 + lift) to = Blocks.AIR.defaultBlockState();
                } else if (s.is(Blocks.STONE_BRICKS)) {
                    if (hs < 20 + lift / 2) to = Blocks.AIR.defaultBlockState();
                    else if (hs < 80) to = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                    else if (hs < 130) to = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                } else if (s.is(BlockTags.LOGS)) {
                    if (hs < 40 + lift) to = Blocks.AIR.defaultBlockState();
                } else if (h == 0 && (s.is(Blocks.COBBLESTONE) || s.is(Blocks.POLISHED_ANDESITE) || s.is(Blocks.SMOOTH_STONE))) {
                    if (hs < 70) to = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                    else if (hs < 110) to = Blocks.COARSE_DIRT.defaultBlockState();
                } else if (s.is(Blocks.COBBLESTONE) && hs < 60) to = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                if (to != null) l.setBlock(mp, to, FLAGS);
            }
        }
        // rong reu, mang nhen, co dai (lan 2: chi dat vao o khong khi)
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int x = cx + dx, z = cz + dz;
            for (int h = 0; h <= height; h++) {
                int y = y0 + h;
                mp.set(x, y, z);
                if (!l.getBlockState(mp).isAir()) continue;
                int hs = hash(x, y, z, salt ^ 0x5151L);
                BlockState below = l.getBlockState(mp.set(x, y - 1, z));
                mp.set(x, y, z);
                if (h == 0 && hs < 36 && (below.is(Blocks.GRASS_BLOCK))) { l.setBlock(mp, Blocks.SHORT_GRASS.defaultBlockState(), FLAGS); continue; }
                if (h == 0 && hs > 245 && below.isSolid()) { l.setBlock(mp, Blocks.COBBLESTONE.defaultBlockState(), FLAGS); continue; }
                if (hs < 22) {
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockState nb = l.getBlockState(mp.relative(dir));
                        if (nb.is(Blocks.STONE_BRICKS) || nb.is(Blocks.MOSSY_STONE_BRICKS) || nb.is(Blocks.CRACKED_STONE_BRICKS)
                                || nb.is(Blocks.OAK_PLANKS) || nb.is(Blocks.COBBLESTONE) || nb.is(Blocks.MOSSY_COBBLESTONE)) {
                            l.setBlock(mp, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(dir), true), FLAGS);
                            break;
                        }
                    }
                } else if (hs > 238 && h >= 1 && h <= 3 && below.isSolid()) {
                    l.setBlock(mp, Blocks.COBWEB.defaultBlockState(), FLAGS);
                }
            }
        }
    }
}
