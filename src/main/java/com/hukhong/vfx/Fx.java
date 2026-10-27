package com.hukhong.vfx;

import com.hukhong.network.FxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** API VFX công khai cho mọi hệ thống: một gói nhỏ, client tự dựng hiệu ứng (beam, sét, decal, vòng chấn...). */
public final class Fx {
    private Fx() {}

    public static final int GOLD = 0xFFD866, CYAN = 0x8FE6FF, RED = 0xC01028, ORANGE = 0xFF8A30, PURPLE = 0xA050FF, WHITE = 0xFFFFFF;

    /** color = 0xRRGGBB (0 -> màu mặc định theo loại); scale ~ bán kính/chiều dài theo loại (1 = chuẩn). */
    public static void play(ServerLevel lv, FxType type, Vec3 pos, Vec3 dir, float scale, int color) {
        play(lv, type, pos, dir, scale, color, 0);
    }

    public static void play(ServerLevel lv, FxType type, Vec3 pos, Vec3 dir, float scale, int color, int lifeTicks) {
        Vec3 d = dir == null ? Vec3.ZERO : dir;
        PacketDistributor.sendToPlayersNear(lv, null, pos.x, pos.y, pos.z, 96,
                new FxPayload(type.ordinal(), pos.x, pos.y, pos.z, (float) d.x, (float) d.y, (float) d.z, scale, color, lifeTicks));
    }

    /** Khoảnh khắc cường hóa / tiến hóa: cột sáng + vòng nở + hạt bay lên quanh người chơi. */
    public static void upgradeBurst(ServerPlayer p, int color) {
        play(p.serverLevel(), FxType.UPGRADE_BURST, p.position(), new Vec3(0, 1, 0), 1f, color);
    }
}
