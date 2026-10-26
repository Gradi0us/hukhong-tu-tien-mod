package com.hukhong.client.story;

import com.hukhong.story.npc.NpcNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Hộp thoại NPC: các trang văn bản (bấm "Tiếp") rồi các nút lựa chọn. */
public class DialogueScreen extends Screen {
    private NpcNet.View view;
    private int page;
    private int bx, by, bw, bh;

    public DialogueScreen(NpcNet.View v) {
        super(Component.translatable(v.nameKey()));
        this.view = v;
    }

    public void show(NpcNet.View v) {
        this.view = v;
        this.page = 0;
        rebuildWidgets();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        bw = Math.min(width - 20, 340);
        bh = 100;
        bx = (width - bw) / 2;
        by = height - bh - 30;
        boolean last = page >= view.pages().size() - 1;
        if (!last) {
            addRenderableWidget(Button.builder(Component.translatable("hukhong.npc.next"), b -> { page++; rebuildWidgets(); })
                    .bounds(bx + bw - 70, by + bh - 22, 64, 18).build());
        } else if (view.choices().isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("hukhong.npc.close"), b -> send(-1))
                    .bounds(bx + bw - 70, by + bh - 22, 64, 18).build());
        } else {
            int n = view.choices().size(), y = by - n * 22;
            for (int i = 0; i < n; i++) {
                final int idx = i;
                addRenderableWidget(Button.builder(Component.translatable(view.choices().get(i)), b -> send(idx))
                        .bounds(bx, y + i * 22, bw, 20).build());
            }
        }
    }

    private void send(int idx) {
        PacketDistributor.sendToServer(new NpcNet.Choose(idx));
        if (idx < 0) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(bx, by, bx + bw, by + bh, 0xE0101018);
        g.renderOutline(bx, by, bw, bh, 0xFF6A4C9C);
        g.drawString(font, title, bx + 8, by + 6, 0xFFFFD34F, false);
        g.drawWordWrap(font, Component.translatable(view.pages().get(Math.min(page, view.pages().size() - 1))),
                bx + 8, by + 22, bw - 16, 0xFFEEEEEE);
    }
}
