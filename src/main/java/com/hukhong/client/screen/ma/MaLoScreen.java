package com.hukhong.client.screen.ma;

import com.hukhong.client.hud.ClientMaState;
import com.hukhong.network.SetMaLoPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Màn nhỏ cho Ma tu: chọn lộ Hấp Thu / Tu Luyện thường (S2 §2.4). Mở từ Hồ sơ (K). */
public class MaLoScreen extends Screen {
    private static final int W = 220, H = 118;
    private Button absorbBtn, normalBtn;

    public MaLoScreen() { super(Component.translatableWithFallback("hukhong.ma.lo.title", "Lộ Ma Tu")); }

    private static Component t(String key, String vi) { return Component.translatableWithFallback(key, vi); }

    @Override
    protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        absorbBtn = addRenderableWidget(Button.builder(t("hukhong.ma.lo.absorb", "Hấp Thu"), b -> choose(true))
                .bounds(x + 10, y + 56, 98, 20).build());
        normalBtn = addRenderableWidget(Button.builder(t("hukhong.ma.lo.normal", "Tu Luyện thường"), b -> choose(false))
                .bounds(x + 112, y + 56, 98, 20).build());
        addRenderableWidget(Button.builder(t("hukhong.profile.close", "Đóng"), b -> onClose())
                .bounds(x + W / 2 - 40, y + H - 26, 80, 18).build());
        refresh();
    }

    private void choose(boolean absorb) {
        PacketDistributor.sendToServer(new SetMaLoPayload(absorb));
        // lạc quan: server xác nhận lại qua MaSyncPayload; cập nhật nút ngay khi sync về
    }

    private void refresh() {
        boolean ab = ClientMaState.absorb();
        absorbBtn.active = !ab;
        normalBtn.active = ab;
    }

    @Override
    public void tick() { refresh(); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        int x = (width - W) / 2, y = (height - H) / 2;
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF662222);
        g.fill(x, y, x + W, y + H, 0xEE140A0F);
        g.drawCenteredString(font, title, width / 2, y + 6, 0xFFFF5555);
        Component cur = ClientMaState.absorb() ? t("hukhong.ma.lo.absorb", "Hấp Thu") : t("hukhong.ma.lo.normal", "Tu Luyện thường");
        g.drawCenteredString(font, t("hukhong.ma.lo.current", "Lộ hiện tại:").copy().append(" ").append(cur), width / 2, y + 22, 0xFFFFFFFF);
        int ly = y + 34;
        for (var line : font.split(t("hukhong.ma.lo.hint", "Hấp Thu: EXP x1.45 nhưng tích Ác Tính. Tu Luyện thường: không tích, xóa Huyết Tế."), W - 20)) {
            g.drawString(font, line, x + 10, ly, 0xFF999999);
            ly += 9;
        }
        super.render(g, mx, my, partial);
    }
}
