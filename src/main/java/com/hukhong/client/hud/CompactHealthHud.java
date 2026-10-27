package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Thanh máu xếp lớp thay hàng tim vanilla khi máu lớn: mỗi lớp = 100 máu, đổi màu theo lớp (đỏ, cam, vàng, lục, lam, xanh,
 * tím...), lớp dưới hiện làm nền phía sau, chữ "xN" = số lớp còn lại, số máu + hấp thụ (viền vàng phía trên). Giáp > 20
 * cũng gộp thành thanh gọn. Máu nhỏ (tổng <= 40 = 2 hàng tim) giữ nguyên tim vanilla. Tắt bằng config compactHealth.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CompactHealthHud {
    private CompactHealthHud() {}

    /** Máu mỗi lớp. */
    static final int ROW_HP = 100;
    /** Từ ngưỡng này (máu tối đa + hấp thụ) thì dùng thanh gọn. */
    static final int COMPACT_FROM = 40;

    private static final int BAR_W = 90, BAR_H = 10, PLATE_W = 17;
    private static final int[] PALETTE = {
            0xFFD83A3A, 0xFFE8802C, 0xFFE8C838, 0xFF58C040, 0xFF38C8C8,
            0xFF4878E8, 0xFF9858E0, 0xFFD848B0, 0xFFEEEEEE, 0xFFFFD060};
    private static final int[] PALETTE_CB = { // Okabe-Ito
            0xFFD55E00, 0xFFE69F00, 0xFFF0E442, 0xFF009E73, 0xFF56B4E9,
            0xFF0072B2, 0xFFCC79A7, 0xFF999999, 0xFFEEEEEE, 0xFFFFD060};
    private static final int INK = 0xFF0E0812, BG = 0xFF2A2030, GOLD = 0xFFE8C060;

    private static int lastHp = -1;
    private static int flashUntil;

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.wrapLayer(VanillaGuiLayers.PLAYER_HEALTH, orig -> (g, dt) -> {
            if (useCompact()) health(g, dt); else orig.render(g, dt);
        });
        e.wrapLayer(VanillaGuiLayers.ARMOR_LEVEL, orig -> (g, dt) -> {
            if (ClientConfig.compactHealth() && armorValue() > 20) armor(g); else orig.render(g, dt);
        });
    }

    private static Player cam() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCameraEntity() instanceof Player p ? p : null;
    }

    private static int armorValue() {
        Player p = cam();
        return p == null ? 0 : p.getArmorValue();
    }

    private static boolean useCompact() {
        if (!ClientConfig.compactHealth()) return false;
        Player p = cam();
        if (p == null) return false;
        float max = Math.max((float) p.getAttributeValue(Attributes.MAX_HEALTH), p.getHealth());
        return max + p.getAbsorptionAmount() > COMPACT_FROM;
    }

    private static int[] pal() { return ClientConfig.colorblind() ? PALETTE_CB : PALETTE; }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    private static void health(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        Player p = cam();
        if (p == null) return;
        Gui gui = mc.gui;
        Font font = mc.font;
        int hp = Mth.ceil(p.getHealth());
        int max = Math.max(Mth.ceil(p.getAttributeValue(Attributes.MAX_HEALTH)), hp);
        int abs = Mth.ceil(p.getAbsorptionAmount());
        if (lastHp >= 0 && hp < lastHp) flashUntil = p.tickCount + 8;
        lastHp = hp;

        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - gui.leftHeight;
        gui.leftHeight += BAR_H + 2;

        int stacks = Math.max(1, (max + ROW_HP - 1) / ROW_HP);
        int idx = hp <= 0 ? 0 : Math.min(stacks - 1, (hp - 1) / ROW_HP);
        int[] pal = pal();
        int col = pal[idx % pal.length];
        if (p.hasEffect(MobEffects.POISON)) col = mix(col, 0xFF7A9A20, 0.6f);
        else if (p.hasEffect(MobEffects.WITHER)) col = mix(col, 0xFF30303A, 0.65f);
        else if (p.getTicksFrozen() > 0) col = mix(col, 0xFFA8DCF0, 0.5f);

        // bản (plate) trái: "xN"
        int bx = x + PLATE_W, bw = BAR_W - PLATE_W;
        g.fill(x, y, x + PLATE_W, y + BAR_H, INK);
        g.fill(x + 1, y + 1, x + PLATE_W - 1, y + BAR_H - 1, BG);
        String stk = stacks > 1 ? "x" + (idx + 1) : "HP";
        float ps = 0.75f;
        g.pose().pushPose();
        g.pose().translate(x + PLATE_W / 2f - font.width(stk) * ps / 2f, y + 2.2f, 0);
        g.pose().scale(ps, ps, 1f);
        g.drawString(font, stk, 0, 0, pal[idx % pal.length], true);
        g.pose().popPose();

        // thanh
        g.fill(bx, y, bx + bw, y + BAR_H, INK);
        int ix = bx + 1, iy = y + 1, iw = bw - 2, ih = BAR_H - 2;
        g.fill(ix, iy, ix + iw, iy + ih, idx > 0 ? mix(pal[(idx - 1) % pal.length], INK, 0.35f) : BG); // lớp dưới làm nền
        int in = hp - idx * ROW_HP;
        int fw = Mth.clamp(Math.round(iw * in / (float) ROW_HP), hp > 0 ? 1 : 0, iw);
        if (fw > 0) {
            g.fill(ix, iy, ix + fw, iy + ih, col);
            g.fill(ix, iy, ix + fw, iy + 2, mix(col, 0xFFFFFFFF, 0.3f));          // sáng phía trên
            g.fill(ix, iy + ih - 1, ix + fw, iy + ih, mix(col, INK, 0.4f));        // tối phía dưới
        }
        if (p.tickCount < flashUntil) g.fill(ix, iy, ix + iw, iy + ih, 0x50FFFFFF);
        for (int k = 1; k < 4; k++) g.fill(ix + iw * k / 4, iy, ix + iw * k / 4 + 1, iy + ih, 0x30000000); // vạch 25%

        // hấp thụ: dải vàng 2px phía trên
        if (abs > 0) {
            int aw = Mth.clamp(Math.round(iw * abs / (float) ROW_HP), 1, iw);
            g.fill(ix, iy, ix + aw, iy + 2, 0xFFFFD84A);
            g.fill(ix, iy + 2, ix + aw, iy + 3, 0xFF9A7A20);
        }

        // số máu
        String txt = hp + "/" + max + (abs > 0 ? " +" + abs : "");
        float ts = txt.length() > 12 ? 0.6f : 0.75f;
        g.pose().pushPose();
        g.pose().translate(bx + bw / 2f - font.width(txt) * ts / 2f, y + 2.2f, 0);
        g.pose().scale(ts, ts, 1f);
        g.drawString(font, txt, 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    private static void armor(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Player p = cam();
        if (p == null) return;
        Gui gui = mc.gui;
        Font font = mc.font;
        int val = p.getArmorValue();
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - gui.leftHeight;
        gui.leftHeight += 8;
        int h = 6;
        g.fill(x, y, x + BAR_W, y + h, INK);
        g.fill(x + 1, y + 1, x + BAR_W - 1, y + h - 1, BG);
        int rows = Math.max(1, (val + 19) / 20); // mỗi lớp 20 giáp (10 biểu tượng vanilla)
        int idx = Math.min(rows - 1, (val - 1) / 20);
        int in = val - idx * 20;
        int iw = BAR_W - 2;
        if (idx > 0) g.fill(x + 1, y + 1, x + 1 + iw, y + h - 1, 0xFF6C7A88);
        int fw = Mth.clamp(Math.round(iw * in / 20f), 1, iw);
        g.fill(x + 1, y + 1, x + 1 + fw, y + h - 1, 0xFFB8C4D0);
        g.fill(x + 1, y + 1, x + 1 + fw, y + 2, 0xFFE8F0F8);
        String txt = (rows > 1 ? "x" + (idx + 1) + " " : "") + val;
        g.pose().pushPose();
        g.pose().translate(x + BAR_W / 2f - font.width(txt) * 0.6f / 2f, y + 0.6f, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(font, txt, 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }
}
