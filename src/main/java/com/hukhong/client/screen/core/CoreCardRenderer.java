package com.hukhong.client.screen.core;

import com.hukhong.HuKhong;
import com.hukhong.cores.CoreDef;
import com.hukhong.cores.CoreRegistry;
import com.hukhong.cores.CoreTier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Vẽ thẻ Lõi (48x64 phóng 1.5x) và mô tả bị động — dùng chung cho Ấn Hồn Đài và Bảng tu sĩ. */
public final class CoreCardRenderer {
    private CoreCardRenderer() {}

    public static final int CW = 72, CH = 96;
    private static final ResourceLocation BACK = tex("card_back");

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/gui/core/" + n + ".png");
    }

    public static ResourceLocation frame(CoreTier t) { return tex("card_frame_" + t.id); }

    public static CoreDef def(ResourceLocation id) {
        var level = Minecraft.getInstance().level;
        return level == null ? null : CoreRegistry.get(level.registryAccess(), id);
    }

    public static int textColor(CoreTier t) {
        if (t.rainbow()) return rainbowColor(t, (Util.getMillis() % 100000L) / 100000.0 * speed(t));
        return 0xFF000000 | (t == CoreTier.HAC ? 0xA0A0B8 : t.rgb);
    }

    /** Màu RGB (không alpha) dùng cho vầng sáng chọn thẻ. */
    public static int glowRgb(CoreTier t) { return textColor(t) & 0xFFFFFF; }

    // ---- phẩm cầu vồng: Thất Thải (7 màu, chậm) và Cửu Thải (9 màu, nhanh, thêm viền trong + lấp lánh)
    private static final int[] PALETTE7 = {0xFF3B3B, 0xFF9A2E, 0xFFE83B, 0x45E05A, 0x3BD7FF, 0x4A6BFF, 0xB45CFF};
    private static final int[] PALETTE9 = {0xFF3B3B, 0xFF9A2E, 0xFFE83B, 0xA6F03B, 0x45E05A, 0x3BD7FF, 0x4A6BFF, 0xB45CFF, 0xFF6EC7};

    /** Số chu kỳ màu mỗi 100 giây: Thất Thải ~ 1 vòng / 4s, Cửu Thải ~ 1 vòng / 2s. */
    private static double speed(CoreTier t) { return t == CoreTier.CUU_THAI ? 50 : 25; }

    /** Màu ARGB tại pha phase (vòng lặp 0..1) trên bảng màu của phẩm, nội suy mượt giữa hai màu kề nhau. */
    public static int rainbowColor(CoreTier t, double phase) {
        int[] pal = t == CoreTier.CUU_THAI ? PALETTE9 : PALETTE7;
        double f = (phase - Math.floor(phase)) * pal.length;
        int i = (int) f, j = (i + 1) % pal.length;
        float u = (float) (f - i);
        int a = pal[i], b = pal[j];
        int r = (int) (((a >> 16) & 255) * (1 - u) + ((b >> 16) & 255) * u);
        int g = (int) (((a >> 8) & 255) * (1 - u) + ((b >> 8) & 255) * u);
        int bl = (int) ((a & 255) * (1 - u) + (b & 255) * u);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Viền cầu vồng chạy quanh thẻ (thay cho texture khung) + vầng sáng nhịp thở; Cửu Thải thêm viền trong ngược chiều. */
    private static void drawRainbowFrame(GuiGraphics g, CoreTier t) {
        double time = (Util.getMillis() % 100000L) / 100000.0 * speed(t);
        float pulse = 0.5f + 0.5f * (float) Math.sin(Util.getMillis() / 300.0);
        int halo = (rainbowColor(t, time) & 0xFFFFFF) | ((int) (40 + 60 * pulse) << 24);
        g.fill(-3, -3, CW + 3, CH + 3, halo);
        g.fill(-1, -1, CW + 1, CH + 1, (rainbowColor(t, time + 0.5) & 0xFFFFFF) | 0x66000000);
        strip(g, t, time, 0, 3, 1.0 / (2 * (CW + CH)));
        if (t == CoreTier.CUU_THAI) {
            strip(g, t, -time * 1.3 + 0.37, 4, 1, 1.0 / (2 * (CW + CH)));
            long ms = Util.getMillis();
            for (int k = 0; k < 3; k++) { // lấp lánh trắng chạy trên viền
                double pos = ((ms / (1400.0 + 330 * k)) + k * 0.31) % 1.0;
                int[] xy = perimeter(pos * 2 * (CW + CH));
                g.fill(xy[0] - 1, xy[1] - 1, xy[0] + 2, xy[1] + 2, 0xCCFFFFFF);
            }
        }
    }

    private static void strip(GuiGraphics g, CoreTier t, double phase0, int inset, int thick, double step) {
        int w = CW - 2 * inset, h = CH - 2 * inset, total = 2 * (w + h);
        for (int d = 0; d < total; d++) {
            int c = rainbowColor(t, phase0 + d * step * (2 * (CW + CH)) / total);
            int x, y, dx, dy;
            if (d < w) { x = d; y = 0; dx = 1; dy = thick; }
            else if (d < w + h) { x = w - thick; y = d - w; dx = thick; dy = 1; }
            else if (d < 2 * w + h) { x = w - (d - w - h) - 1; y = h - thick; dx = 1; dy = thick; }
            else { x = 0; y = h - (d - 2 * w - h) - 1; dx = thick; dy = 1; }
            g.fill(inset + x, inset + y, inset + x + dx, inset + y + dy, c);
        }
    }

    private static int[] perimeter(double d) {
        int total = 2 * (CW + CH);
        d = ((d % total) + total) % total;
        if (d < CW) return new int[]{(int) d, 1};
        d -= CW;
        if (d < CH) return new int[]{CW - 2, (int) d};
        d -= CH;
        if (d < CW) return new int[]{CW - 1 - (int) d, CH - 2};
        d -= CW;
        return new int[]{1, CH - 1 - (int) d};
    }

    /** Vẽ một mặt thẻ ở (0,0) hệ toạ độ hiện tại. */
    public static void drawFace(GuiGraphics g, Font font, boolean back, ResourceLocation core, CoreTier tier) {
        if (back) {
            g.blit(BACK, 0, 0, CW, CH, 0, 0, 48, 64, 48, 64);
            g.pose().pushPose();
            g.pose().translate(CW / 2f, CH / 2f - 6, 0);
            g.pose().scale(3f, 3f, 1f);
            g.drawCenteredString(font, "?", 0, 0, 0xFFAAAAAA);
            g.pose().popPose();
            return;
        }
        g.fill(3, 3, CW - 3, CH - 3, 0xFF14121C);
        if (tier.rainbow()) drawRainbowFrame(g, tier);
        else g.blit(frame(tier), 0, 0, CW, CH, 0, 0, 48, 64, 48, 64);
        if (core != null) {
            CoreDef d = def(core);
            Component name = Component.translatable(d != null ? d.nameKey() : "core.hukhong.unknown");
            g.pose().pushPose();
            g.pose().translate(CW / 2f, 22, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            int y = 0;
            for (var line : font.split(name, 80)) {
                g.drawString(font, line, -font.width(line) / 2, y, 0xFFFFFFFF, true);
                y += 10;
            }
            g.pose().popPose();
            if (d != null) {
                g.pose().pushPose();
                g.pose().translate(CW / 2f, CH - 30, 0);
                g.pose().scale(0.65f, 0.65f, 1f);
                Component sys = Component.translatable("hukhong.system." + d.system());
                g.drawString(font, sys, -font.width(sys) / 2, 0, 0xFFBBBBBB, false);
                g.pose().popPose();
            }
        }
        g.pose().pushPose();
        g.pose().translate(CW / 2f, CH - 18, 0);
        g.pose().scale(0.9f, 0.9f, 1f);
        Component tn = Component.translatable(tier.langKey());
        g.drawString(font, tn, -font.width(tn) / 2, 0, textColor(tier), true);
        g.pose().popPose();
    }

    /** Dòng mô tả bị động của Lõi ở phẩm cho trước. */
    public static List<Component> describe(ResourceLocation core, CoreTier tier) {
        List<Component> out = new ArrayList<>();
        CoreDef d = def(core);
        if (d == null) return out;
        for (CoreDef.Passive p : d.passives()) {
            double v = p.valueAt(tier);
            String num = p.operation() == AttributeModifier.Operation.ADD_VALUE
                    ? sign(v) + trim(v) : sign(v) + trim(v * 100) + "%";
            MutableComponent attr = BuiltInRegistries.ATTRIBUTE.getOptional(p.attribute())
                    .map(a -> Component.translatable(a.getDescriptionId()))
                    .orElse(Component.literal(p.attribute().getPath()));
            out.add(Component.literal(num + " ").append(attr));
        }
        return out;
    }

    private static String sign(double v) { return v >= 0 ? "+" : ""; }

    private static String trim(double v) {
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
