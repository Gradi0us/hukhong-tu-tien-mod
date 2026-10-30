package com.hukhong.phaptu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Luật xuyên tường của Thổ E (dùng chung server + client): khối bất hoại (bedrock, barrier, tường Bí Cảnh...) chặn, trần/đáy thế giới chặn. */
public final class ThoPhase {
    private ThoPhase() {}

    /** Có khối bất hoại (độ cứng < 0: bedrock, barrier, command block...) chạm hộp bb không. */
    public static boolean blocked(Level lv, AABB bb) {
        int x0 = (int) Math.floor(bb.minX), x1 = (int) Math.floor(bb.maxX - 1.0E-4);
        int y0 = (int) Math.floor(bb.minY), y1 = (int) Math.floor(bb.maxY - 1.0E-4);
        int z0 = (int) Math.floor(bb.minZ), z1 = (int) Math.floor(bb.maxZ - 1.0E-4);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            m.set(x, y, z);
            if (!lv.isLoaded(m)) return true;
            BlockState s = lv.getBlockState(m);
            if (!s.isAir() && s.getDestroySpeed(lv, m) < 0) return true;
        }
        return false;
    }

    /** Biên an toàn theo chiều cao: không xuyên xuống hư không / lên trần thế giới. */
    public static boolean yOutOfBounds(Level lv, double y) { return y < lv.getMinBuildHeight() + 2 || y > lv.getMaxBuildHeight() - 3; }

    /** Hộp có thể đặt người chơi ở đây không (đã trừ khối bất hoại và biên chiều cao). */
    public static boolean allowed(Level lv, AABB bb) { return !yOutOfBounds(lv, bb.minY) && !blocked(lv, bb); }
}
