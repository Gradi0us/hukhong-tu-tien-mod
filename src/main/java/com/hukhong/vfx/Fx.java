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

    /**
     * Phi kiếm thật (model Bản Mệnh Kiếm) bay from -> to trong flight tick, sau delay tick. Client dựng cung bay + bóng mờ.
     * Mã hoá: pos=from, dir=to-from, scale=cỡ kiếm, color=rgb | delay<<24, life=flight.
     */
    public static void swordFly(ServerLevel lv, Vec3 from, Vec3 to, int flight, int delay, float size) {
        Vec3 d = to.subtract(from);
        PacketDistributor.sendToPlayersNear(lv, null, from.x, from.y, from.z, 96,
                new FxPayload(FxType.SWORD_FLY.ordinal(), from.x, from.y, from.z, (float) d.x, (float) d.y, (float) d.z,
                        size, CYAN | (Math.min(255, Math.max(0, delay)) << 24), Math.max(2, flight)));
    }


    /** Kiếm rơi cắm đất (Kiếm Vực): from -> to, mũi chúc xuống, nằm lại ~1.5s rồi mờ; bụi + vết nứt nhỏ khi chạm. */
    public static void swordStab(ServerLevel lv, Vec3 from, Vec3 to, int flight, int delay, float size) {
        Vec3 d = to.subtract(from);
        PacketDistributor.sendToPlayersNear(lv, null, to.x, to.y, to.z, 96,
                new FxPayload(FxType.SWORD_STAB.ordinal(), from.x, from.y, from.z, (float) d.x, (float) d.y, (float) d.z,
                        size, 0xBFEFFF | (Math.min(255, Math.max(0, delay)) << 24), Math.max(2, flight)));
    }

    /** Kiếm của chủ vung cung chém trước mặt: roll độ (dấu = chiều quét; 90 = bổ dọc), bán kính cung, cỡ kiếm (>=2.5 = khổng lồ). */
    public static void slash(ServerLevel lv, ServerPlayer owner, double rollDeg, double radius, int delay, int sweepTicks, float size) {
        PacketDistributor.sendToPlayersNear(lv, null, owner.getX(), owner.getY(), owner.getZ(), 96,
                new FxPayload(FxType.SWORD_SLASH.ordinal(), owner.getX(), owner.getY(), owner.getZ(), owner.getId(), (float) rollDeg, (float) radius,
                        size, Math.min(255, Math.max(0, delay)) << 24, Math.max(3, sweepTicks)));
    }

    /** Dải kiếm khí lưỡi liềm bay từ pos theo dir (độ dài dir = quãng đường), bán kính radius, nghiêng rollDeg (-90..90). */
    public static void qi(ServerLevel lv, Vec3 pos, Vec3 dir, float radius, int color, double rollDeg, int lifeTicks) {
        int rb = (int) Math.round((Math.max(-90, Math.min(90, rollDeg)) + 90) * 1.4167);
        PacketDistributor.sendToPlayersNear(lv, null, pos.x, pos.y, pos.z, 96,
                new FxPayload(FxType.SWORD_QI.ordinal(), pos.x, pos.y, pos.z, (float) dir.x, (float) dir.y, (float) dir.z,
                        radius, color == 0 ? 0x9FE8FF | (rb << 24) : (color & 0xFFFFFF) | (rb << 24), lifeTicks));
    }

    /** Kiếm Vực bám theo chủ (client tự theo thực thể): gói làm tươi giữ vùng sáng; radius <= 0 = tắt dần. */
    public static void domain(ServerLevel lv, ServerPlayer owner, float radius, int lifeTicks) {
        PacketDistributor.sendToPlayersNear(lv, null, owner.getX(), owner.getY(), owner.getZ(), 96,
                new FxPayload(FxType.SWORD_DOMAIN.ordinal(), owner.getX(), owner.getY(), owner.getZ(), owner.getId(), 0f, 0f,
                        radius, CYAN, lifeTicks));
    }

    /** Khoảnh khắc cường hóa / tiến hóa: cột sáng + vòng nở + hạt bay lên quanh người chơi. */
    public static void upgradeBurst(ServerPlayer p, int color) {
        play(p.serverLevel(), FxType.UPGRADE_BURST, p.position(), new Vec3(0, 1, 0), 1f, color);
    }
}
