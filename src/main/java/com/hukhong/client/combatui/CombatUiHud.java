package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.client.compat.ShaderCompat;
import com.hukhong.entity.HuMob;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Đường vẽ dự phòng khi Iris đang dùng shaderpack: chiếu vị trí 3D ra màn hình (ma trận ghi lại ở CombatUiClient.onRender)
 * rồi vẽ thanh máu + số nổi bằng GuiGraphics 2D (lớp HUD không bị pipeline shader can thiệp).
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CombatUiHud {
    private CombatUiHud() {}

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post e) {
        if (!ShaderCompat.shadersActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = e.getGuiGraphics();
        Font font = mc.font;
        double now = CombatUiClient.HUD_NOW;
        if (ClientConfig.mobBars()) CombatUiClient.forEachBar(mc, now, (le, alpha) -> bar(g, font, le, alpha));
        if (ClientConfig.damageNumbers()) numbers(g, font, now);
    }

    /** Thế giới -> toạ độ GUI; null nếu sau lưng camera. [x, y, khoảng cách] */
    static float[] project(Vec3 w) {
        Vec3 c = CombatUiClient.HUD_CAM;
        Vector3f v = new Vector3f((float) (w.x - c.x), (float) (w.y - c.y), (float) (w.z - c.z));
        float dist = v.length();
        CombatUiClient.HUD_ROT.transform(v);
        Vector4f clip = new Vector4f(v, 1f);
        CombatUiClient.HUD_PROJ.transform(clip);
        if (clip.w <= 0.05f) return null;
        float nx = clip.x / clip.w, ny = clip.y / clip.w;
        if (Math.abs(nx) > 1.2f || Math.abs(ny) > 1.2f) return null;
        Minecraft mc = Minecraft.getInstance();
        float gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
        return new float[]{(nx * 0.5f + 0.5f) * gw, (1f - (ny * 0.5f + 0.5f)) * gh, dist};
    }

    private static void bar(GuiGraphics g, Font font, LivingEntity le, float alpha) {
        Vec3 p = le.getPosition(CombatUiClient.HUD_PT).add(0, le.getBbHeight() + 0.55, 0);
        float[] s = project(p);
        if (s == null) return;
        float sc = Mth.clamp(10f / Math.max(1f, s[2]), 0.45f, 1.0f);
        float max = Math.max(1f, le.getMaxHealth()), hp = Mth.clamp(le.getHealth(), 0f, max), ratio = hp / max;
        int a = Math.max(4, (int) (alpha * 255));
        int w = 44, h = 5, x0 = -w / 2;
        var ps = g.pose();
        ps.pushPose();
        ps.translate(s[0], s[1], 0);
        ps.scale(sc, sc, 1f);
        g.fill(x0 - 1, -1, x0 + w + 1, h + 1, (a * 190 / 255) << 24);
        g.fill(x0, 0, x0 + w, h, (a * 200 / 255) << 24 | 0x303030);
        int fillCol = ratio > 0.5f ? 0x4CD964 : ratio > 0.25f ? 0xF2C94C : 0xEB5757;
        g.fill(x0, 0, x0 + Math.round(w * ratio), h, a << 24 | fillCol);
        String hpTxt = CombatUiClient.fmt(hp) + "/" + CombatUiClient.fmt(max);
        String txt = le instanceof HuMob hm ? "Lv" + hm.huLevel() + "  " + hpTxt : hpTxt;
        g.drawString(font, txt, -font.width(txt) / 2, -10, a << 24 | 0xFFFFFF, true);
        ps.popPose();
    }

    private static void numbers(GuiGraphics g, Font font, double now) {
        for (CombatUiClient.Num n : CombatUiClient.nums()) {
            double age = Math.max(0, now - n.born);
            if (age >= CombatUiClient.numLife()) continue;
            float t = (float) (age / CombatUiClient.numLife());
            float rise = (float) (1.1 * (1 - Math.pow(1 - t, 2)));
            float alpha = t < 0.65f ? 1f : 1f - (t - 0.65f) / 0.35f;
            float[] s = project(new Vec3(n.x + n.drift * age, n.y + rise, n.z));
            if (s == null) continue;
            boolean crit = n.kind == CombatTextPayload.CRIT;
            float big = 1f + Math.min(0.45f, (float) Math.log10(1.0 + n.amount) / 9f);
            float base = big * (crit ? 2.0f : n.kind == CombatTextPayload.SKILL ? 1.3f : 1.0f);
            float pop = t < 0.12f ? 1f + (0.12f - t) * (crit ? 9f : 4f) : 1f;
            float sc = base * pop * Mth.clamp(9f / Math.max(1f, s[2]), 0.5f, 1.2f);
            int rgb = switch (n.kind) {
                case CombatTextPayload.CRIT -> 0xFFD23C;
                case CombatTextPayload.HEAL -> 0x5CE06A;
                case CombatTextPayload.SKILL -> n.color != 0 ? (n.color & 0xFFFFFF) : 0xB8A0FF;
                default -> 0xFFFFFF;
            };
            String txt = (n.kind == CombatTextPayload.HEAL ? "+" : "") + CombatUiClient.fmt(n.amount) + (crit ? "!" : "");
            int a = Math.max(4, (int) (alpha * 255));
            var ps = g.pose();
            ps.pushPose();
            ps.translate(s[0], s[1], 0);
            ps.scale(sc, sc, 1f);
            g.drawString(font, txt, -font.width(txt) / 2, 0, a << 24 | rgb, true);
            ps.popPose();
        }
    }
}
