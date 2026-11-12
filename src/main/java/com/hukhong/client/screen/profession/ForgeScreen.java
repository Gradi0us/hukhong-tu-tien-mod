package com.hukhong.client.screen.profession;

import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.station.StationMenu;
import com.hukhong.profession.khi.ForgeClient;
import com.hukhong.profession.khi.ForgeMath;
import com.hukhong.profession.khi.ForgeNet;
import com.hukhong.profession.khi.ForgeService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Man hinh Doan Tao Dai (PLAN-v1 §3): 3 o nguyen lieu + ket qua o ben trai (nhu tram cu), bang Nung-Ron-Toi o ben phai.
 * Moi thao tac gui len server (ForgeNet.act); server cham diem tu tick bam. Client chi ve lai tu State.
 */
public class ForgeScreen extends AbstractContainerScreen<StationMenu> {
    private static final int PANEL_W = 150;
    private static final int TRACK_W = PANEL_W - 10;

    private RecipeBookPanel book;
    private Button main, cancel, quenchWater, quenchToi, quenchDau, refine, repair, dismantle;

    public ForgeScreen(StationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    private int px() { return leftPos + imageWidth + 6; }

    private int py() { return topPos; }

    @Override
    protected void init() {
        super.init();
        int x = px() + 5, y = py();
        main = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.start"), b -> onMain())
                .bounds(x, y + 58, TRACK_W, 18).build());
        cancel = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.cancel"),
                b -> ForgeNet.act(ForgeService.OP_CANCEL, 0)).bounds(x, y + 78, TRACK_W, 14).build());
        quenchWater = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.quench.water"),
                b -> ForgeNet.act(ForgeService.OP_Q_WATER, 0)).bounds(x, y + 100, 44, 18).build());
        quenchToi = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.quench.toi"),
                b -> ForgeNet.act(ForgeService.OP_Q_TOI, 0)).bounds(x + 48, y + 100, 44, 18).build());
        quenchDau = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.quench.dau"),
                b -> ForgeNet.act(ForgeService.OP_Q_DAU, 0)).bounds(x + 96, y + 100, 44, 18).build());
        refine = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.refine"),
                b -> ForgeNet.act(ForgeService.OP_REFINE, 0)).bounds(x, y + 124, 68, 16).build());
        repair = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.repair"),
                b -> ForgeNet.act(ForgeService.OP_REPAIR, 0)).bounds(x + 72, y + 124, 68, 16).build());
        dismantle = addRenderableWidget(Button.builder(Component.translatable("hukhong.forge.dismantle"),
                b -> ForgeNet.act(ForgeService.OP_DISMANTLE, 0)).bounds(x, y + 144, TRACK_W, 16).build());
        book = new RecipeBookPanel(font, Profession.DOAN_TAO, leftPos - RecipeBookPanel.W - 4, topPos);
        ForgeNet.act(ForgeService.OP_SYNC, 0);
    }

    /** Thoi gian (tick) ke tu luc Nung, theo dong ho server (ForgeNet.State.startTick). */
    private int elapsed(ForgeNet.State st) {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : (int) (level.getGameTime() - st.startTick());
    }

    private void onMain() {
        ForgeNet.State st = ForgeClient.get();
        switch (st.phase()) {
            case ForgeService.PH_IDLE -> ForgeNet.act(ForgeService.OP_START, 0);
            case ForgeService.PH_HEAT -> ForgeNet.act(ForgeService.OP_PULL, elapsed(st));
            case ForgeService.PH_HAMMER -> ForgeNet.act(ForgeService.OP_STRIKE, elapsed(st));
            default -> { }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (book != null && book.mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (book != null && book.mouseScrolled(mx, my, sy)) return true;
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        frame(g, x, y, imageWidth, imageHeight);
        for (int[] p : StationMenu.POS) g.fill(x + p[0] - 1, y + p[1] - 1, x + p[0] + 17, y + p[1] + 17, 0xFF444466);
        frame(g, px(), py(), PANEL_W, imageHeight);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xEE111119);
        g.fill(x, y, x + w, y + 1, 0xFF555555);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x, y, x + 1, y + h, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        ForgeNet.State st = ForgeClient.get();
        List<ItemStack> in = List.of(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), menu.getSlot(2).getItem());
        ProfessionRecipes.Recipe r = ProfessionRecipes.find(ProfessionRecipes.FORGE, in);
        Component status = r != null ? Component.translatable("hukhong.station.match", r.out().make(1, false).getHoverName())
                : Component.translatable(in.stream().allMatch(ItemStack::isEmpty) ? "hukhong.station.hint" : "hukhong.station.nomatch");
        g.drawString(font, status, leftPos + 8, topPos + 64, 0xFFCCCCCC, false);

        updateButtons(st);
        drawPanel(g, st);
        if (book != null) book.render(g, font, mx, my);
    }

    private void updateButtons(ForgeNet.State st) {
        int ph = st.phase();
        boolean quench = ph == ForgeService.PH_QUENCH;
        boolean idle = ph == ForgeService.PH_IDLE;
        main.visible = !quench;
        main.active = !quench;
        main.setMessage(Component.translatable(switch (ph) {
            case ForgeService.PH_HEAT -> "hukhong.forge.pull";
            case ForgeService.PH_HAMMER -> "hukhong.forge.strike";
            default -> "hukhong.forge.start";
        }));
        cancel.visible = !idle;
        for (Button b : List.of(quenchWater, quenchToi, quenchDau)) b.visible = quench;
        for (Button b : List.of(refine, repair, dismantle)) b.visible = idle;
    }

    private void drawPanel(GuiGraphics g, ForgeNet.State st) {
        int x = px() + 5, y = py();
        g.drawString(font, Component.translatable("hukhong.forge.title"), x, y + 6, 0xFFE0C070, false);
        int ph = st.phase();
        Component phase = Component.translatable(switch (ph) {
            case ForgeService.PH_HEAT -> "hukhong.forge.phase.heat";
            case ForgeService.PH_HAMMER -> "hukhong.forge.phase.hammer";
            case ForgeService.PH_QUENCH -> "hukhong.forge.phase.quench";
            default -> "hukhong.forge.phase.idle";
        });
        g.drawString(font, phase, x, y + 18, 0xFFCCCCCC, false);
        if (ph == ForgeService.PH_IDLE) {
            g.drawString(font, Component.translatable("hukhong.forge.hint"), x, y + 32, 0xFF999999, false);
        } else if (ph == ForgeService.PH_HEAT) {
            drawHeat(g, x, y + 32, st);
        } else if (ph == ForgeService.PH_HAMMER) {
            drawHammer(g, x, y + 32, st);
        } else {
            g.drawString(font, Component.translatable("hukhong.forge.quench.hint"), x, y + 32, 0xFF999999, false);
        }
        if (ph == ForgeService.PH_HAMMER || ph == ForgeService.PH_QUENCH)
            g.drawString(font, Component.translatable("hukhong.forge.score", st.heatPts(), st.hammerSum()),
                    x, y + 46, 0xFFAAAAAA, false);
    }

    private void drawHeat(GuiGraphics g, int x, int y, ForgeNet.State st) {
        int e = elapsed(st);
        double heat = ForgeMath.heat(e);
        double c = ForgeMath.heatCenter(st.seed());
        int hw = ForgeMath.heatHalfWidth(st.tier());
        g.fill(x, y, x + TRACK_W, y + 10, 0xFF222222);
        g.fill(x + zx(c - hw), y, x + zx(c + hw), y + 10, 0xFFB8860B);
        g.fill(x, y, x + zx(heat), y + 10, 0x88FF6A00);
        g.fill(x + zx(heat) - 1, y - 2, x + zx(heat) + 1, y + 12, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("hukhong.forge.heat", (int) heat), x, y + 14, 0xFFE0A060, false);
    }

    private void drawHammer(GuiGraphics g, int x, int y, ForgeNet.State st) {
        int e = elapsed(st);
        double needle = ForgeMath.needle(st.seed(), e);
        int k = Math.min(st.strikes(), ForgeMath.strikes(st.tier()) - 1);
        double c = ForgeMath.strikeCenter(st.seed(), k);
        int hw = ForgeMath.strikeHalfWidth(st.tier(), st.level());
        g.fill(x, y, x + TRACK_W, y + 10, 0xFF222222);
        g.fill(x + zx(c - hw), y, x + zx(c + hw), y + 10, 0xFFE8E8A0);
        g.fill(x + zx(c - hw / 2.0), y, x + zx(c + hw / 2.0), y + 10, 0xFFFFFFFF);
        g.fill(x + zx(needle) - 1, y - 2, x + zx(needle) + 1, y + 12, 0xFFFF5050);
        g.drawString(font, Component.translatable("hukhong.forge.strike_label", st.strikes() + 1, ForgeMath.strikes(st.tier())),
                x, y + 14, 0xFFE0A060, false);
    }

    /** Vi tri (px) tren thanh do theo gia tri 0..100. */
    private static int zx(double v) { return (int) Math.round(Math.max(0, Math.min(100, v)) / 100.0 * TRACK_W); }
}
