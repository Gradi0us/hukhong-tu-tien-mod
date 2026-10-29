package com.hukhong.liberation;

import com.hukhong.voidblock.VoidBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dau truong Tam Hu Hoa: 8 khe nut toa tu tam (VET_NUT, nhip dap bang hat), vong rune Hu Khong quanh vanh, 8 tru Hu Khong Tinh.
 * Chi dung cac khoi Hu Khong -> tay vung (hang doi hoan nguyen cua VoidCorruptor) tra ve dat/co/khong khi.
 */
public final class HeartArena {
    private HeartArena() {}

    public static final int RADIUS = 24;
    public static final int SPOKES = 8;

    /** Thuan: cac o (dx,dz) khe nut tat dinh theo vi tri Tam: SPOKES tia, moi tia dai 5..RADIUS-4 co lech nhe. */
    public static List<int[]> cracks(BlockPos heart) {
        Random r = new Random(heart.getX() * 31L + heart.getZ() * 17L + 0x48454152L);
        List<int[]> out = new ArrayList<>();
        for (int s = 0; s < SPOKES; s++) {
            double ang = s * Math.PI * 2 / SPOKES + (r.nextDouble() - 0.5) * 0.3;
            int len = RADIUS - 4 - r.nextInt(6);
            double wob = 0;
            for (int d = 5; d <= len; d++) {
                wob += (r.nextDouble() - 0.5) * 0.35;
                out.add(new int[]{(int) Math.round(Math.cos(ang + wob * 0.05) * d + wob), (int) Math.round(Math.sin(ang + wob * 0.05) * d - wob)});
            }
        }
        return out;
    }

    /** Thuan: cac o (dx,dz) cua 8 tru. */
    public static List<int[]> pillars() {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < SPOKES; i++) {
            double a = i * Math.PI * 2 / SPOKES + Math.PI / SPOKES;
            out.add(new int[]{(int) Math.round(Math.cos(a) * (RADIUS - 3)), (int) Math.round(Math.sin(a) * (RADIUS - 3))});
        }
        return out;
    }

    /** Du chunk 5x5 quanh Tam da nap? */
    public static boolean loaded(ServerLevel l, BlockPos heart) {
        int cx = heart.getX() >> 4, cz = heart.getZ() >> 4;
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (!l.hasChunk(cx + i, cz + j)) return false;
        return true;
    }

    public static boolean build(ServerLevel l, BlockPos heart) {
        if (!loaded(l, heart)) return false;
        BlockState rift = VoidBlocks.VET_NUT.get().defaultBlockState(), da = VoidBlocks.DA.get().defaultBlockState(),
                tinh = VoidBlocks.TINH.get().defaultBlockState();
        for (int[] c : cracks(heart)) {
            int x = heart.getX() + c[0], z = heart.getZ() + c[1];
            int top = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos p = new BlockPos(x, top, z);
            if (l.getFluidState(p).isEmpty() && l.getBlockState(p).isSolid() && l.getBlockEntity(p) == null) l.setBlock(p, rift, 2 | 16);
        }
        // vong rune vanh ngoai: xen ke Hu Khong Da / Tinh sat mat dat
        for (int i = 0; i < 96; i++) {
            double a = i * Math.PI * 2 / 96;
            int x = heart.getX() + (int) Math.round(Math.cos(a) * RADIUS), z = heart.getZ() + (int) Math.round(Math.sin(a) * RADIUS);
            int top = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos p = new BlockPos(x, top, z);
            if (l.getFluidState(p).isEmpty() && l.getBlockState(p).isSolid() && l.getBlockEntity(p) == null)
                l.setBlock(p, i % 4 == 0 ? tinh : da, 2 | 16);
        }
        int k = 0;
        for (int[] c : pillars()) {
            int x = heart.getX() + c[0], z = heart.getZ() + c[1];
            int top = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (!l.getFluidState(new BlockPos(x, top - 1, z)).isEmpty()) continue;
            int h = 6 + (k++ % 3) * 2;
            for (int y = 0; y < h; y++) l.setBlock(new BlockPos(x, top + y, z), y == h - 1 ? tinh : da, 2 | 16);
        }
        return true;
    }
}
