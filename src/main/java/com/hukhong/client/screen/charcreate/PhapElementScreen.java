package com.hukhong.client.screen.charcreate;

import com.hukhong.charcreate.CharCreateService;
import com.hukhong.charcreate.CharRoll;
import com.hukhong.network.ConfirmCharCreatePayload;
import com.hukhong.phaptu.PhapElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Màn lộ nguyên tố Pháp Tu sau khi chọn đạo: nguyên tố xoay rồi dừng lại (kết quả xác định theo seed roll, server tính lại
 * giống hệt), người chơi có đúng 1 lần reroll. Xác nhận gửi mask đạo + cờ reroll.
 */
public class PhapElementScreen extends Screen {
    private static final int SPIN_TICKS = 44;

    private final CharRoll roll;
    private final int pathMask;
    private boolean rerolled;
    private int ticks;
    private Button confirm, reroll;
    private boolean landed;

    public PhapElementScreen(CharRoll roll, int pathMask) {
        super(Component.translatable("hukhong.phap.element.title"));
        this.roll = roll;
        this.pathMask = pathMask;
    }

    private PhapElement result() { return PhapElement.roll(roll.seed(), rerolled); }

    @Override
    protected void init() {
        confirm = addRenderableWidget(Button.builder(Component.translatable("hukhong.charcreate.confirm"), b -> {
            PacketDistributor.sendToServer(new ConfirmCharCreatePayload(pathMask | (rerolled ? CharCreateService.REROLL_BIT : 0)));
            onClose();
        }).bounds(width / 2 - 104, height / 2 + 60, 100, 20).build());
        reroll = addRenderableWidget(Button.builder(Component.translatable("hukhong.phap.element.reroll"), b -> {
            rerolled = true;
            ticks = 0;
            landed = false;
            b.active = false;
        }).bounds(width / 2 + 4, height / 2 + 60, 100, 20).build());
        confirm.active = false;
        reroll.active = false;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        ticks++;
        boolean done = ticks >= SPIN_TICKS;
        if (done && !landed) {
            landed = true;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.3f));
        } else if (!done && ticks % 4 == 0) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.6f));
        }
        confirm.active = done;
        reroll.active = done && !rerolled;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderTransparentBackground(g);
        float time = ticks + partial;
        int cx = width / 2, cy = height / 2 - 20;
        g.drawCenteredString(font, title, cx, cy - 60, 0xFFFFFFFF);
        PhapElement[] all = PhapElement.values();
        boolean done = ticks >= SPIN_TICKS;
        PhapElement shown = done ? result() : all[(int) (time / 2.5f) % all.length];
        int col = shown.argb();
        float pulse = done ? 0.6f + 0.4f * Mth.sin(time * 0.3f) : 0.35f;
        int r = done ? 46 : 40;
        g.fill(cx - r - 3, cy - r + 8, cx + r + 3, cy + r - 8, ((int) (200 * pulse) << 24) | shown.rgb);
        g.fill(cx - r, cy - r + 11, cx + r, cy + r - 11, 0xFF101018);
        g.pose().pushPose();
        g.pose().translate(cx, cy - 6, 0);
        g.pose().scale(done ? 3f : 2.4f, done ? 3f : 2.4f, 1f);
        g.drawCenteredString(font, Component.translatable(shown.langKey()), 0, 0, col);
        g.pose().popPose();
        g.drawCenteredString(font, Component.translatable(shown.langKey() + ".desc"), cx, cy + 22, done ? 0xFFCCCCCC : 0xFF666666);
        g.drawCenteredString(font, Component.translatable(rerolled ? "hukhong.phap.element.rerolled" : "hukhong.phap.element.reroll_left"),
                cx, cy + 36, 0xFFFFD34F);
        super.render(g, mx, my, partial);
    }
}
