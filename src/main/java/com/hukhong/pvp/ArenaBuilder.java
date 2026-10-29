package com.hukhong.pvp;

import com.hukhong.city.CityBuilder;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Dung Dau Truong Sinh Tu theo thu tuc (khong phu thuoc builder cua city/): san tron ban kinh R, tuong thap, bac khan gia,
 * den bien. Dat ben ngoai tuong Thien Khai Hoang Do (thanh index 9), phia dong.
 */
public final class ArenaBuilder {
    private ArenaBuilder() {}

    public static final int CITY_INDEX = 9, RADIUS = 14, FLAGS = 18, FIGHT_OFFSET = 8;

    /** Thuan: toa do tam san (x,z) tu tam thanh. */
    public static int[] siteXZ(BlockPos cityCenter) { return new int[]{cityCenter.getX() + CityBuilder.HALF + RADIUS + 8, cityCenter.getZ()}; }

    public static BlockPos cityCenter(ServerLevel ow) { return CityService.builtCenter(ow, CityDef.ALL.get(CITY_INDEX)); }

    /** Dung san neu chua co. Goi khi co nguoi choi gan (chunk da nap). @return tam san (y = mat san), hoac null neu chua dung duoc. */
    public static BlockPos build(ServerLevel ow, BlockPos cityCenter) {
        int[] xz = siteXZ(cityCenter);
        int cx = xz[0], cz = xz[1];
        for (int dx = -RADIUS - 2; dx <= RADIUS + 2; dx += 16) for (int dz = -RADIUS - 2; dz <= RADIUS + 2; dz += 16) ow.getChunk((cx + dx) >> 4, (cz + dz) >> 4);
        ow.getChunk((cx + RADIUS + 2) >> 4, (cz + RADIUS + 2) >> 4);
        ow.getChunk((cx - RADIUS - 2) >> 4, (cz - RADIUS - 2) >> 4);
        int y0 = Math.max(ow.getSeaLevel() + 1, ow.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz));
        for (int dx = -RADIUS - 6; dx <= RADIUS + 6; dx++) for (int dz = -RADIUS - 6; dz <= RADIUS + 6; dz++) {
            double r = Math.sqrt(dx * dx + dz * dz);
            int x = cx + dx, z = cz + dz;
            if (r > RADIUS + 5.5) continue;
            // nen + mong
            for (int y = y0 - 5; y < y0; y++) {
                BlockPos bp = new BlockPos(x, y, z);
                var s = ow.getBlockState(bp);
                if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced()) ow.setBlock(bp, Blocks.STONE_BRICKS.defaultBlockState(), FLAGS);
            }
            // khoang trong
            for (int y = y0; y <= y0 + 9; y++) ow.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), FLAGS);
            if (r <= RADIUS) {
                boolean ring = r > RADIUS - 1.5, mid = r < 2.5;
                var floor = mid ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : ring ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                        : ((dx + dz) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                ow.setBlock(new BlockPos(x, y0 - 1, z), floor, FLAGS);
                if (r > RADIUS - 0.8) { // tuong thap 2 block + den
                    ow.setBlock(new BlockPos(x, y0, z), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(), FLAGS);
                    ow.setBlock(new BlockPos(x, y0 + 1, z), ((dx * 7 + dz * 3) % 5 == 0) ? Blocks.LANTERN.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(), FLAGS);
                }
            } else if (r <= RADIUS + 5.5) { // bac khan gia 3 bac
                int step = (int) Math.min(3, Math.ceil(r - RADIUS - 0.5));
                for (int h = 0; h < step; h++) ow.setBlock(new BlockPos(x, y0 + h, z), Blocks.STONE_BRICKS.defaultBlockState(), FLAGS);
            }
        }
        return new BlockPos(cx, y0, cz);
    }

    /** Diem dung cua hai ben (A phia tay, B phia dong), tren mat san. */
    public static BlockPos spotA(BlockPos arena) { return arena.offset(-FIGHT_OFFSET, 0, 0); }
    public static BlockPos spotB(BlockPos arena) { return arena.offset(FIGHT_OFFSET, 0, 0); }
}
