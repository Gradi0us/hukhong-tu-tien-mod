package com.hukhong.voidblock;

import com.hukhong.events.CorruptionData;
import com.hukhong.world.SafeZones;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Xâm thực địa hình theo chunk nhiễm (có ngân sách mỗi tick) và hoàn nguyên khi tẩy. */
public final class VoidCorruptor {
    private VoidCorruptor() {}

    private static final int RADIUS_CHUNKS = 5;
    private static final int COLUMNS_PER_RUN = 16;
    private static final int FLAGS = 2 | 16;

    /** Gọi mỗi tick Overworld (đã có người chơi). */
    public static void tick(ServerLevel ow, CorruptionData d, long t) {
        if (t % 10 == 0 && d.size() > 0) corruptStep(ow, d);
        if (t % 5 == 2 && !d.reverting().isEmpty()) revertStep(ow, d);
        if (t % 100 == 7) spires(ow, d);
    }

    /** Hư hóa một khối (null nếu không chuyển được). */
    public static BlockState corrupt(BlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.GRASS_BLOCK || b == Blocks.PODZOL || b == Blocks.MYCELIUM || b == Blocks.DIRT_PATH)
            return VoidBlocks.CO.get().defaultBlockState();
        if (b == Blocks.DIRT || b == Blocks.COARSE_DIRT || b == Blocks.ROOTED_DIRT)
            return VoidBlocks.DAT.get().defaultBlockState();
        if (b == Blocks.STONE || b == Blocks.ANDESITE || b == Blocks.GRANITE || b == Blocks.DIORITE
                || b == Blocks.COBBLESTONE || b == Blocks.DEEPSLATE || b == Blocks.TUFF)
            return VoidBlocks.DA.get().defaultBlockState();
        if (s.is(BlockTags.OVERWORLD_NATURAL_LOGS) && s.hasProperty(RotatedPillarBlock.AXIS))
            return VoidBlocks.GO.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, s.getValue(RotatedPillarBlock.AXIS));
        if (s.is(BlockTags.LEAVES)) return VoidBlocks.LA.get().defaultBlockState();
        return null;
    }

    /** Trạng thái gốc của khối Hư Không (null nếu không phải). */
    public static BlockState revert(BlockState s, boolean airAbove) {
        Block b = s.getBlock();
        if (b == VoidBlocks.CO.get()) return Blocks.GRASS_BLOCK.defaultBlockState();
        if (b == VoidBlocks.DAT.get()) return Blocks.DIRT.defaultBlockState();
        if (b == VoidBlocks.DA.get()) return Blocks.STONE.defaultBlockState();
        if (b == VoidBlocks.TINH.get()) return Blocks.AIR.defaultBlockState();
        if (b == VoidBlocks.VET_NUT.get()) return (airAbove ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
        if (b == VoidBlocks.GO.get())
            return Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, s.getValue(RotatedPillarBlock.AXIS));
        if (b == VoidBlocks.LA.get()) return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        return null;
    }

    private static boolean safe(ServerLevel ow, int cx, int cz) {
        return SafeZones.isSafe(ow, new BlockPos((cx << 4) + 8, 64, (cz << 4) + 8));
    }

    private static void corruptStep(ServerLevel ow, CorruptionData d) {
        List<ServerPlayer> ps = ow.players();
        RandomSource r = ow.random;
        for (int k = 0; k < Math.min(4, ps.size()); k++) {
            ServerPlayer p = ps.get(r.nextInt(ps.size()));
            int cx = (p.getBlockX() >> 4) + r.nextInt(RADIUS_CHUNKS * 2 + 1) - RADIUS_CHUNKS;
            int cz = (p.getBlockZ() >> 4) + r.nextInt(RADIUS_CHUNKS * 2 + 1) - RADIUS_CHUNKS;
            if (!d.isCorrupted(cx, cz) || !ow.hasChunk(cx, cz) || safe(ow, cx, cz)) continue;
            for (int i = 0; i < COLUMNS_PER_RUN; i++)
                corruptColumn(ow, (cx << 4) + r.nextInt(16), (cz << 4) + r.nextInt(16), r);
        }
    }

    private static void corruptColumn(ServerLevel ow, int x, int z, RandomSource r) {
        int top = ow.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, top, z);
        int done = 0;
        for (int y = top; y > top - 7 && y > ow.getMinBuildHeight() && done < 3; y--) {
            m.setY(y);
            BlockState s = ow.getBlockState(m);
            if (!s.getFluidState().isEmpty()) return;
            BlockState n = corrupt(s);
            if (n == null) continue;
            done++;
            boolean topGrass = n.is(VoidBlocks.CO.get()) && ow.getBlockState(m.above()).isAir();
            if (topGrass && r.nextInt(150) == 0) { ow.setBlock(m, VoidBlocks.VET_NUT.get().defaultBlockState(), FLAGS); continue; }
            ow.setBlock(m, n, FLAGS);
            if (topGrass && r.nextInt(60) == 0) {
                ow.setBlock(m.above(), VoidBlocks.TINH.get().defaultBlockState(), FLAGS);
                if (r.nextBoolean()) ow.setBlock(m.above(2), VoidBlocks.TINH.get().defaultBlockState(), FLAGS);
            }
        }
    }

    private static void revertStep(ServerLevel ow, CorruptionData d) {
        Long pick = null;
        for (long l : d.reverting()) if (ow.hasChunk((int) l, (int) (l >> 32))) { pick = l; break; }
        if (pick == null) return;
        int cx = (int) (long) pick, cz = (int) (pick >> 32);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++)
            for (int dz = 0; dz < 16; dz++) {
                int x = (cx << 4) + dx, z = (cz << 4) + dz;
                int top = ow.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                for (int y = top; y > top - 20 && y > ow.getMinBuildHeight(); y--) {
                    m.set(x, y, z);
                    BlockState s = ow.getBlockState(m);
                    if (s.isAir()) continue;
                    BlockState n = revert(s, ow.getBlockState(m.above()).isAir());
                    if (n != null) ow.setBlock(m, n, FLAGS);
                }
            }
        d.finishRevert(pick);
    }

    /** Tháp Hư Không Tinh ở Tâm Hư Hóa khi có người chơi trong 128 block. */
    private static void spires(ServerLevel ow, CorruptionData d) {
        for (BlockPos h : new ArrayList<>(d.hearts())) {
            if (!ow.hasChunk(h.getX() >> 4, h.getZ() >> 4)) continue;
            boolean near = false;
            for (ServerPlayer p : ow.players()) {
                double dx = p.getX() - h.getX(), dz = p.getZ() - h.getZ();
                if (dx * dx + dz * dz < 128 * 128) { near = true; break; }
            }
            if (!near) continue;
            int top = ow.getHeight(Heightmap.Types.WORLD_SURFACE, h.getX(), h.getZ());
            BlockPos below = new BlockPos(h.getX(), top - 1, h.getZ());
            if (ow.getBlockState(below).is(VoidBlocks.TINH.get()) || !ow.getFluidState(below).isEmpty()) continue;
            buildSpire(ow, h.getX(), top, h.getZ());
        }
    }

    private static void buildSpire(ServerLevel ow, int x, int y0, int z) {
        BlockState da = VoidBlocks.DA.get().defaultBlockState(), tinh = VoidBlocks.TINH.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                int hgt = 3 - Math.max(Math.abs(dx), Math.abs(dz));
                for (int k = 0; k < hgt; k++) ow.setBlock(new BlockPos(x + dx, y0 + k - 1, z + dz), da, FLAGS);
            }
        for (int k = 0; k < 16; k++) {
            ow.setBlock(new BlockPos(x, y0 + k, z), tinh, FLAGS);
            if (k < 9 && k % 3 == 1) {
                ow.setBlock(new BlockPos(x + 1, y0 + k, z), tinh, FLAGS);
                ow.setBlock(new BlockPos(x, y0 + k, z - 1), tinh, FLAGS);
            }
        }
    }
}
