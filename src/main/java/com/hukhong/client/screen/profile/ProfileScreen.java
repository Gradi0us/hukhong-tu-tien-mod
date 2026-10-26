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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bảng tu sĩ (phím K): 3 tab Chỉ số / Lõi / Hệ. Chỉ đọc. Không blur nền. */
public class ProfileScreen extends Screen {
    private static final int GOLD = 0xFFFFD34F, GREEN = 0xFF55FF55, GREY = 0xFF888888;
    private static final int ROW = 14, TAB_H = 14;

    private int tab = 0;
    private double scroll = 0;
    private int x0, y0, pw, ph, cx, cy, cw, ch; // khung + vùng nội dung
    private final List<Component> tooltip = new ArrayList<>();
    private int contentH = 0;

    public ProfileScreen() { super(Component.translatable("hukhong.profile.title")); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    private void layout() {
        pw = Math.min(width - 16, 420);
        ph = Math.min(height - 16, 260);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        cx = x0 + 8;
        cy = y0 + 22 + TAB_H + 6;
        cw = pw - 16;
        ch = y0 + ph - 18 - cy;
    }

    private static Component t(String key, String vi, Object... a) {
        return Component.translatableWithFallback(key, vi, a);
    }

    private Component tabName(int i) {
        return switch (i) {
            case 0 -> t("hukhong.profile.tab.stats", "Chỉ số");
            case 1 -> t("hukhong.profile.tab.cores", "Lõi");
            default -> t("hukhong.profile.tab.systems", "Hệ");
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        layout();
        tooltip.clear();
        g.fill(x0 - 1, y0 - 1, x0 + pw + 1, y0 + ph + 1, 0xFF555555);
        g.fill(x0, y0, x0 + pw, y0 + ph, 0xEE111119);
        g.drawCenteredString(font, title, width / 2, y0 + 6, GOLD);

        int tw = pw / 3 - 6;
        for (int i = 0; i < 3; i++) {
            int tx = x0 + 8 + i * (tw + 6), ty = y0 + 20;
            boolean sel = i == tab, hov = mx >= tx && mx < tx + tw && my >= ty && my < ty + TAB_H;
            g.fill(tx, ty, tx + tw, ty + TAB_H, sel ? 0xFF3A3A55 : hov ? 0xFF2A2A3A : 0xFF1C1C28);
            if (sel) g.fill(tx, ty + TAB_H - 1, tx + tw, ty + TAB_H, GOLD);
            g.drawCenteredString(font, tabName(i), tx + tw / 2, ty + 3, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
        }

        g.fill(cx - 2, cy - 2, cx + cw + 2, cy + ch + 2, 0x66000000);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - ch)));
        g.enableScissor(cx, cy, cx + cw, cy + ch);
        contentH = switch (tab) {
            case 0 -> renderStats(g);
            case 1 -> renderCores(g, mx, my);
            default -> renderSystems(g);
        };
        g.disableScissor();
        if (contentH > ch) { // thanh cuộn
            int bh = Math.max(12, ch * ch / contentH);
            int by = cy + (int) ((ch - bh) * (scroll / (contentH - ch)));
            g.fill(cx + cw + 3, cy, cx + cw + 5, cy + ch, 0xFF222233);
            g.fill(cx + cw + 3, by, cx + cw + 5, by + bh, 0xFFAAAAAA);
        }

        g.fill(x0 + 4, y0 + ph - 16, x0 + pw - 4, y0 + ph - 15, 0xFF333344);
        g.drawCenteredString(font, Component.translatable("hukhong.profile.close"), width / 2, y0 + ph - 11, 0xFF777777);

        if (!tooltip.isEmpty()) g.renderComponentTooltip(font, tooltip, mx, my);
    }

