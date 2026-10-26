package com.hukhong.client.endgame;

import com.hukhong.endgame.EndgameKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Cutscene đơn giản: khóa camera (màn hình chiếm input, server giữ người chơi bất tử + bất động), dải đen điện ảnh,
 * tiêu đề + 3 dòng thoại theo {@link EndgameKit} timeline, hạt do server phát hiện qua lớp phủ trong suốt.
 * ESC/Space/Enter = bỏ qua (sang credits ngay).
 */
public class CutsceneScreen extends Screen {
    private final int ending;
    private int tick;

    public CutsceneScreen(int ending) {
        super(Component.translatable("hukhong.endgame.cutscene." + key(ending) + ".title"));
        this.ending = ending;
    }

    private static String key(int e) { return e == EndgameKit.ENDING_SEAL ? "seal" : "absorb"; }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            toCredits();
            return true;
        }
        return true;
    }

    @Override
    public void tick() {
        if (++tick >= EndgameKit.CUTSCENE_TICKS) toCredits();
    }

    private void toCredits() { Minecraft.getInstance().setScreen(new CreditsScreen(ending)); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float t = tick + pt;
        int ti = (int) t;
        int a = (int) (EndgameKit.overlay(ti) * 255) & 0xFF;
        g.fill(0, 0, width, height, (a << 24) | (ending == EndgameKit.ENDING_SEAL ? 0x0A1018 : 0x14081C));
        int bar = (int) (Math.min(1f, t / EndgameKit.FADE_IN) * height * 0.12f);
        g.fill(0, 0, width, bar, 0xFF000000);
        g.fill(0, height - bar, width, height, 0xFF000000);
        int cx = width / 2;
        int tc = ending == EndgameKit.ENDING_SEAL ? 0xFFFFE9A0 : 0xFFC88CFF;
        if (ti < EndgameKit.FADE_IN + 20) {
            int ta = (int) (Math.min(1f, t / 20f) * 255);
            g.pose().pushPose();
            g.pose().translate(cx, height / 2 - 20, 0);
            g.pose().scale(2.5f, 2.5f, 1f);
            g.drawCenteredString(font, title, 0, 0, (ta << 24) | (tc & 0xFFFFFF));
            g.pose().popPose();
        }
        int line = EndgameKit.line(ti);
        if (line >= 0) {
            int la = (int) (EndgameKit.lineAlpha(ti) * 255) & 0xFF;
            if (la > 4) g.drawCenteredString(font, Component.translatable("hukhong.endgame.cutscene." + key(ending) + "." + (line + 1)),
                    cx, height - bar - 36, (la << 24) | 0xFFFFFF);
        }
        g.drawString(font, Component.translatable("hukhong.endgame.skip"), width - 8 - font.width(Component.translatable("hukhong.endgame.skip")),
                height - bar - 12, 0x80FFFFFF);
        // vòng sáng 2D quay quanh tâm màn hình
        for (int i = 0; i < 24; i++) {
            double ang = t * 0.05 + i * Math.PI / 12, r = 40 + (i % 4) * 14 + Math.sin(t * 0.08 + i) * 6;
            int px = (int) (cx + Math.cos(ang) * r), py = (int) (height / 2 + Math.sin(ang) * r * 0.6);
            g.fill(px, py, px + 2, py + 2, 0xB0000000 | (tc & 0xFFFFFF));
        }
    }
}
