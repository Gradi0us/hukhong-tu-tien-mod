package com.hukhong.client.screen.core;

import com.hukhong.cores.CoreDef;
import com.hukhong.cores.CoreTier;
import com.hukhong.menu.CoreAltarMenu;
import com.hukhong.network.CoreRevealPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/** Màn Ấn Hồn Đài: 3 thẻ mở + thẻ 4 úp (Phó Mặc Số Phận) lật khi chọn. Không blur nền. */
public class CoreAltarScreen extends Screen implements MenuAccess<CoreAltarMenu> {
    private static final int GAP = 14, FLIP_TICKS = 18;
    private static final int CW = CoreCardRenderer.CW, CH = CoreCardRenderer.CH;

    private final CoreAltarMenu menu;
    private int selected = -1;
    private boolean waiting, done, stuck;
    private int waitTicks, flipTicks = -1, openTicks, doneTicks;
    private ResourceLocation resCore;
    private CoreTier resTier = CoreTier.BACH;
    private int resIndex = -1;
    private Button confirm;

    public CoreAltarScreen(CoreAltarMenu menu, Inventory inv, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public CoreAltarMenu getMenu() { return menu; }

    @Override
    protected void init() {
        confirm = addRenderableWidget(Button.builder(Component.translatable("hukhong.altar.confirm"), b -> {
            if (done) { onClose(); return; }
            if (selected < 0 || waiting) return;
            waiting = true;
            waitTicks = 0;
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, selected);
        }).bounds(width / 2 - 60, cardY() + CH + 70, 120, 20).build());
        confirm.active = false;
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private boolean than() { return menu.milestone == com.hukhong.cores.CoreRoller.THAN_MILESTONE; }

    private int cardX(int i) {
        if (than()) return width / 2 - CW / 2;
        int total = 4 * CW + 3 * GAP;
        return width / 2 - total / 2 + i * (CW + GAP);
    }

    private int cardY() { return height / 2 - CH / 2 - 30; }

    public void onReveal(CoreRevealPayload p) {
        done = true;
        waiting = false;
        stuck = p.stuck();
        resIndex = p.index();
        resCore = p.core();
        resTier = CoreTier.of(p.tier());
        selected = p.index();
        if (p.index() == 3) flipTicks = 0;
        var sm = Minecraft.getInstance().getSoundManager();
        if (stuck) sm.play(SimpleSoundInstance.forUI(SoundEvents.WITHER_SPAWN, 0.5f));
        else sm.play(SimpleSoundInstance.forUI(resTier.ordinal() >= CoreTier.TU.ordinal()
                ? SoundEvents.PLAYER_LEVELUP : SoundEvents.EXPERIENCE_ORB_PICKUP, 1f));
        confirm.setMessage(Component.translatable("hukhong.altar.close"));
        confirm.active = true;
    }

    @Override
    public void tick() {
        openTicks++;
        if (flipTicks >= 0 && flipTicks < FLIP_TICKS) flipTicks++;
        if (done) doneTicks++;
        if (waiting && ++waitTicks > 60) waiting = false; // server từ chối: cho thử lại
        if (!done) confirm.active = selected >= 0 && !waiting;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && !done && !waiting) {
            for (int i = 0; i < (than() ? 1 : 4); i++) {
                int x = cardX(i), y = cardY();
                if (mx >= x && mx < x + CW && my >= y && my < y + CH) {
                    selected = i;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** 1.21: super.render() gọi renderBackground() (có blur) SAU khi ta đã vẽ → làm mờ nội dung. Tắt ở đây. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderTransparentBackground(g);
        float time = openTicks + partial;
        g.drawCenteredString(font, title, width / 2, cardY() - 28, 0xFFFFFFFF);
        Component sub = Component.translatable("hukhong.altar.subtitle", com.hukhong.cores.CoreService.milestoneLevel(menu.milestone),
                Component.translatable(CoreTier.of(menu.tier).langKey()));
        g.drawCenteredString(font, sub, width / 2, cardY() - 15, 0xFFFFD34F);

        int hover = -1;
        for (int i = 0; i < (than() ? 1 : 4); i++) {
            int x = cardX(i), y = cardY();
            boolean over = !done && mx >= x && mx < x + CW && my >= y && my < y + CH;
            if (over) hover = i;
            int lift = (over || selected == i) ? -4 : 0;
            CoreTier t = i == 3 ? (resIndex == 3 ? resTier : CoreTier.BACH) : CoreTier.of(menu.tier);
            if (selected == i) {
                float pulse = 0.5f + 0.5f * Mth.sin(time * 0.3f);
                int c = (i == 3 && !done) ? 0xB0B0B0 : CoreCardRenderer.glowRgb(t);
                g.fill(x - 3, y - 3 + lift, x + CW + 3, y + CH + 3 + lift, ((int) (90 + 120 * pulse) << 24) | c);
            }
            boolean back = i == 3;
            float sx = 1f;
            if (i == 3 && flipTicks >= 0) {
                float f = Math.min(1f, (flipTicks + partial) / FLIP_TICKS);
                sx = Math.abs(Mth.cos(f * (float) Math.PI));
                back = f < 0.5f;
            }
            g.pose().pushPose();
            g.pose().translate(x + CW / 2f, y + CH / 2f + lift, 0);
            g.pose().scale(Math.max(0.02f, sx), 1f, 1f);
            g.pose().translate(-CW / 2f, -CH / 2f, 0);
            if (stuck && i == 3 && !back) {
                CoreCardRenderer.drawFace(g, font, true, null, CoreTier.BACH);
                g.fill(0, 0, CW, CH, 0xCC000000);
            } else {
                ResourceLocation core = i < 3 ? menu.ids.get(Math.min(i, menu.ids.size() - 1)) : (back ? null : resCore);
                CoreCardRenderer.drawFace(g, font, back, core, t);
            }
            g.pose().popPose();
        }

        // Thông tin thẻ đang trỏ / chọn
        int show = hover >= 0 ? hover : selected;
        int ty = cardY() + CH + 12;
        if (stuck) {
            g.drawCenteredString(font, Component.translatable("hukhong.altar.stuck"), width / 2, ty, 0xFFFF5555);
        } else if (done) {
            info(g, ty, resCore, resTier);
        } else if (show >= 0 && show < 3) {
            info(g, ty, menu.ids.get(Math.min(show, menu.ids.size() - 1)), CoreTier.of(menu.tier));
        } else if (show == 3) {
            g.drawCenteredString(font, Component.translatable("hukhong.altar.fourth.name"), width / 2, ty, 0xFFB0B0B0);
            g.drawCenteredString(font, Component.translatable("hukhong.altar.fourth.desc"), width / 2, ty + 12, 0xFF888888);
        }
        if (!done) {
            g.drawCenteredString(font, Component.translatable("hukhong.altar.warning"), width / 2, cardY() + CH + 56, 0xFFFF8855);
        } else if (!stuck && doneTicks < 60) {
            g.drawCenteredString(font, Component.translatable("hukhong.altar.done"), width / 2, cardY() + CH + 56, 0xFF55FF55);
        }
        super.render(g, mx, my, partial);
    }

    private void info(GuiGraphics g, int y, ResourceLocation core, CoreTier tier) {
        CoreDef d = CoreCardRenderer.def(core);
        if (d == null) return;
        g.drawCenteredString(font, Component.translatable(d.nameKey()).append("  [")
                .append(Component.translatable(tier.langKey())).append("]"), width / 2, y, CoreCardRenderer.textColor(tier));
        g.drawCenteredString(font, Component.translatable("hukhong.altar.system",
                Component.translatable("hukhong.system." + d.system()),
                Component.translatable("hukhong.core.path." + d.path())), width / 2, y + 11, 0xFFBBBBBB);
        int yy = y + 22;
        for (Component c : CoreCardRenderer.describe(core, tier)) {
            g.drawCenteredString(font, c, width / 2, yy, 0xFF8FE08F);
            yy += 10;
        }
    }
}
