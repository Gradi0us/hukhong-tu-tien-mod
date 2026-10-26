package com.hukhong.client.screen.core;

import com.hukhong.HuKhong;
import com.hukhong.cores.CoreDef;
import com.hukhong.cores.CoreRegistry;
import com.hukhong.cores.CoreTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Vẽ thẻ Lõi (48x64 phóng 1.5x) và mô tả bị động — dùng chung cho Ấn Hồn Đài và Bảng tu sĩ. */
public final class CoreCardRenderer {
    private CoreCardRenderer() {}

    public static final int CW = 72, CH = 96;
    private static final ResourceLocation BACK = tex("card_back");

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/gui/core/" + n + ".png");
    }

    public static ResourceLocation frame(CoreTier t) { return tex("card_frame_" + t.id); }

    public static CoreDef def(ResourceLocation id) {
        var level = Minecraft.getInstance().level;
        return level == null ? null : CoreRegistry.get(level.registryAccess(), id);
    }

    public static int textColor(CoreTier t) { return 0xFF000000 | (t == CoreTier.HAC ? 0xA0A0B8 : t.rgb); }

    /** Vẽ một mặt thẻ ở (0,0) hệ toạ độ hiện tại. */
    public static void drawFace(GuiGraphics g, Font font, boolean back, ResourceLocation core, CoreTier tier) {
        if (back) {
            g.blit(BACK, 0, 0, CW, CH, 0, 0, 48, 64, 48, 64);
            g.pose().pushPose();
            g.pose().translate(CW / 2f, CH / 2f - 6, 0);
            g.pose().scale(3f, 3f, 1f);
            g.drawCenteredString(font, "?", 0, 0, 0xFFAAAAAA);
            g.pose().popPose();
            return;
        }
        g.fill(3, 3, CW - 3, CH - 3, 0xFF14121C);
        g.blit(frame(tier), 0, 0, CW, CH, 0, 0, 48, 64, 48, 64);
        if (core != null) {
            CoreDef d = def(core);
            Component name = Component.translatable(d != null ? d.nameKey() : "core.hukhong.unknown");
            g.pose().pushPose();
            g.pose().translate(CW / 2f, 22, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            int y = 0;
            for (var line : font.split(name, 80)) {
                g.drawString(font, line, -font.width(line) / 2, y, 0xFFFFFFFF, true);
                y += 10;
            }
            g.pose().popPose();
            if (d != null) {
                g.pose().pushPose();
                g.pose().translate(CW / 2f, CH - 30, 0);
                g.pose().scale(0.65f, 0.65f, 1f);
                Component sys = Component.translatable("hukhong.system." + d.system());
                g.drawString(font, sys, -font.width(sys) / 2, 0, 0xFFBBBBBB, false);
                g.pose().popPose();
            }
        }
        g.pose().pushPose();
        g.pose().translate(CW / 2f, CH - 18, 0);
        g.pose().scale(0.9f, 0.9f, 1f);
        Component tn = Component.translatable(tier.langKey());
        g.drawString(font, tn, -font.width(tn) / 2, 0, textColor(tier), true);
        g.pose().popPose();
    }

    /** Dòng mô tả bị động của Lõi ở phẩm cho trước. */
    public static List<Component> describe(ResourceLocation core, CoreTier tier) {
        List<Component> out = new ArrayList<>();
        CoreDef d = def(core);
        if (d == null) return out;
        for (CoreDef.Passive p : d.passives()) {
            double v = p.valueAt(tier);
            String num = p.operation() == AttributeModifier.Operation.ADD_VALUE
                    ? sign(v) + trim(v) : sign(v) + trim(v * 100) + "%";
            MutableComponent attr = BuiltInRegistries.ATTRIBUTE.getOptional(p.attribute())
                    .map(a -> Component.translatable(a.getDescriptionId()))
                    .orElse(Component.literal(p.attribute().getPath()));
            out.add(Component.literal(num + " ").append(attr));
        }
        return out;
    }

    private static String sign(double v) { return v >= 0 ? "+" : ""; }

    private static String trim(double v) {
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
