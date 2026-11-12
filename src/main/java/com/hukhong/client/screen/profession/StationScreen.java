package com.hukhong.client.screen.profession;

import com.hukhong.HuKhong;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionRegistry;
import com.hukhong.profession.station.ProfessionStationBlock;
import com.hukhong.profession.station.StationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

/** GUI Phu An / Doan Tao Dai: 3 o nguyen lieu, 1 o ket qua, nut Che tao, dong trang thai phuong khop. */
public class StationScreen extends AbstractContainerScreen<StationMenu> {
    public StationScreen(StationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    private RecipeBookPanel book;

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("hukhong.station.craft"),
                        b -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 30, topPos + 44, 84, 16).build());
        Profession prof = menu.station == ProfessionStationBlock.Station.PHU_AN ? Profession.PHU_VAN : Profession.DOAN_TAO;
        book = new RecipeBookPanel(font, prof, leftPos - RecipeBookPanel.W - 4, topPos);
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
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xEE111119);
        g.fill(x, y, x + imageWidth, y + 1, 0xFF555555);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);
        g.fill(x, y, x + 1, y + imageHeight, 0xFF555555);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        for (int[] p : StationMenu.POS) g.fill(x + p[0] - 1, y + p[1] - 1, x + p[0] + 17, y + p[1] + 17, 0xFF444466);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        List<ItemStack> in = List.of(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), menu.getSlot(2).getItem());
        ProfessionRecipes.Recipe r = ProfessionRecipes.find(menu.station.recipes(), in);
        Component status;
        if (r != null) status = Component.translatable("hukhong.station.match", r.out().make(1, false).getHoverName());
        else if (in.stream().allMatch(ItemStack::isEmpty)) status = Component.translatable("hukhong.station.hint");
        else status = Component.translatable("hukhong.station.nomatch");
        g.drawString(font, status, leftPos + 8, topPos + 64, 0xFFCCCCCC, false);
        if (book != null) book.render(g, font, mx, my);
    }

    @EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
    public static final class Reg {
        private Reg() {}

        @SubscribeEvent
        public static void onRegister(RegisterMenuScreensEvent e) {
            e.register(ProfessionRegistry.STATION_MENU.get(), StationScreen::open);
        }
    }

    /** Dang ky man hinh: Doan Tao Dai dung ForgeScreen (Nung-Ron-Toi), tram khac dung man hinh cu. */
    public static AbstractContainerScreen<StationMenu> open(StationMenu m, Inventory inv, Component t) {
        return m.station == ProfessionStationBlock.Station.DOAN_TAO_DAI
                ? new ForgeScreen(m, inv, t) : new StationScreen(m, inv, t);
    }
}
