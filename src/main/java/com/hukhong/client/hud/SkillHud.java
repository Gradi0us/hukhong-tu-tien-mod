package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientCache;
import com.hukhong.client.keybind.ClientSkillState;
import com.hukhong.client.keybind.SkillKeys;
import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import com.hukhong.skill.Skill;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.HashMap;
import java.util.Map;

/** 4 ô kỹ năng + hồi chiêu cạnh hotbar (bên phải) + chỉ báo Thế Tu Luyện. Icon thiếu -> ô màu. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class SkillHud {
    private SkillHud() {}

    private static final int SIZE = 20, GAP = 2;
    private static final Map<ResourceLocation, Boolean> EXISTS = new HashMap<>();

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/gui/skill/" + name + ".png");
    }

    private static boolean exists(ResourceLocation rl) {
        return EXISTS.computeIfAbsent(rl, k -> Minecraft.getInstance().getResourceManager().getResource(k).isPresent());
    }

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "skill_hud"), SkillHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        if (!com.hukhong.client.ClientConfig.showHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        CultivationData d = ClientCache.cultivation();
        Path sp = Skill.skillPath(d, ClientSkillState.page());
        if (sp == null) return;

        int x0 = g.guiWidth() / 2 + 91 + 34;
        int y = g.guiHeight() - SIZE - 2;
        boolean stance = ClientSkillState.stance();

        g.drawString(mc.font, Component.translatable(stance ? "hukhong.hud.stance_on" : "hukhong.hud.stance_off"),
                x0, y - 11, stance ? 0xFFFFD34F : 0xFF888888, true);

        // Đa đạo: tên trang (màu đạo) phía trên cùng hàng chữ thế; mỗi đạo 1 thanh mảnh dưới ô kỹ năng.
        java.util.List<Path> owned = com.hukhong.skill.SkillPages.owned(d.paths());
        if (owned.size() > 1) {
            String pn = Component.translatable(com.hukhong.skill.SkillPages.pathLangKey(sp)).getString()
                    + " [" + SkillKeys.PAGE.getTranslatedKeyMessage().getString() + "]";
            g.drawString(mc.font, pn, x0 + 4 * (SIZE + GAP) - mc.font.width(pn), y - 11, sp.argb, true);
            double ratio = (double) d.resource() / d.resourceMax();
            int bw = (4 * (SIZE + GAP) - GAP - (owned.size() - 1) * 2) / owned.size();
            for (int k = 0; k < owned.size(); k++) {
                int bx = x0 + k * (bw + 2), by = y + SIZE + 1;
                g.fill(bx, by, bx + bw, by + 2, 0xAA000000);
                g.fill(bx, by, bx + (int) Math.round(bw * Math.max(0, Math.min(1, ratio))), by + 2, owned.get(k).argb);
                if (owned.get(k) == sp) g.fill(bx, by + 2, bx + bw, by + 3, 0xFFFFFFFF);
            }
        }

        boolean lockT = sp == Path.PHAP && com.hukhong.phaptu.beam.PhapBeamMath.locksT(com.hukhong.phaptu.PhapTu.element(mc.player));
        for (int i = 0; i < 4; i++) {
            int x = x0 + i * (SIZE + GAP);
            com.hukhong.skillbook.SkillBook.Resolved rs = com.hukhong.skillbook.SkillBook.resolve(com.hukhong.skillbook.ClientSkillBook.get(), sp, i, d.level(), lockT);
            Skill s = rs != null ? rs.base() : null;
            g.fill(x, y, x + SIZE, y + SIZE, stance ? 0xCC000000 : 0x88000000);
            com.hukhong.skill.BanMenhSkill bm = i == 2 && rs == null ? com.hukhong.skill.BanMenhSkill.of(sp, d.level()) : null;
            boolean bones = i == 2 && bm == null && com.hukhong.honcot.ClientBones.get().count() > 0;
            boolean has = rs != null || bm != null || bones;
            boolean dome = rs != null && rs.base() == Skill.KIEM_ULT; // Kiếm Vực: kênh bật/tắt
            boolean domeOn = dome && ClientSkillState.domain();
            int baseCost = rs != null ? rs.cost() : bm != null ? bm.cost : 50;
            ResourceLocation icon = tex(com.hukhong.client.phaptu.PhapClient.icon(rs != null ? rs.icon() : bm != null ? bm.icon : bones ? "bm_bone" : "locked"));
            if (exists(icon)) g.blit(icon, x + 2, y + 2, 0, 0, 16, 16, 16, 16);
            else g.fill(x + 2, y + 2, x + 18, y + 18, rs == null ? 0xFF444444 : (sp.argb & 0x00FFFFFF) | 0xFF000000);
            if (has) {
                int rem = ClientSkillState.remaining(i);
                if (rem > 0) {
                    int h = Math.max(1, 16 * rem / ClientSkillState.total(i));
                    g.fill(x + 2, y + 2, x + 18, y + 2 + h, 0xBB000000);
                    String sec = String.valueOf((rem + 19) / 20);
                    g.drawString(mc.font, sec, x + (SIZE - mc.font.width(sec)) / 2, y + 6, 0xFFFFFFFF, true);
                } else if (lockT && i == 3 ? d.resource() < com.hukhong.phaptu.beam.PhapBeamMath.upfrontCost(d.resourceMax(), 1.0)
                        : dome ? d.resource() < Math.max(1, Math.round(d.resourceMax() * com.hukhong.skill.SkillEffects.DOMAIN_DRAIN_PCT))
                        : d.resource() < Resources.costFor(d, baseCost)) {
                    g.fill(x + 2, y + 2, x + 18, y + 18, 0x66FF0000);
                }
            }
            if (domeOn) { // đang bật: viền xanh nhạt + chữ ON
                int bc = 0xFF7FD8FF;
                g.fill(x, y, x + SIZE, y + 1, bc); g.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, bc);
                g.fill(x, y, x + 1, y + SIZE, bc); g.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, bc);
                g.fill(x + 2, y + 2, x + 18, y + 18, 0x337FD8FF);
                g.drawString(mc.font, "ON", x + (SIZE - mc.font.width("ON")) / 2, y + 2, 0xFFBFEFFF, true);
            }
            if (lockT && i == 3) com.hukhong.client.fx.PhapBeamClient.drawHud(g, mc, x, y, SIZE);
            if (!stance) g.fill(x, y, x + SIZE, y + SIZE, 0x66000000);
            String key = SkillKeys.SKILL[i].getTranslatedKeyMessage().getString();
            g.drawString(mc.font, key, x + 2, y + 11, 0xFFE0E0E0, true);
        }
    }
}
