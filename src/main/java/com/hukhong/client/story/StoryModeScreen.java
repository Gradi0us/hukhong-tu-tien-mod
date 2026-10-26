package com.hukhong.client.story;

import com.hukhong.story.StoryData;
import com.hukhong.story.StoryNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Chọn cách chơi (một lần): Truyện hoặc Tự do. */
public class StoryModeScreen extends Screen {
    public StoryModeScreen() { super(Component.translatable("hukhong.story.mode.title")); }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 10;
        addRenderableWidget(Button.builder(Component.translatable("hukhong.story.mode.story"), b -> pick(StoryData.MODE_STORY))
                .bounds(cx - 155, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("hukhong.story.mode.free"), b -> pick(StoryData.MODE_FREE))
                .bounds(cx + 5, y, 150, 20).build());
    }

    private void pick(int mode) {
        PacketDistributor.sendToServer(new StoryNet.ChooseMode(mode));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int cx = width / 2, y = height / 2 - 70;
        g.drawCenteredString(font, title, cx, y, 0xFFFFD34F);
        g.drawCenteredString(font, Component.translatable("hukhong.story.mode.once"), cx, y + 14, 0xFFAAAAAA);
        int ty = height / 2 + 20;
        g.drawWordWrap(font, Component.translatable("hukhong.story.mode.story.desc"), cx - 155, ty, 150, 0xFFDDDDDD);
        g.drawWordWrap(font, Component.translatable("hukhong.story.mode.free.desc"), cx + 5, ty, 150, 0xFFDDDDDD);
    }
}
