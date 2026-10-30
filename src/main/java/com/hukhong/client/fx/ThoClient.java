package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.hukhong.client.keybind.SkillKeys;
import com.hukhong.phaptu.PhapMath;
import com.hukhong.phaptu.ThiemBoPayload;
import com.hukhong.phaptu.ThoPhase;
import com.hukhong.phaptu.ThoStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Thổ phía client: (1) dự đoán chuyển động xuyên tường khi server báo đang xuyên (server đặt noPhysics nên không giật lùi),
 * (2) gửi nhấn / nhả T cho cửa sổ dẫn nổ Thiên Sơn. KHÔNG đọc attachment CULTIVATION: trạng thái đến từ {@link ThoStatePayload}.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ThoClient {
    private ThoClient() {}

    private static long phaseEnd = -1, blastEnd = -1;
    private static boolean prevT;
    private static Vec3 pre;

    public static void update(ThoStatePayload pl) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        switch (pl.kind()) {
            case ThoStatePayload.PHASE_ON -> phaseEnd = now + pl.ticks() + 2;
            case ThoStatePayload.PHASE_OFF -> phaseEnd = -1;
            case ThoStatePayload.BLAST_OPEN -> blastEnd = now + pl.ticks();
            case ThoStatePayload.BLAST_CLOSED -> blastEnd = -1;
            default -> { }
        }
    }

    public static boolean phasing() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && phaseEnd >= 0 && mc.level.getGameTime() < phaseEnd;
    }

    public static boolean blastWindow() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && blastEnd >= 0 && mc.level.getGameTime() < blastEnd;
    }

    /** Gọi mỗi tick từ SkillKeyHandler. active = thế bật + trang Pháp Tu Thổ + không mở màn hình. Đọc phím T vật lý (T trùng phím chat vanilla). */
    public static void tickKeys(Minecraft mc, boolean active) {
        boolean t = active && mc.screen == null && PhapBeamClient.physicalDown(mc, SkillKeys.SKILL[3]);
        if (t && !prevT) PacketDistributor.sendToServer(new ThiemBoPayload(ThiemBoPayload.T_PRESS));
        if (!t && prevT) PacketDistributor.sendToServer(new ThiemBoPayload(ThiemBoPayload.T_RELEASE));
        prevT = t;
    }

    @SubscribeEvent
    public static void onPre(PlayerTickEvent.Pre e) {
        if (e.getEntity() instanceof LocalPlayer lp && lp == Minecraft.getInstance().player) pre = lp.position();
    }

    /**
     * Sau khi vanilla di chuyển có va chạm: nếu đang xuyên tường, tự tính lại vị trí theo phím bấm (không va chạm khối thường,
     * vẫn bị chặn bởi khối bất hoại / biên chiều cao) và ghi đè. Hạ từ từ (>= 0,035/tick) để máy chủ không coi là bay lơ lửng.
     */
    @SubscribeEvent
    public static void onPost(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof LocalPlayer lp) || lp != Minecraft.getInstance().player) return;
        if (!phasing()) { pre = null; return; }
        if (pre == null || lp.isSpectator() || !lp.isAlive()) return;
        double f = lp.input.forwardImpulse, l = lp.input.leftImpulse;
        double len = Math.sqrt(f * f + l * l);
        if (len > 1) { f /= len; l /= len; }
        double yaw = Math.toRadians(lp.getYRot()), sin = Math.sin(yaw), cos = Math.cos(yaw);
        double speed = lp.getSpeed() * 2.16; // block/tick ~ đi bộ 0,216 / chạy 0,28
        double dx = (l * cos - f * sin) * speed, dz = (f * cos + l * sin) * speed;
        double dy = lp.input.jumping ? 0.22 : lp.input.shiftKeyDown ? -0.22 : -PhapMath.THO_PHASE_SINK;
        Vec3 target = free(lp, new Vec3(dx, dy, dz));
        lp.setPos(pre.x + target.x, pre.y + target.y, pre.z + target.z);
        lp.setDeltaMovement(target);
        lp.setOnGround(false);
        lp.fallDistance = 0;
        lp.noPhysics = true;
    }

    /** Thử di chuyển d, nếu bị chặn thì thử từng trục. */
    private static Vec3 free(LocalPlayer lp, Vec3 d) {
        if (ok(lp, d)) return d;
        Vec3 best = Vec3.ZERO;
        for (Vec3 c : new Vec3[]{new Vec3(d.x, d.y, 0), new Vec3(0, d.y, d.z), new Vec3(d.x, 0, 0), new Vec3(0, 0, d.z), new Vec3(0, d.y, 0)}) {
            if (ok(lp, c)) { best = c; break; }
        }
        return best;
    }

    private static boolean ok(LocalPlayer lp, Vec3 d) {
        AABB bb = lp.getDimensions(lp.getPose()).makeBoundingBox(pre.add(d));
        return ThoPhase.allowed(lp.level(), bb);
    }
}
