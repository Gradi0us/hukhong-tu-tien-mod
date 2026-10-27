package com.hukhong.bicanh;

import com.hukhong.voidblock.VoidBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cửa hang không gian của Bí Cảnh: ~9 rộng x 7 cao x 12 sâu, khoét vào địa hình, bậc thang đi xuống,
 * lót Hư Không Đá/Tinh, mặt cổng ở đáy. Hình học xác định từ (vị trí cổng, hướng ra).
 * Toạ độ cục bộ: u = độ sâu (0 = miệng), v = ngang, h = cao so với mặt đứng.
 */
public final class BiCanhCave {
    private BiCanhCave() {}

    public static final int DEPTH = 12, STAIRS = 6;
    private static final int FLAGS = 2 | 16;

    /** Nửa bề rộng theo độ cao h (0..6). */
    private static int half(int h) { return h <= 3 ? 4 : (h <= 5 ? 3 : 2); }
    private static int stand(int y0, int u) { return y0 - Math.min(u, STAIRS); }

    /** y0 (mặt đất ở miệng) suy ra từ vị trí cổng: P.y = stand(11)+1 = y0-5. */
    private static int y0Of(BlockPos portal) { return portal.getY() + STAIRS - 1; }

    private static BlockPos at(BlockPos portal, Direction out, int u, int v, int y) {
        Direction in = out.getOpposite(), side = in.getClockWise();
        BlockPos o = portal.relative(out, DEPTH - 1 - u);
        return new BlockPos(o.getX() + side.getStepX() * v, y, o.getZ() + side.getStepZ() * v);
    }

    /** Vị trí cổng nếu xây được tại miệng (x,z) mặt đất y0 hướng vào dir; null nếu không phù hợp. */
    public static BlockPos plan(ServerLevel l, int x, int z, int y0, Direction in) {
        if (y0 - STAIRS - 2 < l.getMinBuildHeight() + 5) return null;
        Direction out = in.getOpposite();
        BlockPos portal = new BlockPos(x, y0 - STAIRS + 1, z).relative(in, DEPTH - 1);
        for (int u = -1; u <= DEPTH; u += 3)
            for (int v = -5; v <= 5; v += 5) {
                BlockPos p = at(portal, out, u, v, y0);
                if (!l.hasChunkAt(p)) return null;
            }
        for (int u = 0; u <= DEPTH; u++)
            for (int v = -5; v <= 5; v++)
                for (int h = -1; h <= 7; h++) {
                    BlockPos p = at(portal, out, u, v, stand(y0, u) + h);
                    if (!l.hasChunkAt(p)) return null;
                    if (!l.getFluidState(p).isEmpty()) return null;
                }
        return portal;
    }

    public static void build(ServerLevel l, BlockPos portal, Direction out, BiCanhTier tier) {
        int y0 = y0Of(portal);
        RandomSource r = l.random;
        BlockState da = VoidBlocks.DA.get().defaultBlockState(), tinh = VoidBlocks.TINH.get().defaultBlockState();
        java.util.Set<BlockPos> inner = new java.util.HashSet<>();
        for (int u = 0; u < DEPTH; u++)
            for (int v = -4; v <= 4; v++)
                for (int h = 0; h <= 6; h++)
                    if (Math.abs(v) <= half(h)) inner.add(at(portal, out, u, v, stand(y0, u) + h));
        // lót vỏ (chỉ thay khối đặc có sẵn, không lơ lửng; sàn thì ép đặt)
        java.util.Set<BlockPos> shell = new java.util.HashSet<>();
        for (BlockPos p : inner)
            for (Direction dd : Direction.values()) {
                BlockPos q = p.relative(dd);
                if (inner.contains(q)) continue;
                Direction in = out.getOpposite();
                BlockPos mouth = at(portal, out, 0, 0, stand(y0, 0));
                // không bịt miệng hang: bỏ ô ngay trước u=0
                if (dd == out && p.getX() * in.getStepX() + p.getZ() * in.getStepZ() <= mouth.getX() * in.getStepX() + mouth.getZ() * in.getStepZ()) continue;
                shell.add(q);
            }
        for (BlockPos q : shell) {
            BlockState cur = l.getBlockState(q);
            boolean floor = inner.contains(q.above());
            if (!floor && (cur.isAir() || cur.canBeReplaced() || !cur.getFluidState().isEmpty())) continue;
            l.setBlock(q, r.nextInt(12) == 0 ? tinh : da, FLAGS);
        }
        for (BlockPos p : inner) l.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
        // trụ tinh thể ở miệng
        for (int s : new int[]{-5, 5})
            for (int k = 0; k < 3; k++)
                l.setBlock(at(portal, out, 0, s, y0 + k), k == 2 ? tinh : da, FLAGS);
        // mặt cổng 3x3 ở đáy
        BlockState ps = BiCanhRegistry.PORTAL.get().stateFor(tier, out);
        for (int v = -1; v <= 1; v++)
            for (int h = 0; h <= 2; h++)
                l.setBlock(at(portal, out, DEPTH - 1, v, stand(y0, DEPTH - 1) + h), ps, 2);
        // khung đá quanh cổng
        for (int v = -2; v <= 2; v++)
            for (int h = 0; h <= 3; h++) {
                if (Math.abs(v) < 2 && h < 3) continue;
                l.setBlock(at(portal, out, DEPTH - 1, v, stand(y0, DEPTH - 1) + h), tinh, FLAGS);
            }
    }

    /** Cổng hết hạn: gỡ mặt cổng + lớp lót, bít miệng hang (sập). */
    public static void collapse(ServerLevel l, BlockPos portal, Direction out) {
        int y0 = y0Of(portal);
        for (int u = -1; u <= DEPTH; u++)
            for (int v = -5; v <= 5; v++)
                for (int h = -1; h <= 7; h++) {
                    int y = stand(y0, Math.max(u, 0)) + h;
                    BlockPos p = at(portal, out, u, v, y);
                    if (!l.hasChunkAt(p)) continue;
                    BlockState s = l.getBlockState(p);
                    boolean ours = s.is(VoidBlocks.DA.get()) || s.is(VoidBlocks.TINH.get()) || s.is(BiCanhRegistry.PORTAL.get());
                    boolean seal = u >= 0 && u <= 3 && y < y0 && s.isAir();
                    if (ours || seal) l.setBlock(p, y < y0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS | 1);
                }
        BlockPos m = at(portal, out, 2, 0, y0 + 1);
        l.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX() + 0.5, m.getY(), m.getZ() + 0.5, 40, 2, 1, 2, 0.02);
    }
}
