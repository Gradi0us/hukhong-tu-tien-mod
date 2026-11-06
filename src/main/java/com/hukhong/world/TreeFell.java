package com.hukhong.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quật đổ cây (2026-11-06): quái cây khổng lồ / boss đi qua, trụ Pháp Thổ, chiêu kiến tạo địa hình làm đổ cả cây.
 * Chỉ đổ cây tự nhiên: cụm gỗ có lá tự mọc (lá không "persistent") - nhà gỗ người chơi xây không bị phá.
 */
public final class TreeFell {
    private TreeFell() {}

    public static final int MAX_TREE_BLOCKS = 400, MAX_SCAN = 6000;

    private static boolean log(BlockState s) { return s.is(BlockTags.LOGS); }

    private static boolean wildLeaf(BlockState s) {
        return s.is(BlockTags.LEAVES) && !(s.hasProperty(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.PERSISTENT));
    }

    /** Đổ mọi cây có gỗ chạm vào box. Trả số cây đã đổ. */
    public static int fellIn(ServerLevel lv, AABB box, int maxTrees) {
        int x0 = (int) Math.floor(box.minX), y0 = (int) Math.floor(box.minY), z0 = (int) Math.floor(box.minZ);
        int x1 = (int) Math.floor(box.maxX), y1 = (int) Math.floor(box.maxY), z1 = (int) Math.floor(box.maxZ);
        Set<BlockPos> seen = new HashSet<>();
        int felled = 0, scanned = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1 && felled < maxTrees; y++)
            for (int x = x0; x <= x1 && felled < maxTrees; x++)
                for (int z = z0; z <= z1 && felled < maxTrees; z++) {
                    if (++scanned > MAX_SCAN) return felled;
                    m.set(x, y, z);
                    if (seen.contains(m) || !lv.isLoaded(m) || !log(lv.getBlockState(m))) continue;
                    List<BlockPos> tree = collect(lv, m.immutable(), seen);
                    if (tree != null) { topple(lv, tree, box.getCenter()); felled++; }
                }
        return felled;
    }

    public static int fellRadius(ServerLevel lv, Vec3 c, double r, double h, int maxTrees) {
        return fellIn(lv, new AABB(c.x - r, c.y - 1, c.z - r, c.x + r, c.y + h, c.z + r), maxTrees);
    }

    /** Gom gỗ + lá tự mọc liền nhau; null nếu không phải cây tự nhiên (không có lá tự mọc). */
    static List<BlockPos> collect(ServerLevel lv, BlockPos start, Set<BlockPos> seen) {
        List<BlockPos> out = new ArrayList<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        seen.add(start);
        boolean leaves = false;
        while (!q.isEmpty() && out.size() < MAX_TREE_BLOCKS) {
            BlockPos p = q.poll();
            BlockState s = lv.getBlockState(p);
            boolean isLog = log(s);
            if (!isLog && !wildLeaf(s)) continue;
            if (!isLog) leaves = true;
            out.add(p);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos n = p.offset(dx, dy, dz);
                if (Math.abs(n.getX() - start.getX()) > 10 || Math.abs(n.getZ() - start.getZ()) > 10 || n.getY() < start.getY() - 2) continue;
                if (!isLog && !log(lv.getBlockState(n)) && !wildLeaf(lv.getBlockState(n))) continue;
                if (seen.add(n)) q.add(n);
            }
        }
        return leaves ? out : null;
    }

    /** Đổ cây: vỡ khối kèm bụi gỗ / lá, rơi lại 1/3 số gỗ, tiếng cây đổ. */
    static void topple(ServerLevel lv, List<BlockPos> tree, Vec3 from) {
        int i = 0;
        BlockPos base = tree.get(0);
        for (BlockPos p : tree) {
            BlockState s = lv.getBlockState(p);
            boolean isLog = log(s);
            if (i++ % 3 == 0 || !isLog)
                lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, isLog ? 6 : 2, 0.3, 0.3, 0.3, 0.1);
            if (isLog && i % 3 == 0) net.minecraft.world.level.block.Block.dropResources(s, lv, p);
            lv.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        lv.playSound(null, base, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.4f, 0.5f);
        lv.playSound(null, base, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
    }
}
