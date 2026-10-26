package com.hukhong.client.screen.profile;

import com.hukhong.client.ClientCache;
import com.hukhong.client.screen.core.CoreCardRenderer;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.Path;
import com.hukhong.cores.CoreDef;
import com.hukhong.cores.CoreService;
import com.hukhong.cores.CoreSlots;
import com.hukhong.cores.CoreTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bảng tu sĩ (phím K): chỉ số, Lõi đã có, Hệ. Chỉ đọc. Không blur nền. */
public class ProfileScreen extends Screen {
    public ProfileScreen() { super(Component.translatable("hukhong.profile.title")); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderTransparentBackground(g);
        int pw = Math.min(width - 20, 440), ph = Math.min(height - 20, 230);
        int x0 = width / 2 - pw / 2, y0 = height / 2 - ph / 2;
        g.fill(x0 - 1, y0 - 1, x0 + pw + 1, y0 + ph + 1, 0xFF555555);
        g.fill(x0, y0, x0 + pw, y0 + ph, 0xEE111119);
        g.drawCenteredString(font, title, width / 2, y0 + 6, 0xFFFFD34F);

        CultivationData d = ClientCache.cultivation();
        int colW = pw / 2;
        int lx = x0 + 10, y = y0 + 22;

        // --- Cột trái: chỉ số
        g.drawString(font, Component.translatable("hukhong.hud.level", d.level()).append("  ")
                .append(Component.translatable("hukhong.realm." + d.realmIndex())), lx, y, 0xFFFFFFFF);
        y += 11;
        g.drawString(font, Component.translatable("hukhong.profile.exp", d.exp(),
                CultivationService.expToNext(d.level())), lx, y, 0xFFBBBBBB);
        y += 11;
        if (d.milestoneLocked()) {
            g.drawString(font, Component.translatable("hukhong.profile.milestone_locked"), lx, y, 0xFFFF8855);
        }
        y += 13;
        for (Path p : d.paths().stream().sorted().toList()) {
            Aptitude a = d.aptitude().get(p);
            g.drawString(font, Component.translatable(pathLang(p)).append(": ")
                    .append(a == null ? Component.literal("?") : Component.translatable("hukhong.aptitude." + a.name().toLowerCase(Locale.ROOT))),
                    lx, y, p.argb);
            y += 10;
        }
        y += 4;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            y = stat(g, lx, y, Attributes.MAX_HEALTH, player.getAttributeValue(Attributes.MAX_HEALTH));
            y = stat(g, lx, y, Attributes.ATTACK_DAMAGE, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            y = stat(g, lx, y, Attributes.ARMOR, player.getAttributeValue(Attributes.ARMOR));
            y = stat(g, lx, y, Attributes.MOVEMENT_SPEED, player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 10);
            y = stat(g, lx, y, Attributes.LUCK, player.getAttributeValue(Attributes.LUCK));
        }

        // --- Cột phải: Lõi + Hệ
        int rx = x0 + colW + 4;
        int ry = y0 + 22;
        g.drawString(font, Component.translatable("hukhong.profile.cores"), rx, ry, 0xFFFFD34F);
        ry += 12;
        CoreSlots slots = ClientCores.get();
        if (slots.picked().isEmpty()) {
            g.drawString(font, Component.translatable("hukhong.profile.no_cores"), rx, ry, 0xFF888888);
            ry += 12;
        }
        var picked = slots.picked().stream().sorted(Comparator.comparingInt(CoreSlots.Picked::milestone)).toList();
        for (CoreSlots.Picked pk : picked) {
            CoreDef def = CoreCardRenderer.def(pk.core());
            CoreTier t = pk.coreTier();
            Component name = Component.translatable(def != null ? def.nameKey() : "core.hukhong.unknown");
            g.drawString(font, Component.literal("#" + pk.milestone() + " ").append(name).append(" [")
                    .append(Component.translatable(t.langKey())).append("]"), rx, ry, CoreCardRenderer.textColor(t));
            ry += 10;
            for (Component c : CoreCardRenderer.describe(pk.core(), t)) {
                g.drawString(font, c, rx + 8, ry, 0xFF8FE08F);
                ry += 9;
            }
        }
        ry += 6;
        g.drawString(font, Component.translatable("hukhong.profile.systems"), rx, ry, 0xFFFFD34F);
        ry += 12;
        Map<String, Integer> counts = Minecraft.getInstance().level == null ? Map.of()
                : CoreService.systemCounts(slots, CoreCardRenderer::def);
        if (counts.isEmpty()) g.drawString(font, "-", rx, ry, 0xFF888888);
        for (var en : counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList()) {
            boolean active = CoreService.hasSetBonus(en.getKey(), en.getValue());
            g.drawString(font, Component.translatable("hukhong.system." + en.getKey())
                    .append(" " + en.getValue() + (active ? "  (2)" : "")), rx, ry, active ? 0xFF55FF55 : 0xFFBBBBBB);
            ry += 10;
        }
        g.drawCenteredString(font, Component.translatable("hukhong.profile.close"), width / 2, y0 + ph - 12, 0xFF777777);
    }

    private int stat(GuiGraphics g, int x, int y, net.minecraft.core.Holder<Attribute> attr, double value) {
        g.drawString(font, Component.translatable(attr.value().getDescriptionId()).append(": ")
                .append(String.format(Locale.ROOT, "%.2f", value)), x, y, 0xFFDDDDDD);
        return y + 10;
    }

    private static String pathLang(Path p) {
        return switch (p) {
            case KIEM -> "hukhong.path.sword";
            case DAO -> "hukhong.path.dao";
            case THE -> "hukhong.path.body";
            case MA -> "hukhong.path.demon";
        };
    }
}
