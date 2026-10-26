package com.hukhong.chg;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Đấu trường hang ổ CHG dựng bằng code (không cần file structure): đĩa bán kính 18, 8 cột đèn, bệ giữa. */
public final class ChgArena {
    private ChgArena() {}

    private static Block[] theme(int idx) {  // floor, accent, pillar, light
        return switch (idx) {
            case 1 -> new Block[]{Blocks.SMOOTH_BASALT, Blocks.MAGMA_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.SHROOMLIGHT};
            case 2 -> new Block[]{Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.GLOWSTONE};
            case 3 -> new Block[]{Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.BLUE_ICE, Blocks.SEA_LANTERN};
            default -> new Block[]{Blocks.RED_NETHER_BRICKS, Blocks.NETHERRACK, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRYING_OBSIDIAN};
        };
    }

    /** Dựng tại tâm (cx, cy, cz): cy = mặt sàn. Tải/sinh các chunk liên quan (chỉ gọi một lần mỗi hang). */
    public static void build(ServerLevel sl, int cx, int cy, int cz, int idx) {
        Block[] t = theme(idx);
        BlockState floor = t[0].defaultBlockState(), accent = t[1].defaultBlockState(),
                pillar = t[2].defaultBlockState(), light = t[3].defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        int r = ChgKit.LAIR_RADIUS;
        for (int a = (cx - r) >> 4; a <= (cx + r) >> 4; a++)
            for (int b = (cz - r) >> 4; b <= (cz + r) >> 4; b++) sl.getChunk(a, b);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r) continue;
                BlockState top = (d < 3.5 || ((int) d) % 6 == 0) ? accent : floor;
                for (int y = cy - 6; y < cy; y++) {  // móng
                    p.set(cx + dx, y, cz + dz);
                    if (!sl.getBlockState(p).isSolid()) sl.setBlock(p, floor, 18);
                }
                p.set(cx + dx, cy, cz + dz);
                sl.setBlock(p, top, 18);
                for (int y = cy + 1; y <= cy + 14; y++) {
                    p.set(cx + dx, y, cz + dz);
                    if (!sl.getBlockState(p).isAir()) sl.setBlock(p, air, 18);
                }
            }
        }
        for (int i = 0; i < 8; i++) {  // 8 cột đèn quanh vành
            double a = i * Math.PI / 4;
            int px = cx + (int) Math.round(Math.cos(a) * (r - 1)), pz = cz + (int) Math.round(Math.sin(a) * (r - 1));
            for (int y = cy + 1; y <= cy + 6; y++) sl.setBlock(p.set(px, y, pz), pillar, 18);
            sl.setBlock(p.set(px, cy + 7, pz), light, 18);
        }
        for (int dx = -2; dx <= 2; dx++)  // bệ giữa
            for (int dz = -2; dz <= 2; dz++)
                if (dx * dx + dz * dz <= 5) sl.setBlock(p.set(cx + dx, cy + 1, cz + dz), pillar, 18);
    }
}
