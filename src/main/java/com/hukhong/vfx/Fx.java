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

    // ================================================================ Bộ công cụ VFX mới (ribbon / decal / cự kiếm / rung camera)
    // owner != null: người nhận khác owner thấy hiệu ứng ở 50% độ đậm (giao tranh nhiều người vẫn đọc được).

    /** Cờ trong life: hiệu ứng của người chơi khác. */
    public static final int DIM = 1 << 30;

    /** Gửi cho mọi người chơi trong 96 block; người không phải owner nhận bản 50%. */
    public static void playOwned(ServerLevel lv, ServerPlayer owner, FxType type, Vec3 pos, Vec3 dir, float scale, int color, int lifeTicks) {
        Vec3 d = dir == null ? Vec3.ZERO : dir;
        double r2 = 96.0 * 96.0;
        for (ServerPlayer sp : lv.players()) {
            if (sp.distanceToSqr(pos) > r2) continue;
            int life = lifeTicks | (owner != null && sp != owner ? DIM : 0);
            PacketDistributor.sendToPlayer(sp, new FxPayload(type.ordinal(), pos.x, pos.y, pos.z, (float) d.x, (float) d.y, (float) d.z, scale, color, life));
        }
    }

    private static int arcByte(double arc) { return arc == 0 ? 0 : (int) Math.max(1, Math.min(255, Math.round(arc * 8) + 128)); }

    /** Vệt sáng bay from->to theo cung (arc block, dấu = lên/xuống), sau delay tick, bay flight tick; width = nửa rộng dải. */
    public static void ribbon(ServerLevel lv, ServerPlayer owner, Vec3 from, Vec3 to, double arc, int flight, int delay, float width, int rgb) {
        ribbonWave(lv, owner, from, to, arc, 0, flight, delay, width, rgb);
    }

    /** Như ribbon nhưng thêm uốn lượn ngang biên độ wave block (rồng, dây máu). */
    public static void ribbonWave(ServerLevel lv, ServerPlayer owner, Vec3 from, Vec3 to, double arc, double wave, int flight, int delay, float width, int rgb) {
        int life = Math.max(2, Math.min(1023, flight)) | (Math.min(255, Math.max(0, delay)) << 10) | (Math.min(31, (int) Math.round(wave * 10)) << 18);
        playOwned(lv, owner, FxType.RIBBON, from, to.subtract(from), width, (rgb & 0xFFFFFF) | (arcByte(arc) << 24), life);
    }

    /** Vệt xoắn ốc quanh trục from->to (bán kính radius, pha phaseRad): dây leo, rồng cuốn. */
    public static void ribbonHelix(ServerLevel lv, ServerPlayer owner, Vec3 from, Vec3 to, double radius, double phaseRad, int flight, int delay, float width, int rgb) {
        int life = Math.max(2, Math.min(1023, flight)) | (Math.min(255, Math.max(0, delay)) << 10) | (Math.min(31, (int) Math.round(radius * 10)) << 18) | (1 << 23);
        int ph = (int) Math.round(((phaseRad % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2)) / (Math.PI * 2) * 255);
        playOwned(lv, owner, FxType.RIBBON, from, to.subtract(from), width, (rgb & 0xFFFFFF) | (ph << 24), life);
    }

    private static void decal(ServerLevel lv, ServerPlayer owner, int kind, Vec3 pos, Vec3 dir, float scale, int rgb, int life) {
        playOwned(lv, owner, FxType.DECAL, pos, dir, scale, (rgb & 0xFFFFFF) | (kind << 24), life);
    }

    /** Pháp trận nhiều vòng xoay ngược chiều + sao n cánh. pos.y = độ cao (đặt trên trời cho "mắt trận"). */
    public static void rune(ServerLevel lv, ServerPlayer owner, Vec3 pos, float radius, int rgb, int life, int points) {
        decal(lv, owner, 0, pos, new Vec3(points, 0, 0), radius, rgb, life);
    }

    /** Vòng nở phẳng để lại dư quang trên đất. */
    public static void groundRing(ServerLevel lv, ServerPlayer owner, Vec3 pos, float radius, int rgb, int life) {
        decal(lv, owner, 1, pos, Vec3.ZERO, radius, rgb, life);
    }

    /** Nứt đất toả tia (4 = chữ thập), đá bay lên rồi rơi. */
    public static void groundCrack(ServerLevel lv, ServerPlayer owner, Vec3 pos, float radius, int rgb, int spokes, int life) {
        decal(lv, owner, 2, pos, new Vec3(spokes, 0, 0), radius, rgb, life);
    }

    /** Vũng (máu, độc...) loang ra rồi co lại. */
    public static void pool(ServerLevel lv, ServerPlayer owner, Vec3 pos, float radius, int rgb, int life) {
        decal(lv, owner, 3, pos, Vec3.ZERO, radius, rgb, life);
    }

    /** Telegraph tròn: viền + vùng đỏ lấp đầy từ tâm, tick cuối chớp trắng. ticks = thời gian tới lúc nổ. */
    public static void telegraphCircle(ServerLevel lv, ServerPlayer owner, Vec3 pos, float radius, int ticks) {
        decal(lv, owner, 4, pos, Vec3.ZERO, radius, 0, ticks);
    }

    /** Telegraph tròn có màu riêng (kỹ năng người chơi dùng màu đạo thay vì đỏ nguy hiểm). */
    public static void telegraphCircle(ServerLevel lv, ServerPlayer owner, Vec3 pos, float radius, int ticks, int rgb) {
        decal(lv, owner, 4, pos, Vec3.ZERO, radius, rgb, ticks);
    }

    /** Telegraph băng có màu riêng. */
    public static void telegraphLine(ServerLevel lv, ServerPlayer owner, Vec3 pos, Vec3 vec, float width, int ticks, int rgb) {
        decal(lv, owner, 6, pos, vec, width, rgb, ticks);
    }

    /** Telegraph hình quạt từ pos hướng facing (ngang), nửa góc halfAngleDeg, bán kính radius. */
    public static void telegraphCone(ServerLevel lv, ServerPlayer owner, Vec3 pos, Vec3 facing, float radius, float halfAngleDeg, int ticks) {
        Vec3 f = new Vec3(facing.x, 0, facing.z);
        f = f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
        decal(lv, owner, 5, pos, new Vec3(f.x, halfAngleDeg, f.z), radius, 0, ticks);
    }

    /** Telegraph hình băng từ pos theo vector vec (độ dài = tầm), bề rộng width. */
    public static void telegraphLine(ServerLevel lv, ServerPlayer owner, Vec3 pos, Vec3 vec, float width, int ticks) {
        decal(lv, owner, 6, pos, vec, width, 0, ticks);
    }

    /** Cự kiếm giáng xuống mũi tại tip (size 1 = thân ~1.3 block); đập đất ở tick impactTick; life tổng (0 = mặc định). */
    public static void giantSword(ServerLevel lv, ServerPlayer owner, Vec3 tip, float size, int rgb, int impactTick, int life) {
        playOwned(lv, owner, FxType.GIANT_SWORD, tip, Vec3.ZERO, size, (rgb & 0xFFFFFF) | (Math.min(255, Math.max(1, impactTick)) << 24), life);
    }

    /** Vết rách không gian từ pos theo vec, nửa cao halfHeight; mở rồi khép trong life tick. */
    public static void rift(ServerLevel lv, ServerPlayer owner, Vec3 pos, Vec3 vec, float halfHeight, int rgb, int life) {
        playOwned(lv, owner, FxType.RIFT, pos, vec, halfHeight, rgb, life);
    }

    /** Rung camera quanh pos: strength ~ độ, ticks, radius block. */
    public static void shake(ServerLevel lv, Vec3 pos, float strength, int ticks, float radius) {
        play(lv, FxType.CAMERA_SHAKE, pos, new Vec3(radius, 0, 0), strength, 0xFFFFFF, ticks);
    }

    /** Vỏ lục giác bám thực thể trong life tick, vỡ vụn khi hết. */
    public static void shell(ServerLevel lv, ServerPlayer owner, net.minecraft.world.entity.Entity e, float radius, int rgb, int life) {
        playOwned(lv, owner, FxType.SHELL, e.position(), new Vec3(e.getId(), 0, 0), radius, rgb, life);
    }
}