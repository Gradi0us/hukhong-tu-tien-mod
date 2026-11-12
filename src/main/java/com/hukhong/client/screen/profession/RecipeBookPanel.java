package com.hukhong.client.screen.profession;

import com.hukhong.craft.ClientRecipeBook;
import com.hukhong.craft.RecipeBook;
import com.hukhong.craft.RecipeBookNet;
import com.hukhong.profession.ClientProfession;
import com.hukhong.profession.Profession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * So cong thuc (panel ben trai man tram). Dung lai duoc: new RecipeBookPanel(font, prof, x, y) trong init() sau super.init();
 * render() trong render() sau super.render(); mouseClicked / mouseScrolled chuyen tiep tu man hinh (tra true = da xu ly).
 * Chi liet ke cong thuc da biet (ClientRecipeBook) cua nghe {@code prof}. Cap nghe < cap can -> ten xam.
 * Bam 1 cong thuc -> hien nguyen lieu; "Xep nguyen lieu" gui RecipeBookNet.Arrange len server.
 */
public final class RecipeBookPanel {
    public static final int W = 124, H = 196, ROWS = 5, ROW_H = 18;
    private static final int LIST_Y = 16, DETAIL_Y = LIST_Y + ROWS * ROW_H + 2, BTN_Y = H - 18;

    private final Profession prof;
    private final int x, y;
    private int scroll, selected = -1;

    public RecipeBookPanel(Font font, Profession prof, int x, int y) {
        this.prof = prof;
        this.x = x;
        this.y = y;
    }

    /** Cong thuc da biet cua nghe nay (thu tu theo danh sach goc). */
    private List<RecipeBook.Entry> known() {
        List<RecipeBook.Entry> out = new ArrayList<>();
        for (RecipeBook.Entry e : RecipeBook.entries(prof)) if (ClientRecipeBook.knows(e.id())) out.add(e);
        return out;
    }

    private int playerLevel() {
        var d = ClientProfession.get();
        return d.profession() == prof ? d.level() : 0;
    }

    public void render(GuiGraphics g, Font font, int mx, int my) {
        g.fill(x, y, x + W, y + H, 0xEE0E0E16);
        g.fill(x, y, x + W, y + 1, 0xFF555555);
        g.fill(x, y + H - 1, x + W, y + H, 0xFF555555);
        g.fill(x, y, x + 1, y + H, 0xFF555555);
        g.fill(x + W - 1, y, x + W, y + H, 0xFF555555);

        List<RecipeBook.Entry> list = known();
        clamp(list.size());
        g.drawString(font, Component.translatable("hukhong.recipe.book.title", list.size()), x + 6, y + 4, 0xFFE0C080, false);
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable("hukhong.recipe.book.empty"), x + 6, y + LIST_Y + 2, 0xFF9E9E9E, false);
            return;
        }
        int lv = playerLevel();
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            RecipeBook.Entry e = list.get(idx);
            int ry = y + LIST_Y + i * ROW_H;
            if (idx == selected) g.fill(x + 2, ry, x + W - 2, ry + ROW_H - 1, 0x663A6EA5);
            g.renderItem(e.display().get(), x + 4, ry + 1);
            boolean ok = lv >= e.requiredLevel();
            g.drawString(font, font.plainSubstrByWidth(e.name().getString(), W - 26), x + 22, ry + 1, ok ? 0xFFFFFFFF : 0xFF777777, false);
            g.drawString(font, Component.translatable("hukhong.recipe.book.info", e.tier(), e.requiredLevel()),
                    x + 22, ry + 9, ok ? 0xFF9E9E9E : 0xFF6B5050, false);
        }
        if (list.size() > ROWS) {
            g.drawString(font, (scroll + 1) + "-" + Math.min(list.size(), scroll + ROWS) + "/" + list.size(),
                    x + W - 34, y + 4, 0xFF9E9E9E, false);
        }

        if (selected < 0 || selected >= list.size()) {
            g.drawString(font, Component.translatable("hukhong.recipe.book.pick"), x + 6, y + DETAIL_Y + 4, 0xFF9E9E9E, false);
            return;
        }
        RecipeBook.Entry e = list.get(selected);
        g.drawString(font, font.plainSubstrByWidth(e.name().getString(), W - 12), x + 6, y + DETAIL_Y, 0xFFFFFFFF, false);
        g.drawString(font, Component.translatable("hukhong.recipe.book.level", e.requiredLevel()), x + 6, y + DETAIL_Y + 10,
                lv >= e.requiredLevel() ? 0xFF9E9E9E : 0xFFE57373, false);
        g.drawString(font, Component.translatable("hukhong.recipe.book.ingredients"), x + 6, y + DETAIL_Y + 22, 0xFFCCCCCC, false);
        var player = Minecraft.getInstance().player;
        int line = 0;
        for (RecipeBook.Ing ing : e.ingredients()) {
            if (line >= 4) break;
            ItemStack def = ing.item().get().getDefaultInstance();
            int have = player == null ? 0 : player.getInventory().countItem(ing.item().get());
            String txt = font.plainSubstrByWidth(def.getHoverName().getString(), W - 40) + " x" + ing.count()
                    + " (" + have + ")";
            g.drawString(font, txt, x + 8, y + DETAIL_Y + 32 + line * 10, have >= ing.count() ? 0xFF8BC34A : 0xFFE57373, false);
            line++;
        }
        boolean hover = inButton(mx, my);
        g.fill(x + 6, y + BTN_Y, x + W - 6, y + BTN_Y + 14, hover ? 0xFF4F7CAC : 0xFF2E4A66);
        g.drawCenteredString(font, Component.translatable("hukhong.recipe.arrange"), x + W / 2, y + BTN_Y + 3, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!contains(mx, my)) return false;
        if (button != 0) return true;
        List<RecipeBook.Entry> list = known();
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            int ry = y + LIST_Y + i * ROW_H;
            if (mx >= x + 2 && mx < x + W - 2 && my >= ry && my < ry + ROW_H - 1) { selected = idx; return true; }
        }
        if (selected >= 0 && selected < list.size() && inButton(mx, my)) {
            PacketDistributor.sendToServer(new RecipeBookNet.Arrange(list.get(selected).id()));
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double dy) {
        if (!contains(mx, my)) return false;
        int size = known().size();
        if (size > ROWS) {
            scroll -= (int) Math.signum(dy);
            clamp(size);
        }
        return true;
    }

    public boolean contains(double mx, double my) { return mx >= x && mx < x + W && my >= y && my < y + H; }

    private boolean inButton(double mx, double my) {
        return mx >= x + 6 && mx < x + W - 6 && my >= y + BTN_Y && my < y + BTN_Y + 14;
    }

    private void clamp(int size) {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, size - ROWS)));
        if (selected >= size) selected = -1;
    }
}
