package com.hukhong.client.boss;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.CutscenePayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.List;

/**
 * Cutscene camera phía client (chỉ client, server không khoá ai). Cách làm: một ArmorStand "máy quay" chỉ sống ở client
 * (không thêm vào level) được đặt làm camera entity — nên vị trí + góc + FOV đều điều khiển được và mượt nhờ nội suy
 * partial-tick của Camera. Đường đi theo {@link CamPath} (Hermite), có hoà vào/ra từ góc nhìn người chơi, thanh đen điện ảnh,
 * phụ đề (không lồng tiếng), phím bỏ qua ({@link BossKeys#SKIP}). Tự thoát khi bị đánh, chết, đổi thế giới hoặc hết giờ.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CutsceneClient {
    private CutsceneClient() {}

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "boss_cutscene");
    private static final int BLEND_IN = 10, BLEND_OUT = 10, SKIP_GRACE = 8, MISSING_GRACE = 40;

    private static CutscenePayload cur;
    private static int elapsed, missing;
    private static boolean skip;
    private static ArmorStand cam;
    private static Vec3 anchor = Vec3.ZERO;
    private static CameraType prevType;
    private static Entity prevCamera;
    private static Vec3 playerEye = Vec3.ZERO;
    private static float playerYaw, playerPitch;

    public static boolean active() { return cur != null; }

    private static int total() { return cur == null ? 0 : cur.ticks() + BLEND_OUT; }

    // ------------------------------------------------------------------ vòng đời
    public static void start(CutscenePayload pl) {
        Minecraft mc = Minecraft.getInstance();
        if (cur != null || mc.level == null || mc.player == null || !mc.player.isAlive() || pl.keys().isEmpty()) return;
        cur = pl;
        elapsed = 0; missing = 0; skip = false;
        anchor = new Vec3(pl.ax(), pl.ay(), pl.az());
        prevType = mc.options.getCameraType();
        prevCamera = mc.getCameraEntity();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        cam.setInvisible(true);
        capturePlayer(mc);
        Vec3 p = playerEye;
        cam.moveTo(p.x, p.y - cam.getEyeHeight(), p.z, playerYaw, playerPitch);
        cam.setOldPosAndRot();
        mc.setCameraEntity(cam);
        while (BossKeys.SKIP.consumeClick()) { /* bỏ nhấn Space cũ (nhảy) */ }
    }

    private static void end() {
        Minecraft mc = Minecraft.getInstance();
        if (cur == null) return;
        cur = null;
        cam = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        else if (prevCamera != null) mc.setCameraEntity(prevCamera);
        if (prevType != null) mc.options.setCameraType(prevType);
        prevCamera = null;
        while (BossKeys.SKIP.consumeClick()) { /* nuốt */ }
    }

    private static void capturePlayer(Minecraft mc) {
        playerEye = mc.player.getEyePosition();
        playerYaw = mc.player.getYRot();
        playerPitch = mc.player.getXRot();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { cur = null; cam = null; prevCamera = null; }

    // ------------------------------------------------------------------ tick
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (cur == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive() || cam == null || cam.level() != mc.level) { end(); return; }
        if (mc.isPaused()) return;
        elapsed++;
        while (BossKeys.SKIP.consumeClick()) if (elapsed > SKIP_GRACE) skip = true;
        if (skip || elapsed >= total() || (elapsed > 4 && mc.player.hurtTime > 0)) { end(); return; }
        // neo theo boss (nếu có)
        if (cur.entityId() >= 0) {
            Entity b = mc.level.getEntity(cur.entityId());
            if (b != null && b.isAlive()) { anchor = b.position(); missing = 0; }
            else if (++missing > MISSING_GRACE) { end(); return; }
        }
        capturePlayer(mc);
        Pose p = poseAt(mc, elapsed);
        cam.setOldPosAndRot();
        cam.setPos(p.pos.x, p.pos.y - cam.getEyeHeight(), p.pos.z);
        cam.setYRot(p.yaw);
        cam.setXRot(p.pitch);
        cam.setYHeadRot(p.yaw);
    }

    private record Pose(Vec3 pos, float yaw, float pitch, float fov) {}

    /** Tư thế camera tại thời điểm t (tick, có thể lẻ), đã hoà vào/ra từ góc nhìn người chơi. */
    private static Pose poseAt(Minecraft mc, float t) {
        int T = cur.ticks();
        CamPath.Pose k = CamPath.sample(cur.keys(), Math.min(t, T));
        double yr = Math.toRadians(cur.refYaw());
        Vec3 fwd = new Vec3(-Math.sin(yr), 0, Math.cos(yr)), right = new Vec3(-Math.cos(yr), 0, -Math.sin(yr));
        Vec3 look = anchor.add(0, k.lookH(), 0);
        Vec3 pos = anchor.add(fwd.scale(k.dz())).add(right.scale(k.dx())).add(0, k.dy(), 0);
        // không xuyên tường: kéo camera về phía điểm nhìn nếu bị chặn
        if (cam != null && mc.level != null) {
            var hit = mc.level.clip(new ClipContext(look, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cam));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 toward = look.subtract(hit.getLocation());
                pos = toward.lengthSqr() < 1e-6 ? hit.getLocation() : hit.getLocation().add(toward.normalize().scale(0.35));
            }
        }
        Vec3 d = look.subtract(pos);
        double hz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG) + k.yaw();
        float pitch = (float) (-Mth.atan2(d.y, hz) * Mth.RAD_TO_DEG) + k.pitch();
        float fov = k.fov();
        // hoà vào (đầu) và hoà ra (cuối) với góc nhìn người chơi
        float w = 1f;
        if (t < BLEND_IN) w = smooth(t / BLEND_IN);
        else if (t > T) w = smooth(1f - (t - T) / BLEND_OUT);
        if (w < 1f) {
            pos = playerEye.lerp(pos, w);
            yaw = playerYaw + Mth.wrapDegrees(yaw - playerYaw) * w;
            pitch = Mth.lerp(w, playerPitch, pitch);
            fov = Mth.lerp(w, (float) mc.options.fov().get(), fov);
        }
        return new Pose(pos, yaw, pitch, fov);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3 - 2 * x);
    }

    // ------------------------------------------------------------------ FOV
    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (cur == null || cam == null || !e.usedConfiguredFov()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        e.setFOV(poseAt(mc, elapsed + (float) e.getPartialTick()).fov());
    }

    // ------------------------------------------------------------------ khoá đầu vào phía client (không đụng server)
    @SubscribeEvent
    public static void onMove(MovementInputUpdateEvent e) {
        if (cur == null) return;
        var in = e.getInput();
        in.up = in.down = in.left = in.right = in.jumping = in.shiftKeyDown = false;
        in.forwardImpulse = 0f;
        in.leftImpulse = 0f;
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered e) {
        if (cur != null) { e.setCanceled(true); e.setSwingHand(false); }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (cur != null) e.setCanceled(true);
    }

    // ------------------------------------------------------------------ GUI
    @SubscribeEvent
    public static void onRegisterLayer(RegisterGuiLayersEvent e) {
        e.registerAboveAll(LAYER, CutsceneClient::renderOverlay);
    }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (cur != null && !LAYER.equals(e.getName())) e.setCanceled(true);
    }

    private static void renderOverlay(GuiGraphics g, net.minecraft.client.DeltaTracker dt) {
        if (cur == null) return;
        Minecraft mc = Minecraft.getInstance();
        float t = elapsed + dt.getGameTimeDeltaPartialTick(false);
        float env = CamPath.envelope(t, total(), 12, 14);
        int w = g.guiWidth(), h = g.guiHeight();
        int bar = (int) (h * 0.115f * smooth(env));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        Font font = mc.font;
        // gợi ý phím bỏ qua
        Component hint = Component.translatable("hukhong.cutscene.skip", BossKeys.SKIP.getTranslatedKeyMessage());
        if (bar > 12) g.drawString(font, hint, w - font.width(hint) - 8, Math.max(2, bar - 12), 0x99FFFFFF, false);
        // phụ đề
        for (SubLine s : cur.subs()) {
            if (t < s.from() || t >= s.to()) continue;
            float a = Math.min(Math.min(1f, (t - s.from()) / 6f), Math.min(1f, (s.to() - t) / 6f));
            int alpha = (int) (255 * Mth.clamp(a, 0f, 1f));
            if (alpha < 8) continue;
            List<FormattedCharSequence> lines = font.split(Component.translatable(s.key()), (int) (w * 0.6f));
            int lh = font.lineHeight + 2, bh = lines.size() * lh + 8;
            int y = h - bar - bh - 6;
            int bw = 0;
            for (FormattedCharSequence l : lines) bw = Math.max(bw, font.width(l));
            g.fill(w / 2 - bw / 2 - 8, y, w / 2 + bw / 2 + 8, y + bh, (alpha * 150 / 255) << 24);
            int ty = y + 4;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, w / 2 - font.width(l) / 2, ty, 0xF2E6FF | (alpha << 24), false);
                ty += lh;
            }
        }
    }
}
