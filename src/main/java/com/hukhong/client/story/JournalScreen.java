package com.hukhong.client.story;

import com.hukhong.story.StoryData;
import com.hukhong.story.StoryDocs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Nhật ký truyện (phím J): danh sách Văn Kiện đã thu + nội dung + tiến trình. Chỉ đọc. */
public class JournalScreen extends Screen {
    private static final int GOLD = 0xFFFFD34F, GREY = 0xFF888888, WHITE = 0xFFEEEEEE, ROW = 12;
    private String selected;
    private int x0, y0, pw, ph;

    public JournalScreen() { super(Component.translatable("hukhong.story.journal.title")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        pw = Math.min(width - 20, 360);
        ph = Math.min(height - 20, 220);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        if (selected == null) for (String d : StoryClient.data().docs()) { selected = d; break; }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        StoryData d = StoryClient.data();
        g.fill(x0, y0, x0 + pw, y0 + ph, 0xE0101018);
        g.renderOutline(x0, y0, pw, ph, 0xFF6A4C9C);
        g.drawString(font, title, x0 + 8, y0 + 6, GOLD, false);
        if (d.mode() == StoryData.MODE_STORY) {
            Component obj = Component.translatable("hukhong.story.journal.objective",
                    Component.translatable("hukhong.story.chap." + NpcClient.data().current() + ".obj"));
            g.drawString(font, font.plainSubstrByWidth(obj.getString(), pw - 132), x0 + 124, y0 + 6, 0xFF55FFFF, false);
        }
        Component mode = Component.translatable(d.mode() == StoryData.MODE_STORY ? "hukhong.story.mode.story"
                : d.mode() == StoryData.MODE_FREE ? "hukhong.story.mode.free" : "hukhong.story.mode.none");
        g.drawString(font, Component.translatable("hukhong.story.journal.mode", mode), x0 + 8, y0 + 18, GREY, false);
        g.drawString(font, Component.translatable("hukhong.story.journal.progress", d.docs().size(), StoryDocs.ALL.size()),
                x0 + 8, y0 + 30, GREY, false);

        int listW = 110, y = y0 + 46;
        for (StoryDocs.Doc doc : StoryDocs.ALL) {
            boolean has = d.has(doc.id());
            boolean sel = doc.id().equals(selected);
            if (sel) g.fill(x0 + 6, y - 1, x0 + 6 + listW, y + ROW - 2, 0x806A4C9C);
            Component name = has ? Component.translatable(doc.itemKey()) : Component.translatable("hukhong.story.journal.unknown");
            g.drawString(font, font.plainSubstrByWidth(name.getString(), listW - 4), x0 + 8, y, has ? WHITE : GREY, false);
            y += ROW;
        }

        int tx = x0 + listW + 18, tw = pw - listW - 26;
        if (selected != null && d.has(selected)) {
            StoryDocs.Doc doc = StoryDocs.get(selected);
            g.drawString(font, Component.translatable(doc.itemKey()), tx, y0 + 46, GOLD, false);
            g.drawWordWrap(font, Component.translatable(doc.textKey()), tx, y0 + 60, tw, WHITE);
        } else {
            g.drawWordWrap(font, Component.translatable("hukhong.story.journal.hint"), tx, y0 + 46, tw, GREY);
        }

        int ly = y0 + ph - 28;
        for (char c : new char[]{'a', 'b'}) {
            boolean unlocked = (d.lair() & StoryDocs.lairBit(c)) != 0;
            Component t = unlocked ? Component.translatable("hukhong.story.lair." + c)
                    : Component.translatable("hukhong.story.journal.lair_locked." + c, d.chainCount(c), StoryDocs.CLUES_FOR_LAIR);
            g.drawString(font, font.plainSubstrByWidth(t.getString(), pw - 16), x0 + 8, ly, unlocked ? 0xFF55FF55 : GREY, false);
            ly += ROW;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        StoryData d = StoryClient.data();
        int y = y0 + 46;
        for (StoryDocs.Doc doc : StoryDocs.ALL) {
            if (mx >= x0 + 6 && mx < x0 + 116 && my >= y - 1 && my < y + ROW - 2) {
                if (d.has(doc.id())) selected = doc.id();
                return true;
            }
            y += ROW;
        }
        return super.mouseClicked(mx, my, button);
    }
}
