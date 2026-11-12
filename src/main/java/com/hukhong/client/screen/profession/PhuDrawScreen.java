package com.hukhong.client.screen.profession;

import com.hukhong.craft.ClientRecipeBook;
import com.hukhong.phapkhi.PhapKhiKind;
import com.hukhong.profession.ClientProfession;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.phu.PhuDrawNet;
import com.hukhong.profession.phu.PhuPattern;
import com.hukhong.profession.station.StationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Minigame Ve phu (Phu An). Giu chuot trai o dau net roi keo theo net trong ong +-R px.
 * Lech ong > 1 s hoac het gio / nha chuot: gui (x, y, t) ve server, server cham lai va quyet dinh ket qua.
 * Esc / Huy: bo, khong gui gi (khong tieu nguyen lieu).
 */
public final class PhuDrawScreen extends Screen {
    private static final int CW = PhuPattern.CANVAS;
    /** Gioi han toa do gui di (server chap nhan [-400, 700]). */
    private static final int CLAMP_LO = -300, CLAMP_HI = 500;

    private final Screen parent;
    private final PhapKhiKind kind;
    private final int tier;
    private final int level;
    private final List<double[]> path;
    private final double radius;
    private final int limit;

    /** Mau: x, y, t, trong ong (1/0). */
    private final List<int[]> samples = new ArrayList<>();
    private boolean active, finished;
    private long t0;
    private int elapsed, lastSample = -1, outStart = -1;
    private int cx, cy;

    private PhuDrawScreen(Screen parent, PhapKhiKind kind, int tier, int level) {
        super(Component.translatable("hukhong.phu.title", Component.translatableWithFallback(kind.langKey(), kind.vi)));
        this.parent = parent;
        this.kind = kind;
        this.tier = tier;
        this.level = level;
        this.path = PhuPattern.path(kind.id, tier);
        this.radius = PhuPattern.radius(level);
        this.limit = PhuPattern.limitMs(tier);
    }

