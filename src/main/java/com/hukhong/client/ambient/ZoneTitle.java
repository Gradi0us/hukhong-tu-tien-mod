package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.network.MapMarkersPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.HashMap;
import java.util.Map;

/** Thẻ tên khu vực khi bước vào thành/cảng/tông/hiệp hội: mờ vào – giữ – mờ ra, chữ giãn cách + vạch sáng; mỗi khu có thời gian hồi. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ZoneTitle {
    private ZoneTitle() {}

    private static final int FADE_IN = 30, HOLD = 86, FADE_OUT = 34, TOTAL = FADE_IN + HOLD + FADE_OUT;
    private static final long COOLDOWN = 20 * 60 * 4L;
    private static final Map<String, Long> LAST = new HashMap<>();
    private static boolean wasInside;
    private static int age = -1, kind;
    private static Component name, sub;
    private static int color;

    static void tick(Minecraft mc, MapMarkersPayload.Marker zone, int zoneKind, long now) {
        boolean inside = zone != null && AmbientClient.rawPeace() > 0.82f;
        if (inside && !wasInside && ClientConfig.zoneTitles() && !AmbientClient.inBiCanh()) {
            String key = zone.kind() + zone.id();
            Long last = LAST.get(key);
            if (last == null || now - last > COOLDOWN) {
                LAST.put(key, now);
                age = 0;
                kind = zoneKind;
                name = Component.translatable(zone.nameKey());
                sub = Component.translatable("hukhong.amb.zone." + switch (zoneKind) {
                    case AmbientClient.Z_PORT -> "port";
                    case AmbientClient.Z_SECT -> "sect";
                    case AmbientClient.Z_GUILD -> "guild";
                    default -> "city";
                });
                color = switch (zoneKind) {
                    case AmbientClient.Z_PORT -> 0x7CC4FF;
                    case AmbientClient.Z_SECT -> 0x7FE8D6;
                    case AmbientClient.Z_GUILD -> 0xC79BFF;
                    default -> 0xEBC970;
                };
            }
        }
        wasInside = inside;
        if (age >= 0 && ++age > TOTAL) age = -1;
    }

    private static float smooth(float x) { x = Mth.clamp(x, 0f, 1f); return x * x * (3 - 2 * x); }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post e) {
        if (age < 0 || name == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        float t = age + e.getPartialTick().getGameTimeDeltaPartialTick(false);
        float a = t < FADE_IN ? smooth(t / FADE_IN) : t < FADE_IN + HOLD ? 1f : 1f - smooth((t - FADE_IN - HOLD) / FADE_OUT);
        if (a < 0.03f) return;
        GuiGraphics g = e.getGuiGraphics();
        Font font = mc.font;
        int W = g.guiWidth(), H = g.guiHeight();
        float drift = (1f - smooth(t / (FADE_IN + 20f))) * 6f;
        float cy = H * 0.24f + drift;

        // tên: chữ giãn cách
        String s = name.getString();
        float scale = 2.6f;
        int spacing = 3;
        int total = 0;
        for (int i = 0; i < s.length(); i++) total += font.width(String.valueOf(s.charAt(i))) + spacing;
        total -= spacing;
        float x = W / 2f - total * scale / 2f;
        g.pose().pushPose();
        g.pose().translate(x, cy, 0);
        g.pose().scale(scale, scale, 1f);
        int cx = 0;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            float lag = smooth((t - i * 1.6f) / FADE_IN); // chữ hiện lần lượt
            float ca = a * lag;
            if (ca >= 0.03f) {
                int alpha = Mth.clamp((int) (ca * 255), 4, 255);
                g.drawString(font, ch, cx, 0, (alpha << 24) | color, false);
                int sh = Mth.clamp((int) (ca * 120), 4, 255);
                g.drawString(font, ch, cx + 1, 1, (sh << 24) | 0x101018, false);
            }
            cx += font.width(ch) + spacing;
        }
        g.pose().popPose();

        // vạch sáng nở từ giữa
        float grow = smooth(t / (FADE_IN + 14f));
        int halfW = (int) (total * scale * 0.5f * (0.9f + 0.4f * grow));
        int ly = (int) (cy + font.lineHeight * scale + 5);
        int cols = Math.max(1, halfW * 2);
        for (int i = 0; i < cols; i += 2) {
            float u = (i / (float) cols) * 2f - 1f;
            float fall = 1f - Math.abs(u);
            int al = Mth.clamp((int) (a * 190 * fall * fall), 0, 255);
            if (al < 4) continue;
            g.fill(W / 2 - halfW + i, ly, W / 2 - halfW + i + 2, ly + 1, (al << 24) | color);
        }

        // phụ đề
        float sa = a * smooth((t - 10f) / FADE_IN);
        if (sa >= 0.03f) {
            String ss = sub.getString().toUpperCase(java.util.Locale.ROOT);
            int sw = 0;
            for (int i = 0; i < ss.length(); i++) sw += font.width(String.valueOf(ss.charAt(i))) + 2;
            int sx = W / 2 - sw / 2, sy = ly + 8;
            int alpha = Mth.clamp((int) (sa * 230), 4, 255);
            for (int i = 0; i < ss.length(); i++) {
                String ch = String.valueOf(ss.charAt(i));
                g.drawString(font, ch, sx, sy, (alpha << 24) | 0xD8D8E4, false);
                sx += font.width(ch) + 2;
            }
        }
    }
}
