package com.hukhong.scaling;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Kiếp Số: độ khó theo người chơi (lưu trong persistentData của người chơi). API cho quái: {@link #mult}. */
public final class Difficulty {
    private Difficulty() {}

    public static final String KEY = "hk_kiep", HEART = "hk_tam_tinh", POWER = "hk_linh_tinh";

    /** Giá trị phía client (đồng bộ bằng KiepPayload) - chỉ dùng cho HUD. */
    public static volatile float clientKiep, clientMult = 1f;

    public static double kiep(Player p) {
        CompoundTag t = p.getPersistentData();
        return t.contains(KEY) ? t.getDouble(KEY) : 0.0;
    }

    public static void setKiep(Player p, double v) {
        p.getPersistentData().putDouble(KEY, Math.max(0, Math.min(v, ScalingConfig.d(ScalingConfig.MAX_KIEP, 100))));
    }

    /** Hệ số HP từ một Kiếp Số cụ thể (thuần tuý, dùng cho test). */
    public static double multOf(double kiep) {
        double m = 1.0 + kiep * ScalingConfig.d(ScalingConfig.HP_PER_KIEP, 0.015);
        return Math.min(m, ScalingConfig.d(ScalingConfig.MAX_MULT, 3.0));
    }

    /** Hệ số sát thương tương ứng một hệ số HP. */
    public static double dmgPart(double hpMult) {
        return 1.0 + (hpMult - 1.0) * ScalingConfig.d(ScalingConfig.DMG_RATIO, 0.4);
    }

    /** Kiếp Số trung bình của người chơi (cùng chiều không gian) trong bán kính quanh vị trí; 0 nếu không có ai. */
    public static double avgKiep(ServerLevel level, BlockPos pos) {
        int r = ScalingConfig.SPEC.isLoaded() ? ScalingConfig.RADIUS.get() : 128;
        double r2 = (double) r * r, sum = 0;
        int n = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > r2) continue;
            sum += kiep(p);
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    /** API: hệ số HP quái (>= 1) tại vị trí theo Kiếp Số trung bình người chơi gần đó. */
    public static double mult(ServerLevel level, BlockPos pos) {
        if (!ScalingConfig.on()) return 1.0;
        return multOf(avgKiep(level, pos));
    }

    /** Tốc độ tăng mỗi giây: theo thời gian chơi và cấp tu luyện. */
    public static double perSecond(int cultivationLevel) {
        return ScalingConfig.d(ScalingConfig.KIEP_PER_MINUTE, 0.10) / 60.0
                * (1.0 + cultivationLevel * ScalingConfig.d(ScalingConfig.LEVEL_WEIGHT, 0.04));
    }
}
