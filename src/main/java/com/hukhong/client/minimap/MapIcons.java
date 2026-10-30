package com.hukhong.client.minimap;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;

/** Biểu tượng pixel cho minimap/bản đồ lớn (không dùng texture ngoài, thiếu file vẫn hiển thị). */
final class MapIcons {
    private MapIcons() {}

    static final int INK = 0xFF0C0810, GOLD = 0xFFE8C060, GOLD_DK = 0xFF8A6A2C, JADE = 0xFF6FE0D0, JADE_DK = 0xFF1E5A66,
            RED = 0xFFFF5A4A, WHITE = 0xFFF4EEDC;

    /** Thành: nhà vuông 5x5 vàng (đã dựng) / viền rỗng mờ (vị trí xấp xỉ). */
    static void city(GuiGraphics g, int cx, int cy, boolean built, boolean hot) {
        int x = cx - 3, y = cy - 3;
        g.fill(x, y, x + 7, y + 7, INK);
        if (built) {
            g.fill(x + 1, y + 1, x + 6, y + 6, hot ? WHITE : GOLD);
            g.fill(x + 2, y + 3, x + 5, y + 6, GOLD_DK);
            g.fill(x + 3, y + 4, x + 4, y + 6, INK);
        } else {
            g.fill(x + 1, y + 1, x + 6, y + 6, 0xFF5A4A28);
            g.fill(x + 2, y + 2, x + 5, y + 5, INK);
        }
    }

    /** Tông: thoi ngọc bích (đã dựng) / thoi rỗng (xấp xỉ). */
    /** Khu Cho: mai sap soc do-trang tren cot go. */
    static void market(GuiGraphics g, int cx, int cy, boolean hot) {
        g.fill(cx - 4, cy - 3, cx + 5, cy, INK);
        g.fill(cx - 3, cy + 0, cx - 1, cy + 4, INK);
        g.fill(cx + 2, cy + 0, cx + 4, cy + 4, INK);
        int c = hot ? WHITE : 0xFFE8803A;
        g.fill(cx - 3, cy - 2, cx + 4, cy - 1, c);
        g.fill(cx - 3, cy - 1, cx + 4, cy, 0xFFF4EDE0);
        g.fill(cx - 2, cy + 0, cx - 1, cy + 3, 0xFF8A6A3A);
        g.fill(cx + 3, cy + 0, cx + 4, cy + 3, 0xFF8A6A3A);
    }

