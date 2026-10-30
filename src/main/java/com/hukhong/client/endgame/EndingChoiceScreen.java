package com.hukhong.client.endgame;

import com.hukhong.endgame.EndgameKit;
import com.hukhong.endgame.EndgameNet;
import com.hukhong.endgame.SieuDoKit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Chọn kết thúc (một lần, cá nhân): Phong Ấn, Hấp Thụ, hoặc Siêu Độ (mờ đi kèm gợi ý còn thiếu gì nếu chưa đủ điều kiện).
 * Số liệu do server gửi trong payload (client không đọc CULTIVATION). Không đóng được bằng ESC.
 */
public class EndingChoiceScreen extends Screen {
    private final EndgameNet.OpenChoice info;

    public EndingChoiceScreen(EndgameNet.OpenChoice info) {
        super(Component.translatable("hukhong.endgame.choice.title"));
        this.info = info;
    }

    /** Các dòng yêu cầu còn thiếu (tooltip + vẽ dưới nút). */
    List<Component> missingLines() {
        List<Component> l = new ArrayList<>();
        int m = info.missing();
        if ((m & SieuDoKit.M_DOCS) != 0) l.add(Component.translatable("hukhong.sieudo.need.docs", info.docs(), SieuDoKit.DOCS_NEEDED));
        if ((m & SieuDoKit.M_HU_ANH) != 0) l.add(Component.translatable("hukhong.sieudo.need.hu_anh", info.huAnh(), SieuDoKit.HU_ANH_BELOW));
        if ((m & SieuDoKit.M_CHG) != 0) l.add(Component.translatable("hukhong.sieudo.need.chg", info.chg(), SieuDoKit.SHADES));
        if ((m & SieuDoKit.M_ZONES) != 0) l.add(Component.translatable("hukhong.sieudo.need.zones", info.zones(), info.zonesNeeded()));
        return l;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 + 10;
        addRenderableWidget(Button.builder(Component.translatable("hukhong.endgame.choice.seal"), b -> pick(EndgameKit.ENDING_SEAL))
                .bounds(cx - 185, y, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("hukhong.endgame.choice.absorb"), b -> pick(EndgameKit.ENDING_ABSORB))
                .bounds(cx - 60, y, 120, 20).build());
        Button truth = Button.builder(Component.translatable("hukhong.endgame.choice.truth"), b -> pick(EndgameKit.ENDING_TRUTH))
                .bounds(cx + 65, y, 120, 20).build();
        if (info.missing() != 0) { // mờ, kèm tooltip yêu cầu còn thiếu
            truth.active = false;
            Component tip = Component.translatable("hukhong.sieudo.need.title");
            for (Component c : missingLines()) tip = tip.copy().append("\n").append(c);
            truth.setTooltip(Tooltip.create(tip));
        }
        addRenderableWidget(truth);
    }

    private void pick(int ending) {
        PacketDistributor.sendToServer(new EndgameNet.ChooseEnding(ending));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        super.render(g, mx, my, pt);
        int cx = width / 2, y = height / 2 - 70;
        g.drawCenteredString(font, title, cx, y, 0xFFE0C0FF);
        g.drawCenteredString(font, Component.translatable("hukhong.endgame.choice.once"), cx, y + 14, 0xFFAAAAAA);
        int ty = height / 2 + 40;
        g.drawWordWrap(font, Component.translatable("hukhong.endgame.choice.seal.desc"), cx - 185, ty, 120, 0xFFDDDDDD);
        g.drawWordWrap(font, Component.translatable("hukhong.endgame.choice.absorb.desc"), cx - 60, ty, 120, 0xFFDDDDDD);
        boolean ok = info.missing() == 0;
        g.drawWordWrap(font, Component.translatable("hukhong.endgame.choice.truth.desc"), cx + 65, ty, 120, ok ? 0xFFA8F0E0 : 0xFF777777);
        if (!ok) {
            int hy = ty + 70;
            for (Component c : missingLines()) { g.drawWordWrap(font, c, cx + 65, hy, 120, 0xFFFF8080); hy += 22; }
        }
    }
}
