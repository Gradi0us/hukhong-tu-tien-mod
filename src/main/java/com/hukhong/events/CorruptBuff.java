package com.hukhong.events;

import com.hukhong.entity.HuHoa;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Quai sinh trong vung Hu Hoa nhiem: +30% cap va +30% suc manh (HP, sat thuong). */
public final class CorruptBuff {
    private CorruptBuff() {}

    public static final double MUL = 1.3;

    public static boolean at(net.minecraft.world.level.ServerLevelAccessor l, BlockPos p) {
        return l.getLevel() instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD
                && CorruptionData.get(sl).isCorrupted(p);
    }

    public static int level(boolean corrupted, int lv) {
        return corrupted ? Math.min(HuHoa.MAX_MOB_LEVEL, (int) Math.round(lv * MUL)) : lv;
    }

    public static double mul(boolean corrupted) { return corrupted ? MUL : 1.0; }
}
