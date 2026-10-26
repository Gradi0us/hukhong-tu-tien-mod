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
        Path sp = Skill.skillPath(d);
        if (sp == null) return;

        int x0 = g.guiWidth() / 2 + 91 + 34;
        int y = g.guiHeight() - SIZE - 2;
        boolean stance = ClientSkillState.stance();

        g.drawString(mc.font, Component.translatable(stance ? "hukhong.hud.stance_on" : "hukhong.hud.stance_off"),
                x0, y - 11, stance ? 0xFFFFD34F : 0xFF888888, true);

        for (int i = 0; i < 4; i++) {
            int x = x0 + i * (SIZE + GAP);
            Skill s = Skill.of(sp, i);
            g.fill(x, y, x + SIZE, y + SIZE, stance ? 0xCC000000 : 0x88000000);
            ResourceLocation icon = tex(s == null ? "locked" : s.icon);
            if (exists(icon)) g.blit(icon, x + 2, y + 2, 0, 0, 16, 16, 16, 16);
            else g.fill(x + 2, y + 2, x + 18, y + 18, s == null ? 0xFF444444 : (sp.argb & 0x00FFFFFF) | 0xFF000000);
            if (s != null) {
                int rem = ClientSkillState.remaining(i);
                if (rem > 0) {
                    int h = Math.max(1, 16 * rem / ClientSkillState.total(i));
                    g.fill(x + 2, y + 2, x + 18, y + 2 + h, 0xBB000000);
                    String sec = String.valueOf((rem + 19) / 20);
                    g.drawString(mc.font, sec, x + (SIZE - mc.font.width(sec)) / 2, y + 6, 0xFFFFFFFF, true);
                } else if (d.resource() < Resources.costFor(d, s.cost)) {
                    g.fill(x + 2, y + 2, x + 18, y + 18, 0x66FF0000);
                }
            }
            if (!stance) g.fill(x, y, x + SIZE, y + SIZE, 0x66000000);
            String key = SkillKeys.SKILL[i].getTranslatedKeyMessage().getString();
            g.drawString(mc.font, key, x + 2, y + 11, 0xFFE0E0E0, true);
        }
    }
}