    /** Mo minigame tu man tram Phu An: lay cong thuc theo nguyen lieu trong o. */
    public static void open(Screen parent, StationMenu menu) {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> in = List.of(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), menu.getSlot(2).getItem());
        ProfessionRecipes.Recipe r = ProfessionRecipes.find(ProfessionRecipes.PHU, in);
        if (r == null) {
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable("hukhong.station.no_recipe"), true);
            return;
        }
        if (!ClientRecipeBook.knows("phu:" + r.id())) {
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable("hukhong.recipe.unknown"), true);
            return;
        }
        PhapKhiKind kind = PhuPattern.kindOf(r.id());
        if (kind == null) return;
        mc.setScreen(new PhuDrawScreen(parent, kind, r.tier(), ClientProfession.get().level()));
    }

    @Override
    protected void init() {
        cx = width / 2 - CW / 2;
        cy = height / 2 - CW / 2 + 6;
        addRenderableWidget(Button.builder(Component.translatable("hukhong.phu.cancel"), b -> onClose())
                .bounds(width / 2 - 30, cy + CW + 14, 60, 16).build());
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && !active && !finished && inCanvas(mx, my)) {
            active = true;
            t0 = System.currentTimeMillis();
            samples.clear();
            outStart = -1;
            lastSample = -1;
            addSample(0, mx, my);
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0 && active) { finish(); return true; }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public void onClose() {
        if (finished) return;
        finished = true; // bo: khong gui
        active = false;
        Minecraft.getInstance().setScreen(parent);
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= cx && mx < cx + CW && my >= cy && my < cy + CW;
    }

    // ------------------------------------------------------------------ logic

    /** Moi frame: lay mau moi 64 ms; het gio = ket thuc. */
    private void tick(double mx, double my) {
        int now = (int) (System.currentTimeMillis() - t0);
        elapsed = Math.min(now, limit);
        if (now >= limit) { finish(); return; }
        if (lastSample < 0 || now - lastSample >= PhuPattern.SAMPLE_MS) addSample(now, mx, my);
    }

    private void addSample(int t, double mx, double my) {
        if (samples.size() >= PhuPattern.MAX_SAMPLES) { finish(); return; }
        int x = (int) Math.max(CLAMP_LO, Math.min(CLAMP_HI, Math.round(mx - cx)));
        int y = (int) Math.max(CLAMP_LO, Math.min(CLAMP_HI, Math.round(my - cy)));
        boolean in = PhuPattern.distToPath(x, y, path) <= radius;
        if (in) outStart = -1;
        else if (outStart < 0) outStart = t;
        samples.add(new int[]{x, y, t, in ? 1 : 0});
        lastSample = t;
        if (!in && t - outStart > PhuPattern.OUT_LIMIT_MS) finish();
    }

    /** Gui mau len server 1 lan. */
    private void finish() {
        if (finished) return;
        finished = true;
        active = false;
        List<Integer> flat = new ArrayList<>(samples.size() * 3);
        for (int[] s : samples) { flat.add(s[0]); flat.add(s[1]); flat.add(s[2]); }
        PacketDistributor.sendToServer(new PhuDrawNet.Submit(flat, Math.max(elapsed, lastSample)));
        Minecraft.getInstance().setScreen(parent);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);
        if (active) tick(mx, my);
        if (finished) return;

        g.drawCenteredString(font, title, width / 2, cy - 26, 0xFFE0C080);
        g.drawCenteredString(font, Component.translatable("hukhong.phu.info", tier, (int) Math.round(radius)), width / 2, cy - 14, 0xFFAAAAAA);

        g.fill(cx - 4, cy - 4, cx + CW + 4, cy + CW + 4, 0xEE111119);
        // ong
        int r = (int) Math.round(radius);
        for (int i = 0; i + 1 < path.size(); i++) {
            double[] a = path.get(i), b = path.get(i + 1);
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(b[0] - a[0], b[1] - a[1]) / 3));
            for (int s = 0; s <= steps; s++) {
                int px = (int) Math.round(a[0] + (b[0] - a[0]) * s / steps);
                int py = (int) Math.round(a[1] + (b[1] - a[1]) * s / steps);
                g.fill(cx + px - r, cy + py - r, cx + px + r, cy + py + r, 0x2266AAFF);
                g.fill(cx + px - 1, cy + py - 1, cx + px + 1, cy + py + 1, 0xFFE0C080);
            }
        }
        double[] start = path.get(0);
        g.fill(cx + (int) start[0] - 3, cy + (int) start[1] - 3, cx + (int) start[0] + 3, cy + (int) start[1] + 3, 0xFFFFFFFF);

        // mau cua nguoi choi
        for (int[] s : samples) {
            g.fill(cx + s[0] - 1, cy + s[1] - 1, cx + s[0] + 1, cy + s[1] + 1, s[3] == 1 ? 0xFF8BC34A : 0xFFE57373);
        }

        // trang thai
        int y = cy + CW + 4;
        if (!active) {
            g.drawCenteredString(font, Component.translatable("hukhong.phu.hint"), width / 2, y, 0xFFCCCCCC);
        } else {
            double sec = elapsed / 1000.0, lim = limit / 1000.0;
            g.drawCenteredString(font, Component.translatable("hukhong.phu.time", String.format("%.1f", sec), String.format("%.1f", lim)),
                    width / 2, y, 0xFFCCCCCC);
            if (outStart >= 0) {
                String out = String.format("%.1f", (elapsed - outStart) / 1000.0);
                g.drawCenteredString(font, Component.translatable("hukhong.phu.out_tube", out), width / 2, y + 10, 0xFFE57373);
            } else {
                g.drawCenteredString(font, Component.translatable("hukhong.phu.in_tube"), width / 2, y + 10, 0xFF8BC34A);
            }
        }
    }
}
