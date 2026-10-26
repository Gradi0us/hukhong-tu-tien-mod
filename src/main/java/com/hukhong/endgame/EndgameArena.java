package com.hukhong.endgame;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Dựng thủ tục: đảo chính + 8 đảo lơ lửng trong Hư Không Bản Nguyên, và cổng ở Overworld. */
public final class EndgameArena {
    private EndgameArena() {}

    /** Điểm người chơi hạ xuống / điểm boss / cổng về. */
    public static final BlockPos ARRIVE = new BlockPos(0, EndgameKit.CENTER_Y + 1, 14);
    public static final BlockPos BOSS_SPOT = new BlockPos(0, EndgameKit.CENTER_Y + 1, -6);
    public static final BlockPos RETURN_PORTAL = new BlockPos(0, EndgameKit.CENTER_Y + 1, 18);

    private static int hash(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    private static Block pick(int x, int y, int z, boolean top) {
        int h = hash(x, y, z) % 100;
        if (top) return h < 8 ? Blocks.CRYING_OBSIDIAN : h < 30 ? Blocks.OBSIDIAN : h < 40 ? Blocks.AMETHYST_BLOCK : Blocks.END_STONE_BRICKS;
        return h < 12 ? Blocks.BLACKSTONE : h < 18 ? Blocks.AMETHYST_BLOCK : Blocks.END_STONE;
    }

    /** Một đảo hình nón ngược: mặt phẳng ở y=cy, dày dần về tâm. */
    private static void island(ServerLevel sl, int cx, int cy, int cz, int r) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int depth = EndgameKit.depth(Math.hypot(dx, dz), r);
                for (int k = 0; k < depth; k++) {
                    int x = cx + dx, y = cy - k, z = cz + dz;
                    sl.setBlock(new BlockPos(x, y, z), pick(x, y, z, k == 0).defaultBlockState(), 2);
                }
            }
    }

    public static void buildIslands(ServerLevel sl) {
        int cy = EndgameKit.CENTER_Y;
        island(sl, 0, cy, 0, EndgameKit.MAIN_RADIUS);
        for (int i = 0; i < EndgameKit.ISLAND_COUNT; i++) {
            int[] s = EndgameKit.island(i);
            island(sl, s[0], cy + s[1], s[2], s[3]);
            BlockPos top = new BlockPos(s[0], cy + s[1] + 1, s[2]);  // cột sáng nhỏ trên mỗi đảo
            for (int h = 0; h < 3; h++) sl.setBlock(top.above(h), Blocks.OBSIDIAN.defaultBlockState(), 2);
            sl.setBlock(top.above(3), Blocks.END_ROD.defaultBlockState(), 2);
        }
        // vòng cột quanh đảo chính
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            BlockPos b = new BlockPos((int) Math.round(Math.cos(a) * 19), cy + 1, (int) Math.round(Math.sin(a) * 19));
            for (int h = 0; h < 4; h++) sl.setBlock(b.above(h), Blocks.OBSIDIAN.defaultBlockState(), 2);
            sl.setBlock(b.above(4), Blocks.END_ROD.defaultBlockState(), 2);
        }
        sl.setBlock(RETURN_PORTAL, EndgameRegistry.PORTAL.get().defaultBlockState(), 2);
        sl.setBlock(RETURN_PORTAL.below(), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        sl.setBlock(RETURN_PORTAL.above(), Blocks.AIR.defaultBlockState(), 2);
    }

    /** Cổng ở Overworld: nền 7x7 đá khóc, 4 trụ, khối cổng ở giữa. */
    public static void buildGateway(ServerLevel ow, BlockPos ground) {
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos g = ground.offset(dx, -1, dz);
                ow.setBlock(g, (Math.abs(dx) == 3 || Math.abs(dz) == 3) ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : Blocks.OBSIDIAN.defaultBlockState(), 2);
                for (int h = 0; h < 6; h++) ow.setBlock(ground.offset(dx, h, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos c = ground.offset(3 * sx, 0, 3 * sz);
                for (int h = 0; h < 3; h++) ow.setBlock(c.above(h), Blocks.OBSIDIAN.defaultBlockState(), 2);
                ow.setBlock(c.above(3), Blocks.END_ROD.defaultBlockState(), 2);
            }
        ow.setBlock(ground, EndgameRegistry.PORTAL.get().defaultBlockState(), 2);
    }
}
