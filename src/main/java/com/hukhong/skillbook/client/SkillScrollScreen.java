package com.hukhong.skillbook.client;

import com.hukhong.skillbook.ApplyScrollPayload;
import com.hukhong.skillbook.ClientSkillBook;
import com.hukhong.skillbook.SkillBook;
import com.hukhong.skillbook.SkillBookMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Menu don gian chon ky nang de dung Truc Nang Cap Ky Nang (server gui danh sach, server xac thuc khi ap dung). */
public final class SkillScrollScreen extends Screen {
    private static final int PER_PAGE = 8;
    private List<String> ids;
    private int page;

    private SkillScrollScreen(List<String> ids) {
        super(Component.translatable("hukhong.skillbook.upgrade_scroll.title"));
        this.ids = ids;
    }

    public static void open(List<String> ids) { Minecraft.getInstance().setScreen(new SkillScrollScreen(ids)); }

    @Override
    protected void init() {
        int w = 200, x = (width - w) / 2, y0 = height / 2 - 70;
        int from = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && from + i < ids.size(); i++) {
            String id = ids.get(from + i);
            int lv = ClientSkillBook.get().level(id);
            Component label = Component.empty().append(SkillBook.nameOf(id)).append(Component.literal("  Lv." + lv + " -> " + (lv + 1)));
            addRenderableWidget(Button.builder(label, b -> {
                PacketDistributor.sendToServer(new ApplyScrollPayload(id));
                onClose();
            }).bounds(x, y0 + i * 20, w, 18).build());
        }
        int pages = (ids.size() + PER_PAGE - 1) / PER_PAGE;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); rebuildWidgets(); })
                    .bounds(x, y0 + PER_PAGE * 20 + 4, 40, 18).build()).active = page > 0;
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(pages - 1, page + 1); rebuildWidgets(); })
                    .bounds(x + w - 40, y0 + PER_PAGE * 20 + 4, 40, 18).build()).active = page < pages - 1;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt); // tự vẽ nền mờ 1 lần; chữ vẽ sau để không bị nhòe
        g.drawCenteredString(font, title, width / 2, height / 2 - 90, 0xFFE8C060);
        g.drawCenteredString(font, Component.translatable("hukhong.skillbook.upgrade_scroll.pick"), width / 2, height / 2 - 78, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
