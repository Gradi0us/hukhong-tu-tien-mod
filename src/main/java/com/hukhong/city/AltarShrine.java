package com.hukhong.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.hukhong.structures.ProcKit.air;
import static com.hukhong.structures.ProcKit.set;

/**
 * Dien dan Ấn Hồn Đài: dai bat giac (gach den / thach anh, tia ron froglight phat sang), ho nuoc co den bao quanh voi 4 cau,
 * 4 tru da + banner + lua hon, vong lo lung tren cao. Dung chung cho thanh thu tuc va thanh import
 * (import: chi dung khi vung phang, khong co cong trinh; thu ban kinh lon -> nho). Chi chay tren server thread.
 */
public final class AltarShrine {
    private AltarShrine() {}

    /** Thanh thu tuc: R=5 -> vet T=7. */
    public static final int PROC_R = 6, PROC_T = PROC_R + 3;
    /** Thanh nui / cang (it cho): ban kinh nho, dien dan phang. */
    public static final int SMALL_R = 5, SMALL_T = SMALL_R + 2;

    public static int procR(String cat) { return cat.equals("city_mountain") || cat.equals("port") ? SMALL_R : PROC_R; }
    public static int procT(String cat) { return procR(cat) >= 6 ? procR(cat) + 3 : procR(cat) + 2; }

    /** Khoang cach tam quay hang toi altar, tinh tu vet T. */
    public static int stallRadius(int t) { return t + 3; }

    /** Khoang cach kieu bat giac. */
    static int oct(int dx, int dz) {
        int a = Math.abs(dx), b = Math.abs(dz);
        return Math.max(Math.max(a, b), (int) Math.round((a + b) * 0.72));
    }

    private static boolean natural(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    /** Vung ban kinh r quanh (cx,cz) o mat nen fy khong co cong trinh nhan tao (chi cay / co / khong khi). */
    private static boolean clear(ServerLevel l, int cx, int cz, int fy, int r) {
        int m = r >= 6 ? 3 : 2;
        if (r >= 6 && !toriiClear(l, cx, cz, fy, r + 3)) return false;
        for (int dx = -r - m; dx <= r + m; dx++) for (int dz = -r - m; dz <= r + m; dz++) {
            int o = oct(dx, dz);
            if (o > r + m) continue;
            BlockPos base = new BlockPos(cx + dx, fy, cz + dz);
            if (!l.hasChunkAt(base)) return false;
            if (!l.getFluidState(base).isEmpty() || !l.getFluidState(base.above()).isEmpty()) return false;
            int hmax = o <= r + m - 1 ? 13 : 3;
            for (int h = 1; h <= hmax; h++) {
                if (dx == 0 && dz == 0) continue;
                if (!natural(l.getBlockState(base.above(h)))) return false;
            }
        }
        return true;
    }

    /**
     * Dung dien dan quanh altar (altar = o dat Ấn Hồn Đài, mat nen = altar.y-1). maxR gioi han ban kinh dai.
     * force=true: bo kiem tra cong trinh (thanh thu tuc). Tra ban kinh vet T (0 neu khong du cho: chi lat 3x3).
     */
    public static int build(ServerLevel l, BlockPos altar, int maxR, boolean force) {
        int cx = altar.getX(), cz = altar.getZ(), fy = altar.getY() - 1;
        int R = 0;
        int top = Math.min(maxR, 9);
        for (int r = top; r >= 3; r--) if (force ? r == top : clear(l, cx, cz, fy, r)) { R = r; break; }
        if (R >= 6) return buildGrand(l, cx, cz, fy, R);
        if (R == 0) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                set(l, cx + dx, fy, cz + dz, Blocks.CHISELED_STONE_BRICKS);
                for (int h = 1; h <= 3; h++) if (!(dx == 0 && dz == 0)) air(l, cx + dx, fy + h, cz + dz);
            }
            return 0;
        }
        final int T = R + 2;
        boolean moat = R >= 5;
        int k = (int) Math.floor(R * 0.69);

