package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Hình học khí quyển cỡ lớn: cột sáng tím trên Tâm Hư Hóa, xoáy trời phía trên, khe nứt Hư Không ở chân trời khi xâm lăng,
 * tia sáng mờ (god-ray) ở Bí Cảnh / Tâm Thức. Dải mềm cộng sáng, luôn quay mặt về camera theo phương ngang.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class AmbientBeams {
    private AmbientBeams() {}

    private static final int RIFT_COUNT = 4;
    private static final double RIFT_RADIUS = 125;

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        if (lv == null || !AmbientClient.active()) return;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cp = e.getCamera().getPosition();
        float time = AmbientClient.ticks() + pt;
        boolean hearts = AmbientClient.overworld() && !AmbientState.hearts().isEmpty();
        boolean war = AmbientClient.overworld() && AmbientClient.war(pt) > 0.02f && (AmbientState.invasion() || AmbientState.raid());
        boolean rays = (AmbientClient.inBiCanh() || AmbientClient.inTamThuc()) && AmbientClient.dust(pt) > 0.02f;
        if (!hearts && !war && !rays) return;

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        if (hearts) for (BlockPos h : AmbientState.hearts()) heart(h, cp, time, AmbientClient.master(pt));
        if (war) rifts(mc, lv, cp, time, AmbientClient.war(pt));
        if (rays) godRays(mc, cp, time, AmbientClient.dust(pt));
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void additive() { RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE); }

    private static BufferBuilder begin() { return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR); }

    private static void end(BufferBuilder bb) {
        MeshData md = bb.build();
        if (md != null) BufferUploader.drawWithShader(md);
    }

    /** Vector ngang vuông góc hướng nhìn tới (dx,dz), độ dài 1. */
    private static float[] perp(double dx, double dz) {
        double h = Math.hypot(dx, dz);
        if (h < 1e-3) return new float[]{1f, 0f};
        return new float[]{(float) (-dz / h), (float) (dx / h)};
    }

    private static void v(BufferBuilder bb, double x, double y, double z, float r, float g, float b, float a) {
        bb.addVertex((float) x, (float) y, (float) z).setColor(r, g, b, Mth.clamp(a, 0f, 1f));
    }

    // ---------------------------------------------------------------- cột sáng + xoáy
    private static void heart(BlockPos h, Vec3 cp, float time, float master) {
        double dx = h.getX() + 0.5 - cp.x, dz = h.getZ() + 0.5 - cp.z, dy = h.getY() - cp.y;
        double hd = Math.hypot(dx, dz);
        if (hd > 900) return;
        float distFade = (float) Mth.clamp((860 - hd) / 200.0, 0, 1) * master;
        if (distFade <= 0.003f) return;
        float[] p = perp(dx, dz);
        float widen = (float) (1 + hd / 160.0);
        float flick = 0.88f + 0.12f * Mth.sin(time * 0.13f);
        additive();
        BufferBuilder bb = begin();
        double top = 330 - h.getY();
        int N = 22;
        float[][] layers = { // {halfWidth, alpha, r, g, b}
                {5.2f, 0.10f, 0.45f, 0.18f, 0.85f},
                {2.5f, 0.22f, 0.62f, 0.30f, 1.00f},
                {0.9f, 0.62f, 0.86f, 0.70f, 1.00f},
        };
        for (float[] L : layers) {
            float w = L[0] * widen;
            for (int i = 0; i < N; i++) {
                double y1 = -3 + top * Math.pow(i / (double) N, 1.6), y2 = -3 + top * Math.pow((i + 1) / (double) N, 1.6);
                float a1 = L[1] * distFade * flick * vFade(i / (float) N), a2 = L[1] * distFade * flick * vFade((i + 1) / (float) N);
                double wx = p[0] * w, wz = p[1] * w;
                v(bb, dx - wx, dy + y1, dz - wz, L[2], L[3], L[4], a1);
                v(bb, dx + wx, dy + y1, dz + wz, L[2], L[3], L[4], a1);
                v(bb, dx + wx, dy + y2, dz + wz, L[2], L[3], L[4], a2);
                v(bb, dx - wx, dy + y2, dz - wz, L[2], L[3], L[4], a2);
            }
        }
        end(bb);
        vortex(dx, dy + 150, dz, time, distFade);
    }

    private static float vFade(float t) { return (float) (Math.pow(1 - t, 0.9) * Mth.clamp(t * 14f, 0.35f, 1f)); }

    private static void vortex(double cx, double cy, double cz, float time, float fade) {
        // lõi tối (pha trộn thường) rồi các nhánh xoáy cộng sáng
        RenderSystem.defaultBlendFunc();
        BufferBuilder core = begin();
        int S = 30;
        for (int j = 0; j < S; j++) {
            double a1 = j * Math.PI * 2 / S, a2 = (j + 1) * Math.PI * 2 / S;
            v(core, cx, cy, cz, 0.03f, 0f, 0.07f, 0.62f * fade);
            v(core, cx + Math.cos(a1) * 46, cy, cz + Math.sin(a1) * 46, 0.05f, 0f, 0.10f, 0f);
            v(core, cx + Math.cos(a2) * 46, cy, cz + Math.sin(a2) * 46, 0.05f, 0f, 0.10f, 0f);
            v(core, cx, cy, cz, 0.03f, 0f, 0.07f, 0.62f * fade);
        }
        end(core);
        additive();
        BufferBuilder bb = begin();
        int ARMS = 4, STEPS = 26;
        double spin = time * 0.009;
        for (int arm = 0; arm < ARMS; arm++) {
            double base = arm * Math.PI * 2 / ARMS + spin;
            for (int i = 0; i < STEPS; i++) {
                double t1 = i / (double) STEPS, t2 = (i + 1) / (double) STEPS;
                double[] a = armPt(base, t1), b = armPt(base, t2);
                double w1 = 1.5 + 6.5 * t1, w2 = 1.5 + 6.5 * t2;
                float al1 = arm(t1) * 0.5f * fade, al2 = arm(t2) * 0.5f * fade;
                double n1x = -Math.sin(Math.atan2(a[1], a[0])) , n1z = Math.cos(Math.atan2(a[1], a[0]));
                double n2x = -Math.sin(Math.atan2(b[1], b[0])) , n2z = Math.cos(Math.atan2(b[1], b[0]));
                // vuông góc với bán kính (theo hướng tiếp tuyến ~ mở rộng dải)
                double r1x = Math.cos(Math.atan2(a[1], a[0])), r1z = Math.sin(Math.atan2(a[1], a[0]));
                double r2x = Math.cos(Math.atan2(b[1], b[0])), r2z = Math.sin(Math.atan2(b[1], b[0]));
                float cr = 0.55f + 0.3f * (float) t1, cg = 0.18f, cb = 0.95f - 0.25f * (float) t1;
                v(bb, cx + a[0] - r1x * w1, cy, cz + a[1] - r1z * w1, cr, cg, cb, al1);
                v(bb, cx + a[0] + r1x * w1, cy, cz + a[1] + r1z * w1, cr, cg, cb, al1);
                v(bb, cx + b[0] + r2x * w2, cy, cz + b[1] + r2z * w2, cr, cg, cb, al2);
                v(bb, cx + b[0] - r2x * w2, cy, cz + b[1] - r2z * w2, cr, cg, cb, al2);
            }
        }
        end(bb);
    }

    private static double[] armPt(double base, double t) {
        double r = 4 + 50 * t, th = base + t * Math.PI * 1.35;
        return new double[]{Math.cos(th) * r, Math.sin(th) * r};
    }

    private static float arm(double t) { return (float) (Math.sin(Math.PI * Math.pow(t, 0.55)) * (1 - 0.35 * t)); }

    // ---------------------------------------------------------------- khe nứt xâm lăng
    private static void rifts(Minecraft mc, ClientLevel lv, Vec3 cp, float time, float war) {
        double ex, ez;
        if (AmbientState.raid() && !AmbientState.invasion()) { ex = mc.player.getX(); ez = mc.player.getZ(); }
        else { ex = AmbientState.eventX(); ez = AmbientState.eventZ(); }
        double pd = Math.hypot(cp.x - ex, cp.z - ez);
        if (pd > 420) return;
        for (int i = 0; i < RIFT_COUNT; i++) {
            double ang = Math.PI * 0.25 + i * Math.PI * 0.5 + 0.35 * Math.sin(i * 12.9898);
            double rx = ex + Math.cos(ang) * RIFT_RADIUS, rz = ez + Math.sin(ang) * RIFT_RADIUS;
            int gy = lv.getChunkSource().hasChunk((int) Math.floor(rx) >> 4, (int) Math.floor(rz) >> 4)
                    ? lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(rx), (int) Math.floor(rz))
                    : (int) mc.player.getY();
            rift(rx - cp.x, gy + 15 - cp.y, rz - cp.z, time, war, i);
        }
    }

    private static void rift(double dx, double dy, double dz, float time, float war, int seed) {
        double hd = Math.hypot(dx, dz);
        float fade = war * (float) Mth.clamp((hd - 14) / 30.0, 0, 1);
        if (fade <= 0.01f) return;
        float[] p = perp(dx, dz);
        float open = (float) (1 - Math.pow(1 - Mth.clamp(war * 1.1f, 0, 1), 3));
        float pulse = 0.85f + 0.15f * Mth.sin(time * 0.09f + seed * 2f);
        int N = 30;
        double hw = 7.5 * open, hh = 17 * open;
        double[] ox = new double[N + 1], oy = new double[N + 1], jr = new double[N + 1];
        for (int k = 0; k <= N; k++) {
            double th = (k % N) * Math.PI * 2 / N;
            double j = 1 + 0.16 * Math.sin(5 * th + time * 0.05 + seed) + 0.09 * Math.sin(11 * th - time * 0.083 + seed * 1.7) + 0.05 * Math.sin(19 * th + time * 0.2);
            jr[k] = j;
            ox[k] = Math.cos(th) * hw * j;
            oy[k] = Math.sin(th) * hh * j;
        }
        // nền tối bên trong (pha trộn thường)
        RenderSystem.defaultBlendFunc();
        BufferBuilder fill = begin();
        for (int k = 0; k < N; k++) {
            v(fill, dx, dy, dz, 0.07f, 0f, 0.12f, 0.80f * fade);
            v(fill, dx + p[0] * ox[k], dy + oy[k], dz + p[1] * ox[k], 0.10f, 0.0f, 0.16f, 0.72f * fade);
            v(fill, dx + p[0] * ox[k + 1], dy + oy[k + 1], dz + p[1] * ox[k + 1], 0.10f, 0.0f, 0.16f, 0.72f * fade);
            v(fill, dx, dy, dz, 0.07f, 0f, 0.12f, 0.80f * fade);
        }
        end(fill);
        // viền sáng: đỏ tím ngoài, lavender trong
        additive();
        BufferBuilder ring = begin();
        for (int k = 0; k < N; k++) {
            double s0 = 1.0, s1 = 1.0 + 0.30, s2 = 0.86;
            float a = 0.55f * fade * pulse;
            quadRing(ring, dx, dy, dz, p, ox, oy, k, s2, s0, 0.35f, 0.15f, 0.75f, 0f, 0.95f, 0.75f, 1.0f, a);
            quadRing(ring, dx, dy, dz, p, ox, oy, k, s0, s1, 0.95f, 0.75f, 1.0f, a, 0.85f, 0.15f, 0.30f, 0f);
        }
        end(ring);
    }

    private static void quadRing(BufferBuilder bb, double dx, double dy, double dz, float[] p, double[] ox, double[] oy, int k,
                                 double sIn, double sOut, float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        v(bb, dx + p[0] * ox[k] * sIn, dy + oy[k] * sIn, dz + p[1] * ox[k] * sIn, r1, g1, b1, a1);
        v(bb, dx + p[0] * ox[k] * sOut, dy + oy[k] * sOut, dz + p[1] * ox[k] * sOut, r2, g2, b2, a2);
        v(bb, dx + p[0] * ox[k + 1] * sOut, dy + oy[k + 1] * sOut, dz + p[1] * ox[k + 1] * sOut, r2, g2, b2, a2);
        v(bb, dx + p[0] * ox[k + 1] * sIn, dy + oy[k + 1] * sIn, dz + p[1] * ox[k + 1] * sIn, r1, g1, b1, a1);
    }

    // ---------------------------------------------------------------- god-ray Bí Cảnh / Tâm Thức
    private static void godRays(Minecraft mc, Vec3 cp, float time, float dust) {
        float[] pal = AmbientClient.inBiCanh() ? AmbientSky.tierPalette(AmbientState.bicanhTier()) : AmbientSky.tierPalette(0);
        final int CELL = 20;
        int gx = Mth.floor(cp.x / CELL), gz = Mth.floor(cp.z / CELL);
        additive();
        BufferBuilder bb = begin();
        boolean any = false;
        for (int ix = gx - 3; ix <= gx + 3; ix++) {
            for (int iz = gz - 3; iz <= gz + 3; iz++) {
                int h = hash(ix, iz);
                if ((h & 0xFF) > 70) continue;
                double bx = (ix + ((h >> 8 & 0xFF) / 255.0)) * CELL, bz = (iz + ((h >> 16 & 0xFF) / 255.0)) * CELL;
                double dx = bx - cp.x, dz = bz - cp.z;
                double hd = Math.hypot(dx, dz);
                float df = (float) Mth.clamp((62 - hd) / 30.0, 0, 1);
                if (df <= 0) continue;
                float w = 2.2f + (h >> 24 & 7) * 0.55f;
                float[] p = perp(dx, dz);
                float breathe = 0.6f + 0.4f * Mth.sin(time * 0.012f + h * 0.01f);
                double slx = 9, slz = 4; // nghiêng
                int N = 6;
                for (int i = 0; i < N; i++) {
                    double t1 = i / (double) N, t2 = (i + 1) / (double) N;
                    float a1 = beamA(t1) * 0.075f * dust * df * breathe, a2 = beamA(t2) * 0.075f * dust * df * breathe;
                    double y1 = 24 - 36 * t1, y2 = 24 - 36 * t2;
                    double x1 = dx + slx * t1, z1 = dz + slz * t1, x2 = dx + slx * t2, z2 = dz + slz * t2;
                    double w1 = w * (0.7 + 0.8 * t1), w2 = w * (0.7 + 0.8 * t2);
                    v(bb, x1 - p[0] * w1, y1, z1 - p[1] * w1, pal[0], pal[1], pal[2], a1);
                    v(bb, x1 + p[0] * w1, y1, z1 + p[1] * w1, pal[0], pal[1], pal[2], a1);
                    v(bb, x2 + p[0] * w2, y2, z2 + p[1] * w2, pal[0], pal[1], pal[2], a2);
                    v(bb, x2 - p[0] * w2, y2, z2 - p[1] * w2, pal[0], pal[1], pal[2], a2);
                }
                any = true;
            }
        }
        if (any) end(bb);
    }

    private static float beamA(double t) { return (float) Math.sin(Math.PI * Mth.clamp(t, 0, 1)); }

    private static int hash(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
}
