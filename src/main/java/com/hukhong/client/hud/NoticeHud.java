package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * Thay layer title/subtitle và actionbar (overlay message) của vanilla bằng banner gọn kiểu tiên hiệp: tự xuống dòng
 * tối đa ~60% chiều rộng, chữ co 0.75-1.0, nền tối + viền vàng, fade theo thời gian của packet gốc. Mọi title/actionbar
 * server gửi (xâm lăng, thử luyện, ngũ suy, cốt truyện, đột phá, chung cuộc...) đều đi qua đây nên không tràn khỏi màn hình.
 * Tắt bằng config compactNotices -> trả về layer vanilla.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class NoticeHud {
    private NoticeHud() {}

    private static final int MAX_LINES = 3;

    /** Đọc trạng thái private của Gui bằng reflection (không dùng AT để khỏi phải dựng lại artifact NeoForge). Lỗi -> trả layer vanilla. */
    static final class Refl {
        private static final java.util.Map<String, java.lang.reflect.Field> F = new java.util.HashMap<>();
        private static final String[] NAMES = {"title", "subtitle", "titleTime", "titleFadeInTime", "titleStayTime",
                "titleFadeOutTime", "overlayMessageString", "overlayMessageTime", "animateOverlayMessageColor"};
        private static boolean ok;
        private static boolean init;

        static boolean ok() {
            if (!init) {
                init = true;
                try {
                    for (String n : NAMES) {
                        var f = Gui.class.getDeclaredField(n);
                        f.setAccessible(true);
                        F.put(n, f);
                    }
                    ok = true;
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    com.mojang.logging.LogUtils.getLogger().warn("NoticeHud: không đọc được trường Gui ({}), dùng title vanilla", ex.toString());
                }
            }
            return ok;
        }

        static Object get(Gui g, String n) {
            try { return F.get(n).get(g); } catch (ReflectiveOperationException | RuntimeException ex) { return null; }
        }
        static int i(Gui g, String n) { return get(g, n) instanceof Integer v ? v : 0; }
        static boolean b(Gui g, String n) { return get(g, n) instanceof Boolean v && v; }
    }

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.wrapLayer(VanillaGuiLayers.TITLE, orig -> (g, dt) -> {
            if (ClientConfig.compactNotices() && Refl.ok()) title(g, dt); else orig.render(g, dt);
        });
        e.wrapLayer(VanillaGuiLayers.OVERLAY_MESSAGE, orig -> (g, dt) -> {
            if (ClientConfig.compactNotices() && Refl.ok()) actionbar(g, dt); else orig.render(g, dt);
        });
    }

    // ---- title + subtitle ----

    private static void title(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        Gui gui = mc.gui;
        Component title = (Component) Refl.get(gui, "title"), sub = (Component) Refl.get(gui, "subtitle");
        int time = Refl.i(gui, "titleTime");
        if (title == null || time <= 0) return;
        float f = time - dt.getGameTimeDeltaPartialTick(false);
        int fin = Math.max(1, Refl.i(gui, "titleFadeInTime")), fout = Math.max(1, Refl.i(gui, "titleFadeOutTime")), stay = Refl.i(gui, "titleStayTime");
        float a = 255f;
        if (time > fout + stay) a = (fin + stay + fout - f) * 255f / fin;
        if (time <= fout) a = f * 255f / fout;
        int alpha = Mth.clamp((int) a, 0, 255);
        if (alpha <= 8) return;

        Font font = mc.font;
        int gw = g.guiWidth(), gh = g.guiHeight();
        int maxW = Mth.clamp((int) (gw * 0.6f), 120, 420);
        int pad = 6;
        float ts = 1.0f, ss = 0.75f;
        List<FormattedCharSequence> tl = font.split(title, (int) (maxW / ts));
        boolean hasSub = sub != null && !sub.getString().isEmpty();
        List<FormattedCharSequence> sl = hasSub ? font.split(sub, (int) (maxW / ss)) : List.of();
        if (tl.size() > 2) { ts = 0.85f; tl = font.split(title, (int) (maxW / ts)); }
        if (tl.size() > MAX_LINES) tl = tl.subList(0, MAX_LINES);
        if (sl.size() > MAX_LINES) sl = sl.subList(0, MAX_LINES);

        int lh = 9;
        float th = tl.size() * lh * ts, sh = sl.size() * lh * ss;
        float inner = th + (hasSub ? sh + 5 : 0);
        float tw = 0;
        for (var l : tl) tw = Math.max(tw, font.width(l) * ts);
        for (var l : sl) tw = Math.max(tw, font.width(l) * ss);
        int boxW = Math.min(maxW + 2 * pad, (int) Math.ceil(tw) + 2 * pad + 8);
        int boxH = (int) Math.ceil(inner) + 2 * pad;
        int bx = (gw - boxW) / 2;
        int by = Math.max(12, Math.min((int) (gh * 0.17f), gh - boxH - 12));

        frame(g, bx, by, boxW, boxH, alpha);
        float y = by + pad;
        int cx = gw / 2;
        for (var l : tl) {
            line(g, font, l, cx, y, ts, (alpha << 24) | 0xF6E7B4);
            y += lh * ts;
        }
        if (hasSub) {
            y += 1;
            int ca = (alpha * 150 / 255) << 24;
            g.fill(cx - 14, (int) y, cx + 14, (int) y + 1, ca | 0xC9A24A);
            y += 4;
            for (var l : sl) {
                line(g, font, l, cx, y, ss, (alpha << 24) | 0xD8D2C4);
                y += lh * ss;
            }
        }
    }

    // ---- actionbar ----

    private static void actionbar(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        Gui gui = mc.gui;
        Component msg = (Component) Refl.get(gui, "overlayMessageString");
        int msgTime = Refl.i(gui, "overlayMessageTime");
        if (msg == null || msgTime <= 0) return;
        float f = msgTime - dt.getGameTimeDeltaPartialTick(false);
        int alpha = Math.min(255, (int) (f * 255f / 20f));
        if (alpha <= 8) return;

        Font font = mc.font;
        int gw = g.guiWidth(), gh = g.guiHeight();
        int maxW = Mth.clamp((int) (gw * 0.6f), 140, 420);
        float sc = 1.0f;
        List<FormattedCharSequence> ls = font.split(msg, (int) (maxW / sc));
        if (ls.size() > 1) { sc = 0.85f; ls = font.split(msg, (int) (maxW / sc)); }
        if (ls.size() > MAX_LINES) ls = ls.subList(0, MAX_LINES);
        int lh = 9;
        float w = 0;
        for (var l : ls) w = Math.max(w, font.width(l) * sc);
        int boxW = (int) Math.ceil(w) + 10, boxH = (int) Math.ceil(ls.size() * lh * sc) + 6;
        // vanilla: đáy chữ ở gh - max(max(left,right)+9, 68); giữ nguyên để không đè thanh máu/thức ăn
        int yShift = Math.max(Math.max(gui.leftHeight, gui.rightHeight) + 9, 68);
        int bottom = gh - yShift + 4;
        int bx = (gw - boxW) / 2, by = bottom - boxH;
        g.fill(bx, by, bx + boxW, by + boxH, ((alpha * 130 / 255) << 24) | 0x0E0A14);
        int ga = (alpha * 200 / 255) << 24;
        g.fill(bx, by, bx + 1, by + boxH, ga | 0xC9A24A);
        g.fill(bx + boxW - 1, by, bx + boxW, by + boxH, ga | 0xC9A24A);

        int color = Refl.b(gui, "animateOverlayMessageColor") ? Mth.hsvToArgb(f / 50f, 0.7f, 0.6f, alpha) : (alpha << 24) | 0xFFFFFF;
        float y = by + 3;
        for (var l : ls) {
            line(g, font, l, gw / 2, y, sc, color);
            y += lh * sc;
        }
    }

    // ---- vẽ ----

    private static void line(GuiGraphics g, Font font, FormattedCharSequence l, int cx, float y, float sc, int argb) {
        g.pose().pushPose();
        g.pose().translate(cx - font.width(l) * sc / 2f, y, 0);
        g.pose().scale(sc, sc, 1f);
        g.drawString(font, l, 0, 0, argb, true);
        g.pose().popPose();
    }

    /** Nền tối + viền vàng 1px + 4 góc đậm (pixel-art). */
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        g.fill(x, y, x + w, y + h, ((alpha * 175 / 255) << 24) | 0x0E0A14);
        int b = (alpha * 220 / 255) << 24 | 0xC9A24A;
        g.fill(x, y, x + w, y + 1, b);
        g.fill(x, y + h - 1, x + w, y + h, b);
        g.fill(x, y, x + 1, y + h, b);
        g.fill(x + w - 1, y, x + w, y + h, b);
        int c = (alpha << 24) | 0xF6E7B4;
        g.fill(x - 1, y - 1, x + 2, y + 2, c);
        g.fill(x + w - 2, y - 1, x + w + 1, y + 2, c);
        g.fill(x - 1, y + h - 2, x + 2, y + h + 1, c);
        g.fill(x + w - 2, y + h - 2, x + w + 1, y + h + 1, c);
    }
}
