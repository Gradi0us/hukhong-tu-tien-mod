package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** Ma Vực (R Ma Tu): vẽ đôi cánh quỷ (màng dơi đỏ thẫm, vỗ chậm) và cặp sừng cong trên đầu người đang mở lĩnh vực. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MaDomainClient {
    private MaDomainClient() {}

    private static final Map<Integer, Long> END = new HashMap<>();

    public static void set(int id, int ticks) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (ticks <= 0) END.remove(id);
        else END.put(id, mc.level.getGameTime() + ticks);
    }

    private static boolean on(Player p) {
        Long e = END.get(p.getId());
        return e != null && p.level().getGameTime() < e;
    }

    @SubscribeEvent
    public static void render(RenderPlayerEvent.Post e) {
        Player p = e.getEntity();
        if (!on(p) || p.isInvisible()) return;
        float pt = e.getPartialTick();
        float age = p.tickCount + pt;
        PoseStack ps = e.getPoseStack();
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.lightning());
        float bodyYaw = net.minecraft.util.Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
        float headYaw = net.minecraft.util.Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot);
        float headPitch = net.minecraft.util.Mth.lerp(pt, p.xRotO, p.getXRot());
        boolean crouch = p.isCrouching();

        // ---- cánh: gốc ở giữa lưng, mỗi bên 3 xương + màng
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180 - bodyYaw));
        ps.translate(0, crouch ? 1.15 : 1.3, 0.16);
        float flap = (float) Math.sin(age * 0.18) * 18f;
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(side * (25 + flap)));
            wing(ps.last().pose(), vc, side);
            ps.popPose();
        }
        ps.popPose();

        // ---- sừng: theo đầu
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180 - headYaw));
        ps.translate(0, crouch ? 1.5 : 1.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees(-headPitch));
        for (int side = -1; side <= 1; side += 2) horn(ps.last().pose(), vc, side);
        ps.popPose();
    }

    private static void quad(Matrix4f m, VertexConsumer vc, float[] a, float[] b, float[] c, float[] d, int argb0, int argb1) {
        v(m, vc, a, argb0); v(m, vc, b, argb0); v(m, vc, c, argb1); v(m, vc, d, argb1);
        v(m, vc, d, argb1); v(m, vc, c, argb1); v(m, vc, b, argb0); v(m, vc, a, argb0); // hai mặt
    }

    private static void v(Matrix4f m, VertexConsumer vc, float[] p, int argb) {
        vc.addVertex(m, p[0], p[1], p[2]).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24));
    }

    /** Cánh dơi: 3 ngón xương toả ra từ khớp vai, màng căng giữa các ngón (đỏ thẫm -> mép tím). */
    private static void wing(Matrix4f m, VertexConsumer vc, int side) {
        float s = side;
        float[] root = {0, 0, 0}, arm = {s * 0.55f, 0.45f, 0.25f};
        float[][] tips = {{s * 1.55f, 0.75f, 0.45f}, {s * 1.75f, 0.05f, 0.5f}, {s * 1.35f, -0.65f, 0.4f}, {s * 0.6f, -0.85f, 0.25f}};
        int mem0 = 0xC0700010, mem1 = 0x90400060, bone = 0xFF1A0004;
        quad(m, vc, root, arm, tips[0], tips[0], mem0, mem1);
        for (int i = 0; i < tips.length - 1; i++) {
            float[] mid = {(tips[i][0] + tips[i + 1][0]) * 0.42f, (tips[i][1] + tips[i + 1][1]) * 0.42f + 0.1f, (tips[i][2] + tips[i + 1][2]) * 0.45f};
            quad(m, vc, arm, tips[i], mid, tips[i + 1], mem0, mem1);
        }
        quad(m, vc, root, arm, tips[3], tips[3], mem0, mem1);
        for (float[] t : tips) bone(m, vc, arm, t, 0.035f, bone);
        bone(m, vc, root, arm, 0.05f, bone);
    }

    private static void bone(Matrix4f m, VertexConsumer vc, float[] a, float[] b, float w, int col) {
        quad(m, vc, new float[]{a[0], a[1] + w, a[2]}, new float[]{a[0], a[1] - w, a[2]}, new float[]{b[0], b[1] - w * 0.4f, b[2]}, new float[]{b[0], b[1] + w * 0.4f, b[2]}, col, col);
        quad(m, vc, new float[]{a[0], a[1], a[2] + w}, new float[]{a[0], a[1], a[2] - w}, new float[]{b[0], b[1], b[2] - w * 0.4f}, new float[]{b[0], b[1], b[2] + w * 0.4f}, col, col);
    }

    /** Sừng cong: 4 đoạn nón thu nhỏ, gốc đen -> chóp đỏ rực. */
    private static void horn(Matrix4f m, VertexConsumer vc, int side) {
        float s = side;
        float[][] pts = {{s * 0.2f, 0.42f, -0.05f}, {s * 0.32f, 0.62f, 0.02f}, {s * 0.38f, 0.82f, 0.14f}, {s * 0.34f, 0.98f, 0.3f}, {s * 0.24f, 1.06f, 0.44f}};
        float[] w = {0.09f, 0.07f, 0.05f, 0.03f, 0.005f};
        int[] col = {0xFF120004, 0xFF2A0006, 0xFF5A0A10, 0xFFA01020, 0xFFFF4030};
        for (int i = 0; i < pts.length - 1; i++) {
            float[] a = pts[i], b = pts[i + 1];
            for (int k = 0; k < 4; k++) {
                double a0 = Math.PI / 2 * k, a1 = Math.PI / 2 * (k + 1);
                float[] p0 = {a[0] + (float) Math.cos(a0) * w[i], a[1], a[2] + (float) Math.sin(a0) * w[i]};
                float[] p1 = {a[0] + (float) Math.cos(a1) * w[i], a[1], a[2] + (float) Math.sin(a1) * w[i]};
                float[] q1 = {b[0] + (float) Math.cos(a1) * w[i + 1], b[1], b[2] + (float) Math.sin(a1) * w[i + 1]};
                float[] q0 = {b[0] + (float) Math.cos(a0) * w[i + 1], b[1], b[2] + (float) Math.sin(a0) * w[i + 1]};
                quad(m, vc, p0, p1, q1, q0, col[i], col[i + 1]);
            }
        }
    }
}
