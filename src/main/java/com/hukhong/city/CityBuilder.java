package com.hukhong.city;

import com.hukhong.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

/** Dựng Lưu Ly Cảng bằng code (không dùng .nbt). Tường đá + 4 cổng, quảng trường + Ấn Hồn Đài, 4 nhà, dân làng. */
public final class CityBuilder {
    private CityBuilder() {}

    public static final int HALF = 20;
    private static final int FLAGS = 18; // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE

    public static BlockPos chooseSite(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        RandomSource r = RandomSource.create(level.getSeed() ^ 0x4C4C43L);
        BlockPos best = spawn;
        for (int i = 0; i < 10; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = 300 + r.nextDouble() * 300;
            best = new BlockPos(spawn.getX() + (int) (Math.cos(ang) * dist), 64, spawn.getZ() + (int) (Math.sin(ang) * dist));
            var b = level.getBiome(best);
            if (!b.is(BiomeTags.IS_OCEAN) && !b.is(BiomeTags.IS_RIVER)) break;
        }
        return best;
    }

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        l.setBlock(new BlockPos(x, y, z), s, FLAGS);
    }

    private static void set(ServerLevel l, int x, int y, int z, Block b) { set(l, x, y, z, b.defaultBlockState()); }

    /** Dựng thành tại site (x,z); trả tâm (y = mặt nền). */
    public static BlockPos build(ServerLevel l, BlockPos site) {
        int cx = site.getX(), cz = site.getZ();
        for (int x = (cx - HALF - 2) >> 4; x <= (cx + HALF + 2) >> 4; x++)
            for (int z = (cz - HALF - 2) >> 4; z <= (cz + HALF + 2) >> 4; z++) l.getChunk(x, z);
        int y = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1;
        while (y > l.getMinBuildHeight() + 8) {
            BlockState s = l.getBlockState(new BlockPos(cx, y, cz));
            if (s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced()) y--; else break;
        }
        int y0 = Math.max(y, l.getSeaLevel());

        // 1) san phẳng: cỏ ở y0, đất bên dưới, dọn trống phía trên
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            int x = cx + dx, z = cz + dz;
            set(l, x, y0, z, Blocks.GRASS_BLOCK);
            for (int d = 1; d <= 16; d++) {
                BlockPos bp = new BlockPos(x, y0 - d, z);
                BlockState s = l.getBlockState(bp);
                if (s.blocksMotion() && l.getFluidState(bp).isEmpty()) break;
                l.setBlock(bp, Blocks.DIRT.defaultBlockState(), FLAGS);
            }
            for (int h = 1; h <= 14; h++) {
                BlockPos bp = new BlockPos(x, y0 + h, z);
                if (!l.getBlockState(bp).isAir()) l.setBlock(bp, Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
        // 2) đường + quảng trường
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            boolean plaza = Math.abs(dx) <= 5 && Math.abs(dz) <= 5;
            boolean road = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
            if (plaza) set(l, cx + dx, y0, cz + dz, ((dx + dz) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_STONE);
            else if (road) set(l, cx + dx, y0, cz + dz, Blocks.COBBLESTONE);
        }
        // 3) tường + cổng + tháp góc
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != HALF) continue;
            boolean corner = Math.abs(dx) == HALF && Math.abs(dz) == HALF;
            int along = Math.abs(dx) == HALF ? dz : dx;
            boolean gate = Math.abs(along) <= 2;
            int top = corner ? 8 : 5;
            for (int h = 1; h <= top; h++) {
                if (gate && h <= 3) continue;
                set(l, cx + dx, y0 + h, cz + dz, Blocks.STONE_BRICKS);
            }
            if (!corner && ((dx + dz) & 1) == 0) set(l, cx + dx, y0 + 6, cz + dz, Blocks.STONE_BRICKS);
            if (!corner && Math.abs(along) == 3) set(l, cx + dx, y0 + 6, cz + dz, Blocks.LANTERN);
            if (corner) set(l, cx + dx, y0 + 9, cz + dz, Blocks.LANTERN);
        }
        // 4) bệ + Ấn Hồn Đài + cột sáng (end rod)
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(l, cx + dx, y0, cz + dz, Blocks.CHISELED_STONE_BRICKS);
        set(l, cx, y0 + 1, cz, ModBlocks.AN_HON_DAI.get());
        for (int h = 2; h <= 6; h++) set(l, cx, y0 + h, cz, Blocks.END_ROD);
        // 5) nhà
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) house(l, cx + sx * 11, cz + sz * 11, y0, sx);
        // 6) biển tên
        sign(l, cx, y0 + 1, cz + 23, 0, "hukhong.city.name");
        sign(l, cx, y0 + 1, cz - 23, 8, "hukhong.city.name");
        sign(l, cx + 23, y0 + 1, cz, 12, "hukhong.city.name");
        sign(l, cx - 23, y0 + 1, cz, 4, "hukhong.city.name");
        sign(l, cx, y0 + 1, cz + 3, 0, "hukhong.city.altar_sign");
        // 7) dân làng
        int[][] vp = {{3, 3}, {-3, 3}, {3, -3}};
        for (int[] p : vp) {
            Villager v = EntityType.VILLAGER.create(l);
            if (v == null) continue;
            v.moveTo(cx + p[0] + 0.5, y0 + 1, cz + p[1] + 0.5, l.random.nextFloat() * 360f, 0f);
            v.finalizeSpawn(l, l.getCurrentDifficultyAt(v.blockPosition()), MobSpawnType.STRUCTURE, null);
            v.setPersistenceRequired();
            l.addFreshEntity(v);
        }
        return new BlockPos(cx, y0, cz);
    }

    private static void house(ServerLevel l, int hx, int hz, int y0, int sx) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
            set(l, hx + dx, y0, hz + dz, Blocks.SPRUCE_PLANKS);
            for (int h = 1; h <= 3; h++) {
                if (corner) set(l, hx + dx, y0 + h, hz + dz, Blocks.OAK_LOG);
                else if (edge) set(l, hx + dx, y0 + h, hz + dz, Blocks.OAK_PLANKS);
            }
            set(l, hx + dx, y0 + 4, hz + dz, Blocks.OAK_PLANKS);
        }
        Direction out = sx == 1 ? Direction.WEST : Direction.EAST;
        int dxDoor = hx - sx * 3;
        set(l, dxDoor, y0 + 1, hz, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, out).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(l, dxDoor, y0 + 2, hz, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, out).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(l, hx, y0 + 2, hz + 3, Blocks.GLASS_PANE);
        set(l, hx, y0 + 2, hz - 3, Blocks.GLASS_PANE);
        set(l, hx + sx * 3, y0 + 2, hz, Blocks.GLASS_PANE);
        set(l, hx + sx * 2, y0 + 1, hz + 2, Blocks.CRAFTING_TABLE);
        set(l, hx, y0 + 3, hz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private static void sign(ServerLevel l, int x, int y, int z, int rot, String key) {
        BlockPos p = new BlockPos(x, y, z);
        l.setBlock(p, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), FLAGS);
        if (l.getBlockEntity(p) instanceof SignBlockEntity s) {
            s.setText(s.getFrontText().setMessage(1, Component.translatable(key)), true);
            s.setWaxed(true);
            s.setChanged();
        }
    }

    /** Hoang phế: đổi một phần block thành đá đen/tím, phá một số nhà. Tất định theo vị trí. */
    public static void ruin(ServerLevel l, BlockPos c) {
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
            l.getChunk((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4);
            for (int h = 0; h <= 9; h++) {
                BlockPos bp = new BlockPos(c.getX() + dx, c.getY() + h, c.getZ() + dz);
                BlockState s = l.getBlockState(bp);
                if (s.isAir() || s.is(ModBlocks.AN_HON_DAI.get()) || s.is(Blocks.END_ROD) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT)) continue;
                int hash = (int) ((bp.asLong() * 0x9E3779B97F4A7C15L) >>> 56) & 0xFF; // 0..255
                BlockState to = null;
                if (s.is(Blocks.STONE_BRICKS)) {
                    if (hash < 70) to = Blocks.BLACKSTONE.defaultBlockState();
                    else if (hash < 100) to = Blocks.AIR.defaultBlockState();
                    else if (hash < 120) to = Blocks.PURPUR_BLOCK.defaultBlockState();
                } else if (s.is(Blocks.OAK_PLANKS) || s.is(Blocks.OAK_LOG) || s.is(Blocks.GLASS_PANE) || s.is(Blocks.LANTERN)) {
                    if (hash < 90) to = Blocks.AIR.defaultBlockState();
                    else if (hash < 130) to = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    else if (hash < 150) to = Blocks.AMETHYST_BLOCK.defaultBlockState();
                } else if (h == 0 && (s.is(Blocks.POLISHED_ANDESITE) || s.is(Blocks.SMOOTH_STONE) || s.is(Blocks.COBBLESTONE))) {
                    if (hash < 90) to = Blocks.BLACKSTONE.defaultBlockState();
                    else if (hash < 120) to = Blocks.PURPUR_BLOCK.defaultBlockState();
                }
                if (to != null) l.setBlock(bp, to, FLAGS);
            }
        }
    }
}
