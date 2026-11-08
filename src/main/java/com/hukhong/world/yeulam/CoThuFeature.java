package com.hukhong.world.yeulam;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Cây cổ thụ Đại Sâm Lâm: chỉ mọc trong vùng DaiSamLam; càng vào sâu càng dày và cao.
 * Thân 2x2 (lõi 3x3 khi sâu), rễ lan, tán lá nhiều tầng, dây leo, nền rêu/podzol. Bán kính tối đa ~8 để không vượt vùng ghi.
 */
public class CoThuFeature extends Feature<NoneFeatureConfiguration> {
    public CoThuFeature(Codec<NoneFeatureConfiguration> c) { super(c); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel l = ctx.level();
        BlockPos o = ctx.origin();
        RandomSource r = ctx.random();
        double depth = DaiSamLam.depth(l.getSeed(), o.getX(), o.getZ());
        if (depth <= 0 || r.nextDouble() > 0.35 + 0.65 * depth) return false;
        BlockState ground = l.getBlockState(o.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.SNOW_BLOCK)) return false;
        return grow(l, o, r, depth);
    }

    /** Dựng 1 cây (gọi được từ lệnh / test). */
    public static boolean grow(WorldGenLevel l, BlockPos o, RandomSource r, double depth) {
        boolean dark = r.nextInt(3) == 0;
        BlockState log = (dark ? Blocks.DARK_OAK_LOG : r.nextBoolean() ? Blocks.SPRUCE_LOG : Blocks.JUNGLE_LOG).defaultBlockState();
        BlockState leaf = (dark ? Blocks.DARK_OAK_LEAVES : r.nextBoolean() ? Blocks.SPRUCE_LEAVES : Blocks.JUNGLE_LEAVES)
                .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        // 2026-11-08: cây to dần theo vùng — rìa ngoài 16-22, vùng giữa 26-34 (thân 3x3), hạch tâm 38-50 (thân 4x4)
        int zone = com.hukhong.creature.YeuThuAge.zone(depth);
        int h = zone == 0 ? 16 + r.nextInt(7) : zone == 1 ? 26 + r.nextInt(9) : 38 + r.nextInt(13);
        boolean thick = zone >= 1;
        int t0 = thick ? -1 : 0, t1 = zone == 2 ? 2 : 1;
        // nền rêu / podzol
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            if (dx * dx + dz * dz > 25 || r.nextInt(3) == 0) continue;
            BlockPos g = o.offset(dx, -1, dz);
            if (l.getBlockState(g).is(BlockTags.DIRT) || l.getBlockState(g).is(Blocks.GRASS_BLOCK))
                l.setBlock(g, (r.nextBoolean() ? Blocks.PODZOL : Blocks.MOSS_BLOCK).defaultBlockState(), 2);
        }
        // thân
        for (int y = -1; y < h; y++) for (int dx = t0; dx <= t1; dx++) for (int dz = t0; dz <= t1; dz++) {
            if (thick && y > h * 0.6 && (dx == t0 || dz == t0) && (dx == t1 || dz == t1)) continue;
            set(l, o.offset(dx, y, dz), log);
        }
        // rễ lan 4 hướng
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int len = 3 + r.nextInt(3) + zone * 2;
            BlockState side = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
            for (int i = 2; i < 2 + len; i++) {
                int y = i < 3 ? 1 : 0;
                BlockPos p = o.offset(d.getStepX() * i + (d.getStepX() == 0 ? (i % 2) : 0), y, d.getStepZ() * i + (d.getStepZ() == 0 ? (i % 2) : 0));
                set(l, p, side);
                set(l, p.below(), side);
            }
        }
        // cành + tán nhiều tầng
        int layers = 3 + zone * 2;
        for (int k = 0; k < layers; k++) {
            int cy = h - 2 - k * 5;
            if (cy < 8) break;
            int rad = Math.min(5 + zone * 2, 4 + k + zone);
            canopy(l, o.offset(0, cy, 0), rad, leaf, r);
            if (k > 0) for (Direction d : Direction.Plane.HORIZONTAL) {
                if (r.nextInt(3) == 0) continue;
                BlockState side = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
                for (int i = 1; i <= rad - 1; i++) set(l, o.offset(d.getStepX() * i, cy - 1 + i / 3, d.getStepZ() * i), side);
            }
        }
        canopy(l, o.offset(0, h + 1, 0), 3 + zone, leaf, r);
        // dây leo
        for (int i = 0; i < 18; i++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            int rad = 4 + r.nextInt(3);
            BlockPos s = o.offset(d.getStepX() * rad + r.nextInt(3) - 1, h - 3 - r.nextInt(8), d.getStepZ() * rad + r.nextInt(3) - 1);
            if (!l.getBlockState(s).is(BlockTags.LEAVES)) continue;
            BlockState vine = Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(d.getOpposite()), true);
            for (int y = 1; y < 5 + r.nextInt(6); y++) {
                BlockPos v = s.below(y);
                if (!l.isEmptyBlock(v)) break;
                l.setBlock(v, vine, 2);
            }
        }
        return true;
    }

    private static void canopy(WorldGenLevel l, BlockPos c, int rad, BlockState leaf, RandomSource r) {
        for (int dy = -2; dy <= 2; dy++) {
            int rr = rad - Math.abs(dy) - (dy > 0 ? 1 : 0);
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > rr + 0.3 || (d > rr - 1 && r.nextInt(4) == 0)) continue;
                BlockPos p = c.offset(dx, dy, dz);
                if (l.isEmptyBlock(p) || l.getBlockState(p).is(BlockTags.REPLACEABLE_BY_TREES)) l.setBlock(p, leaf, 2);
            }
        }
    }

    private static void set(WorldGenLevel l, BlockPos p, BlockState s) {
        BlockState cur = l.getBlockState(p);
        if (cur.isAir() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.REPLACEABLE_BY_TREES) || cur.is(BlockTags.DIRT) || cur.is(Blocks.GRASS_BLOCK)
                || cur.is(Blocks.SNOW) || cur.canBeReplaced()) l.setBlock(p, s, 2);
    }
}