    // ------------------------------------------------------------------ tab 0
    private int renderStats(GuiGraphics g) {
        int top = cy - (int) scroll;
        CultivationData d = ClientCache.cultivation();
        int half = cw / 2 - 4;
        int lx = cx, rx = cx + half + 8;
        int y = top;
        g.drawString(font, Component.translatable("hukhong.hud.level", d.level()).append("  ")
                .append(Component.translatable("hukhong.realm." + d.realmIndex())), lx, y, 0xFFFFFFFF);
        y += 12;
        g.drawString(font, Component.translatable("hukhong.profile.exp", d.exp(),
                CultivationService.barSize(d.level())), lx, y, 0xFFBBBBBB);
        y += 12;
        if (CultivationService.inLightBottleneck(d)) {
            for (var line : font.split(Component.translatable("hukhong.profile.light_bottleneck", d.level() + 1), half)) {
                g.drawString(font, line, lx, y, 0xFFFFB347);
                y += 10;
            }
        }
        if (d.milestoneLocked()) {
            for (var line : font.split(Component.translatable("hukhong.profile.milestone_locked"), half)) {
                g.drawString(font, line, lx, y, 0xFFFF8855);
                y += 10;
            }
        }
        y += 4;
        for (Path p : d.paths().stream().sorted().toList()) {
            Aptitude a = d.aptitude().get(p);
            g.drawString(font, Component.translatable(pathLang(p)).append(": ")
                    .append(a == null ? Component.literal("?") : Component.translatable("hukhong.aptitude." + a.name().toLowerCase(Locale.ROOT))),
                    lx, y, p.argb);
            y += 11;
        }
        y += 4;
        g.drawString(font, t("hukhong.profile.age", "Tuổi: %s", ClientCache.age().ageYears()), lx, y, 0xFFBBBBBB);
        y += 11;
        int leftEnd = y;

        // cột phải: bảng chỉ số
        int ry = top;
        if (Minecraft.getInstance().player != null) {
            Map<ResourceLocation, double[]> bonus = coreBonus();
            ry = stat(g, rx, ry, half, "vanilla_health", "Sinh Lực", Attributes.MAX_HEALTH, 1, false, bonus,
                    "minecraft:generic.max_health");
            ry = statId(g, rx, ry, half, "attack_power", "Công", "attack_power", 1, false, bonus);
            ry = statId(g, rx, ry, half, "defense", "Phòng", "defense", 1, false, bonus);
            ry = statId(g, rx, ry, half, "spell_power", "Thần Thức", "spell_power", 1, false, bonus);
            ry = statId(g, rx, ry, half, "crit_chance", "Bạo Kích", "crit_chance", 100, true, bonus);
            ry = statId(g, rx, ry, half, "crit_damage", "Bạo Thương", "crit_damage", 100, true, bonus);
            ry = statId(g, rx, ry, half, "lifesteal", "Hấp Huyết", "lifesteal", 100, true, bonus);
            ry = statId(g, rx, ry, half, "resource_regen", "Hồi Linh", "resource_regen", 1, false, bonus);
            ry = statId(g, rx, ry, half, "max_resource", "Linh Lực Tối Đa", "max_resource", 1, false, bonus);
            ry = stat(g, rx, ry, half, "vanilla_speed", "Tốc độ", Attributes.MOVEMENT_SPEED, 10, false, bonus,
                    "minecraft:generic.movement_speed");
        }
        return Math.max(leftEnd, ry) - top;
    }