    /** Địa điểm đã khám phá: sao bốn cánh xanh lá (đã dọn: xám xanh). */
    static void poi(GuiGraphics g, int cx, int cy, boolean cleared, boolean hot) {
        int c = hot ? WHITE : cleared ? 0xFF7FA08A : 0xFFB6F08C;
        g.fill(cx - 1, cy - 4, cx + 2, cy + 5, INK);
        g.fill(cx - 4, cy - 1, cx + 5, cy + 2, INK);
        g.fill(cx, cy - 3, cx + 1, cy + 4, c);
        g.fill(cx - 3, cy, cx + 4, cy + 1, c);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, c);
    }

    /** Hiệp Hội Mạo Hiểm Giả: khiên tím viền vàng. */
    static void guild(GuiGraphics g, int cx, int cy, boolean hot) {
        g.fill(cx - 3, cy - 4, cx + 4, cy + 1, INK);
        g.fill(cx - 2, cy + 1, cx + 3, cy + 3, INK);
        g.fill(cx - 1, cy + 3, cx + 2, cy + 4, INK);
        int c = hot ? WHITE : 0xFFB070F0;
        g.fill(cx - 2, cy - 3, cx + 3, cy + 1, c);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 3, c);
        g.fill(cx, cy - 3, cx + 1, cy + 2, GOLD);
        g.fill(cx - 2, cy - 1, cx + 3, cy, GOLD);
    }

    /** Vùng xâm thực: hình thoi đỏ tím có tâm đen (đã giải phóng: xanh lá). */
    static void zone(GuiGraphics g, int cx, int cy, boolean liberated, boolean hot) {
        int c = hot ? WHITE : liberated ? 0xFF8CE08C : 0xFFD0405A;
        for (int i = -4; i <= 4; i++) { int w = 4 - Math.abs(i); g.fill(cx - w - 1, cy + i, cx + w + 2, cy + i + 1, INK); }
        for (int i = -3; i <= 3; i++) { int w = 3 - Math.abs(i); g.fill(cx - w, cy + i, cx + w + 1, cy + i + 1, c); }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, INK);
    }

    /** Cảng: hình thoi xanh dương có neo nhỏ. */
    static void port(GuiGraphics g, int cx, int cy, boolean built, boolean hot) {
        int c = built ? 0xFF6AB8FF : 0xFF3A6A94;
        for (int i = -4; i <= 4; i++) { int w = 4 - Math.abs(i); g.fill(cx - w - 1, cy + i - 1, cx + w + 2, cy + i + 2, INK); }
        for (int i = -3; i <= 3; i++) { int w = 3 - Math.abs(i); g.fill(cx - w, cy + i, cx + w + 1, cy + i + 1, hot ? WHITE : c); }
        g.fill(cx, cy - 2, cx + 1, cy + 3, INK);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 2, INK);
    }

    static void sect(GuiGraphics g, int cx, int cy, boolean built, boolean hot) {
        int fill = built ? (hot ? WHITE : JADE) : 0xFF2E5C66;
        for (int dy = -3; dy <= 3; dy++) {
            int hw = 3 - Math.abs(dy);
            g.fill(cx - hw - 1, cy + dy, cx + hw + 2, cy + dy + 1, INK);
        }
        for (int dy = -2; dy <= 2; dy++) {
            int hw = 2 - Math.abs(dy);
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, fill);
        }
        if (built) g.fill(cx, cy, cx + 1, cy + 1, JADE_DK);
        else g.fill(cx, cy, cx + 1, cy + 1, INK);
    }

    /** Mũi tên chỉ lên, xoay angle độ (cùng chiều kim đồng hồ). */
    static void arrow(GuiGraphics g, int cx, int cy, float angle, int color, int outline) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        for (int i = 0; i <= 3; i++) g.fill(-i - 1, -4 + i - 1, i + 2, -4 + i + 1, outline); // viền đầu mũi
        g.fill(-2, 0, 3, 3, outline);
        for (int i = 0; i <= 3; i++) g.fill(-i, -4 + i, i + 1, -3 + i, color);
        g.fill(-1, 0, 2, 2, color);
        g.pose().popPose();
    }

    /** Mục tiêu chính: kim cương vàng nhấp nháy. */
    static void objective(GuiGraphics g, int cx, int cy, long tick) {
        int r = 4 + (int) ((tick / 8) % 2);
        for (int i = -r; i <= r; i++) {
            int w = r - Math.abs(i);
            g.fill(cx - w - 1, cy + i, cx + w + 2, cy + i + 1, INK);
        }
        for (int i = -r + 1; i <= r - 1; i++) {
            int w = r - 1 - Math.abs(i);
            g.fill(cx - w, cy + i, cx + w + 1, cy + i + 1, GOLD);
        }
        g.fill(cx, cy, cx + 1, cy + 1, WHITE);
    }

    /** Điểm đích: vòng đỏ nhấp nháy + chấm trắng. */
    static void waypoint(GuiGraphics g, int cx, int cy, long tick) {
        int r = 3 + (int) ((tick / 6) % 3);
        outline(g, cx, cy, r, 0xCCFF5A4A);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, INK);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, RED);
        g.fill(cx, cy, cx + 1, cy + 1, WHITE);
    }

    static void fillCircle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy) + 0.5);
            g.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, color);
        }
    }

    static void outline(GuiGraphics g, int cx, int cy, int r, int color) {
        int n = Math.max(12, r * 6);
        int lx = Integer.MIN_VALUE, ly = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            if (x == lx && y == ly) continue;
            lx = x; ly = y;
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** Quái thù địch: chấm đỏ 3x3 viền tối. */
    static void mobDot(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, INK);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, RED);
    }

    /** Boss: đầu lâu 9x9 (trắng, hốc mắt tối) viền đỏ. */
    static void bossSkull(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 5, cy - 5, cx + 6, cy + 6, INK);
        g.fill(cx - 4, cy - 4, cx + 5, cy + 4, RED);
        g.fill(cx - 3, cy - 3, cx + 4, cy + 2, WHITE);
        g.fill(cx - 2, cy + 2, cx + 3, cy + 4, WHITE);
        g.fill(cx - 2, cy - 1, cx, cy + 1, INK);
        g.fill(cx + 1, cy - 1, cx + 3, cy + 1, INK);
        g.fill(cx, cy + 1, cx + 1, cy + 2, INK);
    }
}
