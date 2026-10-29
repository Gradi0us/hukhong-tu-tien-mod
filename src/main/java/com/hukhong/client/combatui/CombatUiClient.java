package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.entity.HuMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Thanh máu + HP/Max + cấp trên đầu quái, và số sát thương nổi (billboard trong thế giới). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CombatUiClient {
    private CombatUiClient() {}

    private static final int MAX_NUMBERS = 48, NUM_LIFE = 30, BAR_SHOW = 100, BAR_FADE_IN = 6, BAR_FADE_OUT = 14;

    private static final class Num {
        final float x, y, z, amount; final byte kind; final int color; final long born; final float drift;
        Num(float x, float y, float z, float amount, byte kind, int color, long born, float drift) {
            this.x = x; this.y = y; this.z = z; this.amount = amount; this.kind = kind; this.color = color;
            this.born = born; this.drift = drift;
        }
    }

    private static final class Bar { long start, until; }

    private static final ArrayDeque<Num> NUMS = new ArrayDeque<>();
    private static final Map<Integer, Bar> BARS = new HashMap<>();

    public static void receive(CombatTextPayload pl) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (ClientConfig.damageNumbers()) {
            while (NUMS.size() >= MAX_NUMBERS) NUMS.pollFirst();
            NUMS.addLast(new Num(pl.x(), pl.y(), pl.z(), pl.amount(), pl.kind(), pl.color(), now,
                    (mc.level.random.nextFloat() - 0.5f) * 0.02f));
        }
        if (pl.kind() != CombatTextPayload.HEAL) {
            touchBar(pl.entityId(), now);
            com.hukhong.client.fx.FxHit.onHit(pl.entityId(), pl.amount(), pl.kind(), pl.x(), pl.y(), pl.z());
        }
    }

    private static void touchBar(int id, long now) {
        Bar b = BARS.computeIfAbsent(id, k -> new Bar());
        if (b.until + BAR_FADE_OUT < now) b.start = now;
        b.until = now + BAR_SHOW;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        NUMS.clear();
        BARS.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        boolean bars = ClientConfig.mobBars(), nums = ClientConfig.damageNumbers();
        if (!bars && !nums) { BARS.clear(); NUMS.clear(); return; }
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(true);
        double now = mc.level.getGameTime() + pt;
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        // ROOT CAUSE "khong thay thanh mau": 1.21.1 da nhan frustum/modelView matrix vao RenderSystem model-view stack
        // truoc khi ban su kien nay; mulPose(e.getModelViewMatrix()) nua = xoay camera 2 lan -> moi thu bi ve lech ra ngoai
        // man hinh. Dung PoseStack rong + toa do tuong doi camera (giong FxClient/ObjectiveBeam).
        PoseStack ps = new PoseStack();
        Font font = mc.font;
        if (bars) renderBars(mc, ps, buf, font, cam, cp, now, pt);
        if (nums) renderNumbers(ps, buf, font, cam, cp, now);
        buf.endBatch(RenderType.guiOverlay());
        buf.endBatch();
        if (!LOGGED) {
            LOGGED = true;
            LOG.info("CombatUi first render: bars=" + bars + " nums=" + nums + " cam=" + cp
                    + " entities=" + LAST_DRAWN + " range=" + ClientConfig.mobBarRange());
        }
    }

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static boolean LOGGED;
    private static int LAST_DRAWN;

    // ------------------------------------------------------------------ thanh máu

    private static void renderBars(Minecraft mc, PoseStack ps, MultiBufferSource.BufferSource buf, Font font,
                                   Camera cam, Vec3 cp, double now, float pt) {
        double range = ClientConfig.mobBarRange(), r2 = range * range;
        boolean players = ClientConfig.mobBarsPlayers(), always = ClientConfig.mobBarsAlways();
        long tick = mc.level.getGameTime();
        Entity target = mc.crosshairPickEntity;
        if (target instanceof LivingEntity && target != mc.player) touchBar(target.getId(), tick);
        BARS.values().removeIf(b -> b.until + BAR_FADE_OUT < tick);
        int drawn = 0;
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof LivingEntity le) || le == mc.player || !le.isAlive() || le.isInvisible()
                    || en instanceof ArmorStand) continue;
            if (en instanceof Player && !players) continue;
            if (en.distanceToSqr(mc.player) > r2) continue;
            Bar b = BARS.get(en.getId());
            float alpha = 0;
            if (b != null) {
                alpha = (float) Math.min(1.0, Math.min((now - b.start) / BAR_FADE_IN, (b.until + BAR_FADE_OUT - now) / BAR_FADE_OUT));
            } else if (always && le.getHealth() < le.getMaxHealth()) alpha = 1f;
            if (alpha <= 0.02f) continue;
            if (++drawn > 24) break;
            drawBar(ps, buf, font, cam, cp, le, alpha, pt);
        }
        LAST_DRAWN = drawn;
    }

    private static void drawBar(PoseStack ps, MultiBufferSource.BufferSource buf, Font font, Camera cam, Vec3 cp,
                                LivingEntity le, float alpha, float pt) {
        Vec3 p = le.getPosition(pt);
        double dist = p.distanceTo(cp);
        float s = 0.02f * (float) Math.max(1.0, dist / 10.0);
        ps.pushPose();
        ps.translate(p.x - cp.x, p.y + le.getBbHeight() + 0.55 - cp.y, p.z - cp.z);
        ps.mulPose(cam.rotation());
        ps.scale(-s, -s, s);
        Matrix4f m = ps.last().pose();
        float max = Math.max(1f, le.getMaxHealth()), hp = Mth.clamp(le.getHealth(), 0f, max);
        float ratio = hp / max;
        int a = Math.max(4, (int) (alpha * 255));
        int w = 44, h = 5, x0 = -w / 2;
        VertexConsumer vc = buf.getBuffer(RenderType.guiOverlay());
        quad(vc, m, x0 - 1, -1, x0 + w + 1, h + 1, 0f, (a * 190 / 255) << 24);
        quad(vc, m, x0, 0, x0 + w, h, 0.01f, (a * 200 / 255) << 24 | 0x303030);
        int fillCol = ratio > 0.5f ? 0x4CD964 : ratio > 0.25f ? 0xF2C94C : 0xEB5757;
        quad(vc, m, x0, 0, x0 + Math.round(w * ratio), h, 0.02f, a << 24 | fillCol);
        String hpTxt = fmt(hp) + "/" + fmt(max);
        String txt = le instanceof HuMob hm ? "Lv" + hm.huLevel() + "  " + hpTxt : hpTxt;
        int tw = font.width(txt);
        font.drawInBatch(txt, -tw / 2f, -10f, a << 24 | 0xFFFFFF, true, m, buf, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        ps.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float z, int argb) {
        vc.addVertex(m, x1, y2, z).setColor(argb);
        vc.addVertex(m, x2, y2, z).setColor(argb);
        vc.addVertex(m, x2, y1, z).setColor(argb);
        vc.addVertex(m, x1, y1, z).setColor(argb);
    }

    static String fmt(float v) {
        if (v >= 1_000_000f) return String.format("%.1fM", v / 1_000_000f);
        if (v >= 10_000f) return String.format("%.1fk", v / 1000f);
        return Integer.toString(Math.round(v));
    }

    // ------------------------------------------------------------------ số nổi

    private static void renderNumbers(PoseStack ps, MultiBufferSource.BufferSource buf, Font font, Camera cam,
                                      Vec3 cp, double now) {
        for (Iterator<Num> it = NUMS.iterator(); it.hasNext(); ) {
            Num n = it.next();
            double age = now - n.born;
            if (age >= NUM_LIFE) { it.remove(); continue; }
            if (age < 0) age = 0;
            float t = (float) (age / NUM_LIFE);
            float rise = (float) (1.1 * (1 - Math.pow(1 - t, 2)));            // nhanh rồi chậm lại
            float alpha = t < 0.65f ? 1f : 1f - (t - 0.65f) / 0.35f;
            double x = n.x + n.drift * age, y = n.y + rise, z = n.z;
            double dist = Math.sqrt((x - cp.x) * (x - cp.x) + (y - cp.y) * (y - cp.y) + (z - cp.z) * (z - cp.z));
            float big = 1f + Math.min(0.45f, (float) Math.log10(1.0 + n.amount) / 9f);   // đòn càng nặng số càng to
            float base = big * switch (n.kind) { case CombatTextPayload.CRIT -> 0.060f; case CombatTextPayload.SKILL -> 0.036f; default -> 0.026f; };
            float pop = t < 0.12f ? 1f + (0.12f - t) * (n.kind == CombatTextPayload.CRIT ? 9f : 4f) : 1f;   // nảy to lúc đầu (bạo kích nảy mạnh hơn)
            float s = base * pop * (float) Math.max(1.0, dist / 9.0);
            int rgb = switch (n.kind) {
                case CombatTextPayload.CRIT -> 0xFFD23C;
                case CombatTextPayload.HEAL -> 0x5CE06A;
                case CombatTextPayload.SKILL -> n.color != 0 ? (n.color & 0xFFFFFF) : 0xB8A0FF;
                default -> 0xFFFFFF;
            };
            String txt = (n.kind == CombatTextPayload.HEAL ? "+" : "") + fmt(n.amount) + (n.kind == CombatTextPayload.CRIT ? "!" : "");
            if (n.kind == CombatTextPayload.CRIT && t < 0.5f) x += Math.sin(age * 2.6) * 0.04 * (1 - t * 2); // bạo kích rung nhẹ lúc đầu
            int a = Math.max(4, (int) (alpha * 255));
            ps.pushPose();
            ps.translate(x - cp.x, y - cp.y, z - cp.z);
            ps.mulPose(cam.rotation());
            ps.scale(-s, -s, s);
            font.drawInBatch(txt, -font.width(txt) / 2f, 0f, a << 24 | rgb, true, ps.last().pose(), buf,
                    Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            ps.popPose();
        }
    }
}
