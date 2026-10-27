package com.hukhong.client.objective;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.List;

/** Dòng mục tiêu dưới HUD tu luyện (góc trái trên), kiểu banner gọn: nền tối, viền vàng, tối đa 40% chiều rộng. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ObjectiveHud {
    private ObjectiveHud() {}

    private static final int GOLD = 0xFFC9A24A, GOLD_LT = 0xFFF6E7B4;

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "objective_hud"), ObjectiveHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.showHud() || mc.player == null || mc.level == null || mc.options.hideGui || !ClientObjective.visible()) return;
        Font font = mc.font;
        var d = ClientObjective.data();
        Component line = Component.literal("◆ ").withStyle(s -> s.withColor(GOLD & 0xFFFFFF))
                .append(Component.translatable("hukhong.objective.label").withStyle(s -> s.withColor(GOLD & 0xFFFFFF)))
                .append(Component.literal(" ")).append(d.text());
        BlockPos tg = ClientObjective.sameDimTarget();
        if (tg != null) {
            int dist = (int) Math.sqrt(mc.player.distanceToSqr(tg.getX() + 0.5, mc.player.getY(), tg.getZ() + 0.5));
            line = line.copy().append(Component.literal(" · " + dist + "m").withStyle(s -> s.withColor(0xB8B0A0)));
        }
        float sc = 0.85f;
        int maxW = Math.max(100, (int) (g.guiWidth() * 0.4f));
        List<FormattedCharSequence> ls = font.split(line, (int) (maxW / sc));
        if (ls.size() > 3) ls = ls.subList(0, 3);
        int x = 6, y = 50, pad = 3;
        float w = 0;
        for (var l : ls) w = Math.max(w, font.width(l) * sc);
        int boxW = (int) Math.ceil(w) + 2 * pad + 2, boxH = (int) Math.ceil(ls.size() * 9 * sc) + 2 * pad;
        g.fill(x, y, x + boxW, y + boxH, 0x960E0A14);
        g.fill(x, y, x + 1, y + boxH, GOLD);
        g.fill(x, y, x + boxW, y + 1, 0x80C9A24A);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, 0x80C9A24A);
        float ty = y + pad + 0.5f;
        for (var l : ls) {
            g.pose().pushPose();
            g.pose().translate(x + pad + 2, ty, 0);
            g.pose().scale(sc, sc, 1f);
            g.drawString(font, l, 0, 0, 0xFFF0EAD8, true);
            g.pose().popPose();
            ty += 9 * sc;
        }
    }
}
