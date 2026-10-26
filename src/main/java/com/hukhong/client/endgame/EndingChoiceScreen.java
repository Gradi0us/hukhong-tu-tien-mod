package com.hukhong.client.endgame;

import com.hukhong.endgame.EndgameKit;
import com.hukhong.endgame.EndgameNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Chọn kết thúc (một lần, cá nhân): Phong Ấn hoặc Hấp Thụ. Không đóng được bằng ESC. */
public class EndingChoiceScreen extends Screen {
    public EndingChoiceScreen() { super(Component.translatable("hukhong.endgame.choice.title")); }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 + 10;
        addRenderableWidget(Button.builder(Component.translatable("hukhong.endgame.choice.seal"), b -> pick(EndgameKit.ENDING_SEAL))
                .bounds(cx - 155, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("hukhong.endgame.choice.absorb"), b -> pick(EndgameKit.ENDING_ABSORB))
                .bounds(cx + 5, y, 150, 20).build());
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
        g.drawWordWrap(font, Component.translatable("hukhong.endgame.choice.seal.desc"), cx - 155, ty, 150, 0xFFDDDDDD);
        g.drawWordWrap(font, Component.translatable("hukhong.endgame.choice.absorb.desc"), cx + 5, ty, 150, 0xFFDDDDDD);
    }
}
