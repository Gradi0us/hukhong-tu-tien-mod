package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;

import static com.hukhong.client.fx.FxDraw.col;

/**
 * F6 - Phản hồi đòn đánh. Số sát thương nổi (CombatUiClient) gọi {@link #onHit}: tính độ nặng 0..1 từ sát thương,
 * loại đòn (bạo kích/kỹ năng) và tỉ lệ máu mục tiêu, rồi
 *  - chớp trắng mục tiêu (vỏ hộp cộng sáng 3-4 tick),
 *  - rung camera ngắn + giật FOV nhẹ ("hitstop" giả bằng camera: server không thể dừng thời gian) nếu đòn trúng gần và trước mặt.
 * Cũng là kho rung camera chung cho FxType.CAMERA_SHAKE (boss, va chạm cự kiếm). Tôn trọng R26 (screenEffectsOff).
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class FxHit {
    private FxHit() {}

    private static final class Shake { Vec3 at; float amp; int total, left; float radius; }
    private static final class Flash { int id; int left, total; float power; }

    private static final List<Shake> SHAKES = new ArrayList<>();
    private static final List<Flash> FLASHES = new ArrayList<>();
    private static float fovPunch;

    static void clear() { SHAKES.clear(); FLASHES.clear(); fovPunch = 0; }

    static boolean hasFlash() { return !FLASHES.isEmpty(); }

    /** Rung camera (độ) quanh at; cường độ giảm theo khoảng cách tới radius. */
    public static void shake(Vec3 at, float amp, int ticks, float radius) {
        if (ClientConfig.screenEffectsOff()) return;
        if (SHAKES.size() >= 4) SHAKES.remove(0);
        Shake s = new Shake();
        s.at = at; s.amp = amp; s.total = s.left = Math.max(2, ticks); s.radius = Math.max(4f, radius);
        SHAKES.add(s);
    }

    /** kind: 0 thường, 1 bạo kích, 2 kỹ năng, 3 hồi máu (bỏ qua). */
    public static void onHit(int entityId, float amount, byte kind, double x, double y, double z) {
        if (kind == 3 || amount <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity en = mc.level.getEntity(entityId);
        float max = en instanceof LivingEntity le ? Math.max(1f, le.getMaxHealth()) : 20f;
        float base = kind == 1 ? 0.5f : kind == 2 ? 0.42f : 0.12f;
        float size = Math.min(0.4f, (float) Math.log10(1.0 + amount) / 4f);
        float ratio = Math.min(0.3f, amount / max * 1.5f);
        float power = Math.min(1f, base + size + ratio);
        if (power >= 0.22f && FLASHES.size() < 12) {
            Flash f = new Flash();
            f.id = entityId; f.power = power; f.total = f.left = power > 0.6f ? 4 : 3;
            FLASHES.removeIf(o -> o.id == entityId);
            FLASHES.add(f);
        }
        if (power < 0.3f || ClientConfig.screenEffectsOff()) return;
        Vec3 at = new Vec3(x, y, z), eye = mc.player.getEyePosition();
        double d = at.distanceTo(eye);
        if (d > 8) return;
        Vec3 to = at.subtract(eye).normalize();
        if (to.dot(mc.player.getLookAngle()) < 0.3) return; // chỉ khi mục tiêu ở trước mặt
        float fall = (float) (1.0 - d / 10.0);
        shake(at, 0.9f * power * fall, 3 + (int) (5 * power), 10f);
        fovPunch = Math.max(fovPunch, 0.035f * power * fall);
    }

    // ---------------------------------------------------------------- camera

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (Minecraft.getInstance().isPaused()) return;
        SHAKES.removeIf(s -> --s.left <= 0);
        for (Flash f : FLASHES) f.left--;
        FLASHES.removeIf(f -> f.left <= 0);
        fovPunch *= 0.55f;
    }

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        if (SHAKES.isEmpty()) return;
        Vec3 cam = e.getCamera().getPosition();
        double pt = e.getPartialTick();
        float dp = 0, dy = 0;
        for (Shake s : SHAKES) {
            float k = (s.left - (float) pt) / s.total;
            if (k <= 0) continue;
            double dist = s.at.distanceTo(cam);
            float fall = (float) Math.max(0, 1 - dist / s.radius);
            if (fall <= 0) continue;
            double tm = s.total - s.left + pt;
            float amp = s.amp * fall * k * k; // tắt nhanh: giật mạnh rồi lặng
            dp += (float) Math.sin(tm * 3.1) * amp;
            dy += (float) Math.cos(tm * 2.3) * amp * 0.7f;
        }
        e.setPitch(e.getPitch() + dp);
        e.setYaw(e.getYaw() + dy);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (fovPunch > 0.002f) e.setFOV(e.getFOV() * (1.0 - fovPunch));
    }

    // ---------------------------------------------------------------- chớp trắng mục tiêu

    /** Vỏ hộp trắng cộng sáng quanh thực thể (nội suy theo partial tick), mờ nhanh. */
    static void renderFlash(FxDraw g, float pt) {
        if (FLASHES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Flash f : FLASHES) {
            Entity en = mc.level.getEntity(f.id);
            if (en == null || en.isRemoved() || en.isInvisible()) continue;
            float k = Math.max(0f, (f.left - pt) / f.total);
            float a = k * k * (0.28f + 0.4f * f.power);
            AABB b = en.getBoundingBox().move(en.getPosition(pt).subtract(en.position())).inflate(0.06);
            int cw = col(0xFFFFFF, a);
            double x0 = b.minX, x1 = b.maxX, y0 = b.minY, y1 = b.maxY, z0 = b.minZ, z1 = b.maxZ;
            g.quad2(new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x0, y1, z0), cw, cw, cw, cw);
            g.quad2(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), cw, cw, cw, cw);
            g.quad2(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), cw, cw, cw, cw);
            g.quad2(new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), cw, cw, cw, cw);
            g.quad2(new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), cw, cw, cw, cw);
        }
    }
}