    private int statId(GuiGraphics g, int x, int y, int w, String key, String vi, String path, double mul, boolean pct,
                       Map<ResourceLocation, double[]> bonus) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hukhong", path);
        var h = BuiltInRegistries.ATTRIBUTE.getHolder(id);
        if (h.isEmpty()) return y; // chưa đăng ký -> ẩn dòng
        return stat(g, x, y, w, key, vi, h.get(), mul, pct, bonus, id.toString());
    }

    private int stat(GuiGraphics g, int x, int y, int w, String key, String vi, Holder<Attribute> attr, double mul,
                     boolean pct, Map<ResourceLocation, double[]> bonus, String idStr) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.getAttribute(attr) == null) return y;
        double v = player.getAttributeValue(attr) * mul;
        String val = fmt(v) + (pct ? "%" : "");
        g.drawString(font, t("hukhong.profile.stat." + key, vi), x, y, 0xFFDDDDDD);
        int vw = font.width(val);
        String bs = "";
        double[] b = bonus.get(ResourceLocation.parse(idStr));
        if (b != null && (b[0] != 0 || b[1] != 0)) {
            if (b[0] != 0) bs += "+" + fmt(b[0] * mul) + (pct ? "%" : "");
            if (b[1] != 0) bs += (bs.isEmpty() ? "" : " ") + "+" + fmt(b[1] * 100) + "%";
        }
        int bw = bs.isEmpty() ? 0 : font.width(bs) + 4;
        int right = x + w;
        if (!bs.isEmpty()) g.drawString(font, bs, right - font.width(bs), y, GREEN);
        g.drawString(font, val, right - bw - vw, y, 0xFFFFFFFF);
        return y + 11;
    }

    /** attribute id -> {cộng thẳng, nhân phần trăm} từ passive Lõi. */
    private Map<ResourceLocation, double[]> coreBonus() {
        Map<ResourceLocation, double[]> m = new HashMap<>();
        for (CoreSlots.Picked pk : ClientCores.get().picked()) {
            CoreDef def = CoreCardRenderer.def(pk.core());
            if (def == null) continue;
            for (CoreDef.Passive p : def.passives()) {
                double[] a = m.computeIfAbsent(p.attribute(), k -> new double[2]);
                double v = p.valueAt(pk.coreTier());
                if (p.operation() == AttributeModifier.Operation.ADD_VALUE) a[0] += v; else a[1] += v;
            }
        }
        return m;
    }

    private static String fmt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    // ------------------------------------------------------------------ tab 1
    private int renderCores(GuiGraphics g, int mx, int my) {
        int top = cy - (int) scroll;
        CoreSlots slots = ClientCores.get();
        if (slots.picked().isEmpty()) {
            g.drawString(font, Component.translatable("hukhong.profile.no_cores"), cx, top, GREY);
            return ROW;
        }
        var picked = slots.picked().stream().sorted(Comparator.comparingInt(CoreSlots.Picked::milestone)).toList();
        int y = top;
        for (CoreSlots.Picked pk : picked) {
            CoreDef def = CoreCardRenderer.def(pk.core());
            CoreTier tier = pk.coreTier();
            boolean hov = mx >= cx && mx < cx + cw && my >= Math.max(y, cy) && my < Math.min(y + ROW, cy + ch);
            if (hov) g.fill(cx, y, cx + cw, y + ROW, 0x33FFFFFF);
            g.fill(cx + 2, y + 2, cx + 12, y + 12, 0xFF000000);
            g.fill(cx + 3, y + 3, cx + 11, y + 11, 0xFF000000 | tier.rgb);
            Component name = Component.translatable(def != null ? def.nameKey() : "core.hukhong.unknown");
            g.drawString(font, "Lv" + com.hukhong.cores.CoreService.milestoneLevel(pk.milestone()), cx + 16, y + 3, 0xFF999999);
            g.drawString(font, name, cx + 40, y + 3, CoreCardRenderer.textColor(tier));
            Component tn = Component.translatable(tier.langKey());
            g.drawString(font, tn, cx + cw - font.width(tn) - 4, y + 3, CoreCardRenderer.textColor(tier));
            if (hov) {
                tooltip.add(name.copy().withColor(CoreCardRenderer.textColor(tier) & 0xFFFFFF));
                tooltip.add(tn);
                for (Component c : CoreCardRenderer.describe(pk.core(), tier)) tooltip.add(c.copy().withColor(0x8FE08F));
            }
            y += ROW;
        }
        return y - top;
    }

    // ------------------------------------------------------------------ tab 2
    private int renderSystems(GuiGraphics g) {
        int top = cy - (int) scroll;
        CoreSlots slots = ClientCores.get();
        Map<String, Integer> counts = Minecraft.getInstance().level == null ? Map.of()
                : CoreService.systemCounts(slots, CoreCardRenderer::def);
        if (counts.isEmpty()) {
            g.drawString(font, "-", cx, top, GREY);
            return ROW;
        }
        int y = top;
        for (var en : counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey())).toList()) {
            boolean active = CoreService.hasSetBonus(en.getKey(), en.getValue());
            g.drawString(font, Component.translatable("hukhong.system." + en.getKey()), cx + 4, y + 3,
                    active ? GREEN : 0xFFBBBBBB);
            g.drawString(font, en.getValue() + " " + t("hukhong.profile.pieces", "món").getString(), cx + 110, y + 3, 0xFFDDDDDD);
            Component st = active ? t("hukhong.profile.set_on", "Thưởng 2 món: ĐẠT")
                    : t("hukhong.profile.set_off", "Thưởng 2 món: chưa đạt");
            g.drawString(font, st, cx + cw - font.width(st) - 4, y + 3, active ? GREEN : GREY);
            y += ROW;
        }
        return y - top;
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        int tw = pw / 3 - 6;
        for (int i = 0; i < 3; i++) {
            int tx = x0 + 8 + i * (tw + 6), ty = y0 + 20;
            if (mx >= tx && mx < tx + tw && my >= ty && my < ty + TAB_H) {
                if (tab != i) { tab = i; scroll = 0; }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(scroll - sy * ROW, Math.max(0, contentH - ch)));
        return true;
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
