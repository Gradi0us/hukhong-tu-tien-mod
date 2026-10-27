package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Hạt khí quyển tự quản (không qua ParticleEngine): billboard mềm cộng sáng (glow) hoặc pha trộn thường (tro), màu/cỡ/đời
 * tuỳ ý, trôi lang thang, hút về điểm (dòng linh khí), xoáy. Tối đa {@link #CAP}; tự giảm theo cấu hình hạt.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MoteSystem {
    private MoteSystem() {}

    public static final int CAP = 2600;
    public static final int GLOW = 0, ASH = 1;
    private static final ResourceLocation TEX_GLOW = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/ambient/mote.png");
    private static final ResourceLocation TEX_ASH = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/ambient/ash.png");

    public static final class Mote {
        public double x, y, z, px, py, pz, vx, vy, vz;
        public float r = 1, g = 1, b = 1, a = 1, size = 0.15f;
        public int age, life = 100, kind = GLOW, tag;
        public float phase, wander = 0.002f, flicker = 0.2f, drag = 0.96f, spin;
        public boolean hasTarget;
        public double tx, ty, tz;
        public float attract, swirl;

        public Mote at(double x, double y, double z) { this.x = px = x; this.y = py = y; this.z = pz = z; return this; }
        public Mote vel(double x, double y, double z) { vx = x; vy = y; vz = z; return this; }
        public Mote color(float r, float g, float b, float a) { this.r = r; this.g = g; this.b = b; this.a = a; return this; }
        public Mote size(float s) { size = s; return this; }
        public Mote life(int l) { life = l; return this; }
        public Mote kind(int k) { kind = k; return this; }
        public Mote drift(float wander, float drag) { this.wander = wander; this.drag = drag; return this; }
        public Mote flick(float f) { flicker = f; return this; }
        public Mote tag(int t) { tag = t; return this; }
        public Mote target(double x, double y, double z, float attract, float swirl) {
            hasTarget = true; tx = x; ty = y; tz = z; this.attract = attract; this.swirl = swirl; return this;
        }
    }

    private static final List<Mote> M = new ArrayList<>(CAP);

    public static void clear() { M.clear(); }
    public static int size() { return M.size(); }
    public static int count(int tag) { int c = 0; for (Mote m : M) if (m.tag == tag) c++; return c; }

    /** Hệ số mật độ theo cấu hình hạt (ALL=1, LOW=0.45, OFF=0). */
    public static float density() {
        return switch (ClientConfig.particles()) { case ALL -> 1f; case LOW -> 0.45f; case OFF -> 0f; };
    }

    public static Mote spawn(Mote m) {
        if (density() <= 0f || M.size() >= CAP) return m;
        m.phase = (float) (Math.random() * Math.PI * 2);
        m.spin = (float) (Math.random() * Math.PI * 2);
        M.add(m);
        return m;
    }

    /** Xác suất sinh hiệu chỉnh: rate là kỳ vọng mote/tick. */
    public static int roll(net.minecraft.util.RandomSource rnd, double rate) {
        rate *= density() * Math.max(0.05, ClientConfig.ambientIntensity());
        int n = (int) rate;
        if (rnd.nextDouble() < rate - n) n++;
        return n;
    }

    static void tick(Minecraft mc, ClientLevel lv) {
        if (M.isEmpty() || mc.player == null) return;
        double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();
        var rnd = lv.random;
        for (int i = M.size() - 1; i >= 0; i--) {
            Mote m = M.get(i);
            m.px = m.x; m.py = m.y; m.pz = m.z;
            if (++m.age >= m.life) { swapRemove(i); continue; }
            double dx0 = m.x - cx, dy0 = m.y - cy, dz0 = m.z - cz;
            if (dx0 * dx0 + dy0 * dy0 + dz0 * dz0 > 90 * 90) { swapRemove(i); continue; }
            m.vx += (rnd.nextDouble() - 0.5) * m.wander;
            m.vy += (rnd.nextDouble() - 0.5) * m.wander * 0.6;
            m.vz += (rnd.nextDouble() - 0.5) * m.wander;
            if (m.hasTarget) {
                double dx = m.tx - m.x, dy = m.ty - m.y, dz = m.tz - m.z;
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4;
                m.vx += dx / d * m.attract + (-dz / d) * m.swirl;
                m.vy += dy / d * m.attract;
                m.vz += dz / d * m.attract + (dx / d) * m.swirl;
                if (d < 0.8) m.age = Math.max(m.age, m.life - 6);
            }
            m.vx *= m.drag; m.vy *= m.drag; m.vz *= m.drag;
            m.x += m.vx; m.y += m.vy; m.z += m.vz;
        }
    }

    private static void swapRemove(int i) {
        int last = M.size() - 1;
        M.set(i, M.get(last));
        M.remove(last);
    }

    private static float alphaOf(Mote m, float pt) {
        float age = m.age + pt;
        float fin = Mth.clamp(age / Math.min(24f, m.life * 0.3f), 0f, 1f);
        float fout = Mth.clamp((m.life - age) / (m.life * 0.35f), 0f, 1f);
        float fl = 1f - m.flicker + m.flicker * (0.5f + 0.5f * Mth.sin(m.phase + age * 0.21f));
        float k = fin * fin * (3 - 2 * fin) * fout * fout * (3 - 2 * fout);
        return m.a * k * fl;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || M.isEmpty()) return;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        Vector3f left = cam.getLeftVector(), up = cam.getUpVector();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        pass(GLOW, TEX_GLOW, true, pt, cp, left, up);
        pass(ASH, TEX_ASH, false, pt, cp, left, up);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void pass(int kind, ResourceLocation tex, boolean additive, float pt, Vec3 cp, Vector3f left, Vector3f up) {
        if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bb = null;
        for (Mote m : M) {
            if (m.kind != kind) continue;
            float a = alphaOf(m, pt) * (kind == ASH ? 1f : 1f);
            if (a <= 0.003f) continue;
            if (bb == null) bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            double x = Mth.lerp(pt, m.px, m.x) - cp.x, y = Mth.lerp(pt, m.py, m.y) - cp.y, z = Mth.lerp(pt, m.pz, m.z) - cp.z;
            float s = m.size;
            float lx = left.x * s, ly = left.y * s, lz = left.z * s, ux = up.x * s, uy = up.y * s, uz = up.z * s;
            if (kind == ASH) { // xoay mảnh tro
                float ang = m.spin + (m.age + pt) * 0.05f, c = Mth.cos(ang), sn = Mth.sin(ang);
                float nlx = lx * c + ux * sn, nly = ly * c + uy * sn, nlz = lz * c + uz * sn;
                float nux = -lx * sn + ux * c, nuy = -ly * sn + uy * c, nuz = -lz * sn + uz * c;
                lx = nlx; ly = nly; lz = nlz; ux = nux; uy = nuy; uz = nuz;
            }
            bb.addVertex((float) (x + lx + ux), (float) (y + ly + uy), (float) (z + lz + uz)).setUv(0f, 0f).setColor(m.r, m.g, m.b, a);
            bb.addVertex((float) (x - lx + ux), (float) (y - ly + uy), (float) (z - lz + uz)).setUv(1f, 0f).setColor(m.r, m.g, m.b, a);
            bb.addVertex((float) (x - lx - ux), (float) (y - ly - uy), (float) (z - lz - uz)).setUv(1f, 1f).setColor(m.r, m.g, m.b, a);
            bb.addVertex((float) (x + lx - ux), (float) (y + ly - uy), (float) (z + lz - uz)).setUv(0f, 1f).setColor(m.r, m.g, m.b, a);
        }
        if (bb != null) {
            MeshData md = bb.build();
            if (md != null) BufferUploader.drawWithShader(md);
        }
    }
}