        // 1) don khong gian + nen + hoa van
        for (int dx = -T; dx <= T; dx++) for (int dz = -T; dz <= T; dz++) {
            int o = oct(dx, dz);
            if (o > T) continue;
            int x = cx + dx, z = cz + dz;
            if (o <= T - 1) for (int h = 1; h <= 11; h++) if (!(dx == 0 && dz == 0)) air(l, x, fy + h, z);
            for (int d = 1; d <= 3; d++) { // nen dac
                BlockPos bp = new BlockPos(x, fy - d, z);
                BlockState s = l.getBlockState(bp);
                if (s.blocksMotion() && l.getFluidState(bp).isEmpty()) break;
                l.setBlock(bp, Blocks.STONE.defaultBlockState(), 18);
            }
            set(l, x, fy, z, floor(dx, dz, o, R, T));
            if (moat && o == R + 1 && Math.abs(dx) > 1 && Math.abs(dz) > 1) {
                set(l, x, fy - 1, z, Blocks.SEA_LANTERN);
                set(l, x, fy, z, Blocks.WATER);
            }
        }
        set(l, cx, fy, cz, Blocks.CHISELED_POLISHED_BLACKSTONE);

        // 2) lan can + den linh canh 4 loi len
        if (R >= 4) for (Direction d : Direction.Plane.HORIZONTAL) {
            Direction s = d.getClockWise();
            for (int w : new int[]{-2, 2}) {
                int x = cx + d.getStepX() * R + s.getStepX() * w, z = cz + d.getStepZ() * R + s.getStepZ() * w;
                set(l, x, fy + 1, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
                set(l, x, fy + 2, z, Blocks.SOUL_LANTERN);
            }
            for (int w : new int[]{-3, 3}) {
                int x = cx + d.getStepX() * R + s.getStepX() * w, z = cz + d.getStepZ() * R + s.getStepZ() * w;
                if (oct(x - cx, z - cz) > R) continue;
                set(l, x, fy + 1, z, Blocks.SOUL_CAMPFIRE);
            }
        }

        // 3) bon tru da o goc + banner + lua hon
        if (R >= 4) for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int px = cx + sx * k, pz = cz + sz * k;
            set(l, px, fy, pz, Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(l, px, fy + 1, pz, Blocks.POLISHED_BLACKSTONE_BRICKS);
            for (int h = 2; h <= 5; h++) set(l, px, fy + h, pz, Blocks.QUARTZ_PILLAR);
            set(l, px, fy + 6, pz, Blocks.CHISELED_QUARTZ_BLOCK);
            set(l, px, fy + 7, pz, Blocks.SOUL_CAMPFIRE);
            Block banner = (sx * sz > 0) ? Blocks.PURPLE_WALL_BANNER : Blocks.RED_WALL_BANNER;
            Direction fx = sx > 0 ? Direction.EAST : Direction.WEST, fz = sz > 0 ? Direction.SOUTH : Direction.NORTH;
            for (Direction f : new Direction[]{fx, fz}) {
                BlockPos bp = new BlockPos(px + f.getStepX(), fy + 4, pz + f.getStepZ());
                if (l.getBlockState(bp).isAir()) l.setBlock(bp, banner.defaultBlockState().setValue(WallBannerBlock.FACING, f), 18);
            }
        }

        // 4) vong lo lung tren cao + vong den noi gan altar
        if (R >= 4) for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            int o = oct(dx, dz), x = cx + dx, z = cz + dz;
            if (o == R) {
                set(l, x, fy + 10, z, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                if (dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz))
                    set(l, x, fy + 9, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            if (o == 3 && R >= 5 && ((dx + dz) & 1) == 0) set(l, x, fy + 8, z, Blocks.END_ROD);
        }
        return T;
    }

    private static boolean toriiClear(ServerLevel l, int cx, int cz, int fy, int T) {
        int z = cz + T + 4;
        for (int dx = -8; dx <= 8; dx++) {
            if (!l.hasChunkAt(new BlockPos(cx + dx, fy, z))) return false;
            for (int h = 1; h <= 10; h++) if (!natural(l.getBlockState(new BlockPos(cx + dx, fy + h, z)))) return false;
        }
        return true;
    }

    private static Direction inward(int dx, int dz) {
        return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
    }

    /**
     * Dien dan lon (R>=6, vet T=R+3): ban tren (altar) -> bac da vao -> vanh giua (kinh mau lam phat sang) -> ho nuoc co 4 cau
     * -> bac len nen thanh. 8 tru ngoc kem banner + den treo, lo huong, vong lo lung, cong torii truc nam.
     */
    private static int buildGrand(ServerLevel l, int cx, int cz, int fy, int R) {
        final int T = R + 3;
        for (int dx = -T; dx <= T; dx++) for (int dz = -T; dz <= T; dz++) {
            int o = oct(dx, dz);
            if (o > T) continue;
            int x = cx + dx, z = cz + dz, a = Math.abs(dx), b = Math.abs(dz);
            for (int d = 1; d <= 4; d++) { // nen dac toi fy-4
                BlockPos bp = new BlockPos(x, fy - d, z);
                BlockState s = l.getBlockState(bp);
                if (!s.blocksMotion() || !l.getFluidState(bp).isEmpty()) l.setBlock(bp, Blocks.STONE.defaultBlockState(), 18);
            }
            if (o <= T - 1) for (int h = 1; h <= 13; h++) if (!(dx == 0 && dz == 0)) air(l, x, fy + h, z);
            boolean path = a <= 1 || b <= 1;
            Direction in = inward(dx, dz), out = in.getOpposite();
            boolean spoke = a == 0 || b == 0 || a == b;
            if (o == T) {
                set(l, x, fy, z, ((dx + dz) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES);
            } else if (o == R + 2) {
                if (path) {
                    set(l, x, fy - 1, z, Blocks.QUARTZ_BLOCK);
                    l.setBlock(new BlockPos(x, fy, z), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, out), 18);
                } else {
                    set(l, x, fy, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                    set(l, x, fy + 1, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
                    if (((a + b) & 3) == 0) { set(l, x, fy + 2, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL); set(l, x, fy + 3, z, Blocks.SOUL_LANTERN); }
                }
            } else if (o == R + 1) {
                air(l, x, fy, z);
                if (path) { set(l, x, fy - 1, z, Blocks.QUARTZ_BLOCK); }
                else { air(l, x, fy - 1, z); set(l, x, fy - 2, z, Blocks.WATER); set(l, x, fy - 3, z, Blocks.SEA_LANTERN); }
            } else if (o >= R - 1) {
                air(l, x, fy, z);
                if (o == R) set(l, x, fy - 1, z, ((dx + dz) & 1) == 0 ? Blocks.GOLD_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS);
                else if (spoke) set(l, x, fy - 1, z, Blocks.CHISELED_QUARTZ_BLOCK);
                else if (((dx + dz) & 1) == 0) { set(l, x, fy - 1, z, Blocks.TINTED_GLASS); set(l, x, fy - 2, z, Blocks.SEA_LANTERN); }
                else set(l, x, fy - 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            } else if (o == R - 2) {
                set(l, x, fy - 1, z, Blocks.QUARTZ_BLOCK);
                l.setBlock(new BlockPos(x, fy, z), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, in), 18);
            } else {
                Block bl;
                if (o == 0) bl = Blocks.CHISELED_POLISHED_BLACKSTONE;
                else if (o == 1) bl = Blocks.CRYING_OBSIDIAN;
                else if (o == 2) bl = Blocks.OCHRE_FROGLIGHT;
                else if (o == R - 3) bl = Blocks.GOLD_BLOCK;
                else if (o == R - 5 && o > 2) bl = Blocks.VERDANT_FROGLIGHT;
                else bl = ((dx + dz) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE : Blocks.SMOOTH_QUARTZ;
                set(l, x, fy, z, bl);
            }
        }
        // 8 tru ngoc tren vanh giua + banner + den treo
        Block[] banners = {Blocks.RED_WALL_BANNER, Blocks.PURPLE_WALL_BANNER, Blocks.BLUE_WALL_BANNER, Blocks.WHITE_WALL_BANNER};
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(22.5 + 45 * i);
            int px = (int) Math.round(Math.cos(ang) * (R - 0.5)), pz = (int) Math.round(Math.sin(ang) * (R - 0.5));
            int x = cx + px, z = cz + pz;
            set(l, x, fy, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            for (int h = 1; h <= 7; h++) set(l, x, fy + h, z, Blocks.QUARTZ_PILLAR);
            set(l, x, fy + 8, z, Blocks.CHISELED_QUARTZ_BLOCK);
            set(l, x, fy + 9, z, Blocks.GOLD_BLOCK);
            set(l, x, fy + 10, z, Blocks.SOUL_LANTERN);
            Direction in = inward(px, pz);
            int ax = x + in.getStepX(), az = z + in.getStepZ();
            set(l, ax, fy + 8, az, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
            set(l, ax, fy + 7, az, Blocks.CHAIN);
            set(l, ax, fy + 6, az, Blocks.CHAIN);
            set(l, ax, fy + 5, az, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            for (Direction f : new Direction[]{px > 0 ? Direction.EAST : Direction.WEST, pz > 0 ? Direction.SOUTH : Direction.NORTH}) {
                BlockPos bp = new BlockPos(x + f.getStepX(), fy + 4, z + f.getStepZ());
                if (l.getBlockState(bp).isAir())
                    l.setBlock(bp, banners[i & 3].defaultBlockState().setValue(WallBannerBlock.FACING, f), 18);
            }
        }
        // lo huong tren ban tren + vong den + vong lo lung noi
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            set(l, cx + sx * 2, fy, cz + sz * 2, Blocks.GOLD_BLOCK);
            set(l, cx + sx * 2, fy + 1, cz + sz * 2, Blocks.SOUL_CAMPFIRE);
        }
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            int o = oct(dx, dz), x = cx + dx, z = cz + dz;
            if (o == R - 1) {
                set(l, x, fy + 12, z, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                if (dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz))
                    set(l, x, fy + 11, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            if (o == 4 && ((dx + dz) & 1) == 0) set(l, x, fy + 8, z, Blocks.END_ROD);
        }
        for (int[] c : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) set(l, cx + c[0], fy + 6, cz + c[1], Blocks.AMETHYST_BLOCK);
        // cong torii truc nam
        if (toriiClear(l, cx, cz, fy, T)) {
            int z = cz + T + 4;
            for (int sx = -1; sx <= 1; sx += 2) for (int h = 0; h <= 7; h++) set(l, cx + sx * 5, fy + h, z, Blocks.STRIPPED_CRIMSON_STEM);
            for (int dx = -7; dx <= 7; dx++) set(l, cx + dx, fy + 8, z, Blocks.CRIMSON_PLANKS);
            for (int dx = -8; dx <= 8; dx++) set(l, cx + dx, fy + 9, z, Blocks.CRIMSON_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            for (int dx = -4; dx <= 4; dx++) set(l, cx + dx, fy + 6, z, Blocks.CRIMSON_PLANKS);
            set(l, cx, fy + 7, z, Blocks.GOLD_BLOCK);
            for (int sx = -1; sx <= 1; sx += 2) set(l, cx + sx * 2, fy + 5, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            for (int sx = -1; sx <= 1; sx += 2) { set(l, cx + sx * 6, fy + 8, z, Blocks.CRIMSON_PLANKS); set(l, cx + sx * 5, fy + 10, z, Blocks.SOUL_LANTERN); }
        }
        return T;
    }

    private static BlockState floor(int dx, int dz, int o, int R, int T) {
        int a = Math.abs(dx), b = Math.abs(dz);
        boolean spoke = a == 0 || b == 0 || a == b;
        Block bl;
        if (o >= T) bl = ((dx + dz) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES; // vien ngoai
        else if (o > R) bl = Blocks.POLISHED_BLACKSTONE_BRICKS;                                              // day ho / mat cau
        else if (o == R) bl = Blocks.POLISHED_BLACKSTONE_BRICKS;                                             // vien dai
        else if (o == R - 1) bl = Blocks.VERDANT_FROGLIGHT;                                                  // vong ron phat sang
        else if (o <= 1) bl = Blocks.CRYING_OBSIDIAN;
        else if (o == R - 2) bl = (a == 0 || b == 0) ? Blocks.OCHRE_FROGLIGHT : Blocks.SMOOTH_QUARTZ;
        else if (spoke) bl = Blocks.OCHRE_FROGLIGHT;                                                         // tia ron phat sang
        else bl = ((dx + dz) & 1) == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_QUARTZ;
        return bl.defaultBlockState();
    }
}
