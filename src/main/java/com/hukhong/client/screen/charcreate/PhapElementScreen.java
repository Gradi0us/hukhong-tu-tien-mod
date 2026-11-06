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
 * Chọn nguyên tố Pháp Tu (2026-11-06): người chơi tự chọn 1 trong 6 nguyên tố (trước đây quay ngẫu nhiên + 1 lần đổi).
 * Xác nhận gửi mask đạo + nguyên tố đã chọn (CharCreateService.ELEMENT_SHIFT).
 */
public class PhapElementScreen extends Screen {
    private final CharRoll roll;
    private final int pathMask;
    private PhapElement picked;
    private Button confirm;
    private int ticks;

    public PhapElementScreen(CharRoll roll, int pathMask) {
        super(Component.translatable("hukhong.phap.element.pick_title"));
        this.roll = roll;
        this.pathMask = pathMask;
    }

    @Override
    protected void init() {
        PhapElement[] all = PhapElement.values();
        int bw = 96, bh = 22, gap = 6, cx = width / 2, top = height / 2 - 46;
        for (int i = 0; i < all.length; i++) {
            PhapElement e = all[i];
            int col = i % 3, row = i / 3;
            int x = cx - (3 * bw + 2 * gap) / 2 + col * (bw + gap), y = top + row * (bh + gap);
            addRenderableWidget(Button.builder(Component.translatable(e.langKey()).withColor(e.rgb), b -> {
                picked = e;
                confirm.active = true;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f));
            }).bounds(x, y, bw, bh).build());
        }
        confirm = addRenderableWidget(Button.builder(Component.translatable("hukhong.charcreate.confirm"), b -> {
            if (picked == null) return;
            PacketDistributor.sendToServer(new ConfirmCharCreatePayload(pathMask | ((picked.ordinal() + 1) << CharCreateService.ELEMENT_SHIFT)));
            onClose();
        }).bounds(cx - 50, height / 2 + 66, 100, 20).build());
        confirm.active = false;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() { ticks++; }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderTransparentBackground(g);
        int cx = width / 2, top = height / 2 - 46;
        g.drawCenteredString(font, title, cx, top - 26, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("hukhong.phap.element.pick_hint"), cx, top - 14, 0xFFAAAAAA);
        if (picked != null) {
            float pulse = 0.6f + 0.4f * Mth.sin((ticks + partial) * 0.3f);
            int y = top + 58;
            g.fill(cx - 150, y - 2, cx + 150, y + 44, ((int) (90 * pulse) << 24) | picked.rgb);
            g.fill(cx - 148, y, cx + 148, y + 42, 0xE0101018);
            g.pose().pushPose();
            g.pose().translate(cx, y + 5, 0);
            g.pose().scale(1.8f, 1.8f, 1f);
            g.drawCenteredString(font, Component.translatable(picked.langKey()), 0, 0, picked.argb());
            g.pose().popPose();
            g.drawCenteredString(font, Component.translatable(picked.langKey() + ".desc"), cx, y + 26, 0xFFCCCCCC);
        }
        super.render(g, mx, my, partial);
    }
}
