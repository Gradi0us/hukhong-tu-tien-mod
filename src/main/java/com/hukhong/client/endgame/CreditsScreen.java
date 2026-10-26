package com.hukhong.client.endgame;

import com.hukhong.endgame.EndgameKit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Credits cuộn dọc; ESC/Space/Enter hoặc cuộn hết = xong (báo server đưa về thế giới). */
public class CreditsScreen extends Screen {
    public static final int LINES = 12;
    private static final float SPEED = 0.6f;  // px / tick
    private final int ending;
    private float scroll;
    private boolean done;

    public CreditsScreen(int ending) {
        super(Component.translatable("hukhong.endgame.credits.title"));
        this.ending = ending;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            finish();
            return true;
        }
        return true;
    }

    private void finish() {
        if (done) return;
        done = true;
        EndgameClient.finish();
    }

    @Override
    public void tick() {
        scroll += SPEED;
        if (scroll > height + LINES * 22 + 60) finish();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF000000);
        float y0 = height - (scroll + pt * SPEED);
        int cx = width / 2;
        int tc = ending == EndgameKit.ENDING_SEAL ? 0xFFFFE9A0 : 0xFFC88CFF;
        g.drawCenteredString(font, title, cx, (int) y0, tc);
        for (int i = 1; i <= LINES; i++) {
            int y = (int) (y0 + 30 + (i - 1) * 22);
            if (y < -10 || y > height + 10) continue;
            g.drawCenteredString(font, Component.translatable("hukhong.endgame.credits." + i), cx, y, 0xFFE8E8E8);
        }
        g.drawString(font, Component.translatable("hukhong.endgame.skip"), width - 8 - font.width(Component.translatable("hukhong.endgame.skip")),
                height - 12, 0x80FFFFFF);
    }
}
