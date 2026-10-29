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

    private static final int TABS = 6, TAB_GAP = 3;
    private final List<int[]> boneRows = new ArrayList<>(); // {y, slot}
    private int tab = 0;
    private double scroll = 0;
    private int x0, y0, pw, ph, cx, cy, cw, ch; // khung + vùng nội dung
    private final List<Component> tooltip = new ArrayList<>();
    private int contentH = 0;
    // header (tính trong layout(); render và click dùng chung)
    private int cpX, cpW, loX, loW, titleX, tabY, tabW;
    private Component titleShown = Component.empty();
    private final List<int[]> artRects = new ArrayList<>(); // {x, y, w, pathOrdinal}
    private int ticks = 0, skillsBuiltAt = -1000;
    private List<SkillEntries.Page> skillPages = List.of();
    private List<SkillEntries.Entry> skillLoose = List.of();

    public ProfileScreen() { super(Component.translatable("hukhong.profile.title")); }

    /** Mở thẳng một tab (0 Chỉ số, 1 Lõi, 2 Hệ, 3 Hồn Cốt, 4 Nội Tại, 5 Kỹ Năng). */
    public ProfileScreen withTab(int t) { tab = Math.max(0, Math.min(TABS - 1, t)); return this; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    private static final int HDR_Y = 4, HDR_H = 12, ART_Y = 18, ART_H = 11;

    private void layout() {
        pw = Math.min(width - 16, 420);
        ph = Math.min(height - 16, 260);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        int left = x0 + 8, right = x0 + pw - 8;
        // hàng 1: [Công Pháp] trái, tiêu đề giữa (kẹp giữa hai nút), [Lộ Ma] phải
        cpX = left;
        cpW = font.width(t("hukhong.skillbook.button", "[Công Pháp]"));
        loW = ClientCache.cultivation().paths().contains(Path.MA) ? font.width(t("hukhong.ma.lo.button", "[Lộ Ma]")) : 0;
        loX = right - loW;
        int tl = cpX + cpW + 8, tr = (loW > 0 ? loX : right) - 8;
        int avail = Math.max(0, tr - tl);
        String ts = title.getString();
        if (font.width(ts) > avail) ts = font.plainSubstrByWidth(ts, avail);
        titleShown = Component.literal(ts);
        int tw = font.width(ts);
        titleX = Math.max(tl, Math.min(width / 2 - tw / 2, tr - tw));
        // hàng 2 (chỉ khi có pháp khí): các nút tiến hóa, co lại nếu không đủ chỗ
        artRects.clear();
        List<Path> arts = artifactPaths();
        int rowW = right - left;
        if (!arts.isEmpty()) {
            int total = -6;
            for (Path ap : arts) total += font.width(artifactLabel(ap)) + 6;
            int ax = left;
            int each = total > rowW ? (rowW - 6 * (arts.size() - 1)) / arts.size() : -1;
            for (Path ap : arts) {
                int aw = each > 0 ? each : font.width(artifactLabel(ap));
                artRects.add(new int[]{ax, y0 + ART_Y, aw, ap.ordinal()});
                ax += aw + 6;
            }
        }
        tabY = y0 + ART_Y + (arts.isEmpty() ? 0 : ART_H + 3);
        tabW = (pw - 16 - TAB_GAP * (TABS - 1)) / TABS;
        cx = x0 + 8;
        cy = tabY + TAB_H + 6;
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
            case 3 -> t("hukhong.profile.tab.bones", "Hồn Cốt");
            case 4 -> t("hukhong.profile.tab.passives", "Nội Tại");
            case 5 -> t("hukhong.profile.tab.skills", "Kỹ Năng");
            default -> t("hukhong.profile.tab.systems", "Hệ");
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        layout();
        tooltip.clear();
        g.fill(x0 - 1, y0 - 1, x0 + pw + 1, y0 + ph + 1, 0xFF555555);
        g.fill(x0, y0, x0 + pw, y0 + ph, 0xEE111119);
        int hy = y0 + HDR_Y;
        g.drawString(font, titleShown, titleX, y0 + 6, GOLD);
        Component cp = t("hukhong.skillbook.button", "[Công Pháp]");
        g.drawString(font, cp, cpX, y0 + 6, inRect(mx, my, cpX, hy, cpW, HDR_H) ? 0xFFFFFFFF : 0xFF7FD4FF);
        if (loW > 0) {
            Component lo = t("hukhong.ma.lo.button", "[Lộ Ma]");
            g.drawString(font, lo, loX, y0 + 6, inRect(mx, my, loX, hy, loW, HDR_H) ? 0xFFFFFFFF : 0xFFFF5555);
        }
        for (int[] r : artRects) { // nút tiến hóa pháp khí (Đạo/Thể/Ma/Pháp) bằng Hồn Tinh
            Path ap = Path.values()[r[3]];
            boolean ah = inRect(mx, my, r[0], r[1], r[2], ART_H);
            Component lab = artifactLabel(ap);
            if (font.width(lab) > r[2]) g.drawString(font, font.plainSubstrByWidth(lab.getString(), r[2]), r[0], r[1] + 1, ah ? 0xFFFFFFFF : ap.argb);
            else g.drawString(font, lab, r[0], r[1] + 1, ah ? 0xFFFFFFFF : ap.argb);
            if (ah) artifactTooltip(ap);
        }

        for (int i = 0; i < TABS; i++) {
            int tx = x0 + 8 + i * (tabW + TAB_GAP), ty = tabY;
            boolean sel = i == tab, hov = inRect(mx, my, tx, ty, tabW, TAB_H);
            g.fill(tx, ty, tx + tabW, ty + TAB_H, sel ? 0xFF3A3A55 : hov ? 0xFF2A2A3A : 0xFF1C1C28);
            if (sel) g.fill(tx, ty + TAB_H - 1, tx + tabW, ty + TAB_H, GOLD);
            String tn = tabName(i).getString();
            if (font.width(tn) > tabW - 2) tn = font.plainSubstrByWidth(tn, tabW - 2);
            g.drawString(font, tn, tx + (tabW - font.width(tn)) / 2, ty + 3, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
        }

        g.fill(cx - 2, cy - 2, cx + cw + 2, cy + ch + 2, 0x66000000);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - ch)));
        g.enableScissor(cx, cy, cx + cw, cy + ch);
        contentH = switch (tab) {
            case 0 -> renderStats(g);
            case 1 -> renderCores(g, mx, my);
            case 3 -> renderBones(g, mx, my);
            case 4 -> renderPassives(g);
            case 5 -> renderSkills(g);
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
        int life = ClientCache.age().lifespan(d.realmIndex());
        g.drawString(font, life == com.hukhong.age.AgeMath.UNLIMITED ? Component.translatable("hukhong.profile.lifespan.inf")
                : Component.translatable("hukhong.profile.lifespan", life), lx, y, 0xFFBBBBBB);
        y += 11;
        g.drawString(font, t("hukhong.profile.wallet", "Tử Tinh: %s",
                com.hukhong.currency.TuTinhText.of(com.hukhong.currency.Wallet.count(minecraft.player))), lx, y, 0xFFE0C080);
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
            g.fill(cx + 3, y + 3, cx + 11, y + 11, CoreCardRenderer.textColor(tier));
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

    // ------------------------------------------------------------------ tab 3 (Hồn Cốt)
    private int renderBones(GuiGraphics g, int mx, int my) {
        int top = cy - (int) scroll;
        boneRows.clear();
        var slots = com.hukhong.honcot.ClientBones.get();
        int y = top;
        for (var bs : com.hukhong.honcot.BoneSlot.values()) {
            var st = slots.get(bs);
            boolean hov = mx >= cx && mx < cx + cw && my >= Math.max(y, cy) && my < Math.min(y + ROW * 2, cy + ch);
            if (hov) g.fill(cx, y, cx + cw, y + ROW * 2, 0x33FFFFFF);
            g.drawString(font, Component.translatable(bs.langKey()), cx + 4, y + 3, 0xFF999999);
            if (st.isEmpty()) {
                g.drawString(font, t("hukhong.honcot.empty", "(trống)"), cx + 80, y + 3, GREY);
            } else {
                g.renderItem(st, cx + 62, y - 1);
                g.drawString(font, st.getHoverName(), cx + 82, y + 3, 0xFFFFFFFF);
                var bone = com.hukhong.honcot.BoneSlots.boneOf(st);
                if (bone != null) {
                    int tier = com.hukhong.honcot.HonCotItem.tierOf(st), roll = com.hukhong.honcot.HonCotItem.rollOf(st);
                    StringBuilder sb = new StringBuilder();
                    for (var stat : bone.stats) sb.append(com.hukhong.honcot.HonCotItem.statLine(stat, tier, roll).getString()).append("  ");
                    g.drawString(font, sb.toString(), cx + 82, y + 3 + ROW - 3, GREEN);
                    if (hov) {
                        tooltip.add(Component.translatable(bone.fxKey()).withStyle(net.minecraft.ChatFormatting.GREEN));
                        tooltip.add(t("hukhong.honcot.click_unequip", "Bấm để tháo").copy().withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                }
                boneRows.add(new int[]{y, bs.ordinal()});
            }
            y += ROW * 2 + 2;
        }
        y += 4;
        g.drawString(font, t("hukhong.honcot.hint_r", "Phím R: Kỹ năng Bản Mệnh (cấp 40) · Shift+R: Vạn Cốt Cộng Minh"), cx + 4, y, 0xFFBBBBBB);
        return y + ROW - top;
    }

    // ------------------------------------------------------------------ tab 4 (Nội Tại)
    private static String pc(double v) { return fmt(v * 100) + "%"; }

    /** Mô tả toàn bộ nội tại của các đạo người chơi đang có, với số liệu theo tư chất. */
    private int renderPassives(GuiGraphics g) {
        int top = cy - (int) scroll;
        CultivationData d = ClientCache.cultivation();
        int y = top;
        for (Path p : d.paths().stream().sorted().toList()) {
            Aptitude a = d.aptitude().getOrDefault(p, Aptitude.D);
            g.drawString(font, Component.translatable(pathLang(p)).append(" - ")
                    .append(Component.translatable("hukhong.aptitude." + a.name().toLowerCase(Locale.ROOT))), cx, y, p.argb);
            y += 12;
            for (Component[] e : passiveEntries(p, a, d.level())) {
                g.drawString(font, e[0], cx + 4, y, GOLD);
                y += 10;
                for (var line : font.split(e[1], cw - 12)) {
                    g.drawString(font, line, cx + 10, y, 0xFFCCCCCC);
                    y += 10;
                }
                y += 3;
            }
            y += 4;
        }
        return Math.max(y - top, ROW);
    }

    private static List<Component[]> passiveEntries(Path p, Aptitude a, int lv) {
        // Pháp: nguyên tố đọc từ attachment đã đồng bộ (CULTIVATION không phải attachment đồng bộ => không dùng PhapTu.element() ở client)
        var pl = Minecraft.getInstance().player;
        return PassiveEntries.of(p, a, lv, p == Path.PHAP && pl != null ? com.hukhong.phaptu.PhapTu.rawElement(pl) : null);
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

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void tick() { ticks++; }

    // ------------------------------------------------------------------ tab 5 (Kỹ Năng)
    private void rebuildSkills() {
        skillsBuiltAt = ticks;
        var pl = Minecraft.getInstance().player;
        if (pl == null) { skillPages = List.of(); skillLoose = List.of(); return; }
        CultivationData d = ClientCache.cultivation();
        int[] stage = new int[Path.values().length];
        var ad = pl.getData(com.hukhong.artifact.ArtifactAttachments.ARTIFACT.get());
        for (Path p : Path.values()) stage[p.ordinal()] = com.hukhong.banmenh.BanMenhMath.effectiveStage(ad.get(p), d.level());
        double ap = pl.getAttributeValue(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.ATTACK_POWER));
        double sp = pl.getAttributeValue(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.SPELL_POWER));
        var el = d.paths().contains(Path.PHAP) ? com.hukhong.phaptu.PhapTu.rawElement(pl) : null; // CULTIVATION không đồng bộ: không dùng PhapTu.element()
        var x = new SkillEntries.Ctx(d.level(), d.paths(), d.aptitude(), com.hukhong.skillbook.ClientSkillBook.get(), el, ap, sp,
                pl.getMaxHealth(), d.resourceMax(), com.hukhong.gates.Resources.costFor(d, 1), stage,
                com.hukhong.honcot.ClientBones.get().count());
        skillPages = SkillEntries.build(x);
        skillLoose = SkillEntries.unassigned(x);
    }

    private int renderSkills(GuiGraphics g) {
        if (skillsBuiltAt < 0 || ticks - skillsBuiltAt > 10) rebuildSkills();
        int top = cy - (int) scroll, y = top, tw = cw - 24;
        if (skillPages.isEmpty()) {
            g.drawString(font, t("hukhong.skills.none", "Chưa nhập đạo nên chưa có kỹ năng."), cx, y, GREY);
            return ROW;
        }
        for (SkillEntries.Page pg : skillPages) {
            g.drawString(font, pg.header(), cx, y, pg.path().argb);
            y += 11;
            g.fill(cx, y - 1, cx + cw, y, 0xFF333344);
            y += 3;
            for (SkillEntries.Entry e : pg.basic()) y = drawSkill(g, e, y, tw);
            for (SkillEntries.Entry e : pg.keys()) y = drawSkill(g, e, y, tw);
            y += 5;
        }
        if (!skillLoose.isEmpty()) {
            g.drawString(font, t("hukhong.skills.unassigned", "Bí tịch đã học (chưa gán phím)"), cx, y, 0xFF7FD4FF);
            y += 11;
            g.fill(cx, y - 1, cx + cw, y, 0xFF333344);
            y += 3;
            for (SkillEntries.Entry e : skillLoose) y = drawSkill(g, e, y, tw);
        }
        return Math.max(y - top, ROW);
    }

    private int drawSkill(GuiGraphics g, SkillEntries.Entry e, int y, int tw) {
        if (!e.key().isEmpty()) {
            g.fill(cx + 1, y - 1, cx + 15, y + 9, 0xFF2A2A3A);
            g.drawCenteredString(font, e.key().length() > 2 ? e.key().substring(0, 1) : e.key(), cx + 8, y, GOLD);
        }
        g.drawString(font, e.name(), cx + 20, y, SkillEntries.NAME);
        y += 10;
        for (SkillEntries.Line ln : e.lines()) {
            for (var seq : font.split(ln.text(), tw)) {
                g.drawString(font, seq, cx + 20, y, ln.argb());
                y += 10;
            }
        }
        return y + 4;
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        int hy = y0 + HDR_Y;
        if (loW > 0 && inRect(mx, my, loX, hy, loW, HDR_H)) {
            Minecraft.getInstance().setScreen(new com.hukhong.client.screen.ma.MaLoScreen());
            return true;
        }
        for (int[] r : artRects) {
            if (inRect(mx, my, r[0], r[1], r[2], ART_H)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.hukhong.artifact.ArtifactUpgradePayload(r[3]));
                return true;
            }
        }
        if (inRect(mx, my, cpX, hy, cpW, HDR_H)) {
            Minecraft.getInstance().setScreen(new com.hukhong.skillbook.client.CongPhapScreen());
            return true;
        }
        for (int i = 0; i < TABS; i++) {
            if (inRect(mx, my, x0 + 8 + i * (tabW + TAB_GAP), tabY, tabW, TAB_H)) {
                if (tab != i) { tab = i; scroll = 0; }
                return true;
            }
        }
        if (tab == 3 && mx >= cx && mx < cx + cw && my >= cy && my < cy + ch) {
            for (int[] r : boneRows) {
                if (my >= r[0] && my < r[0] + ROW * 2) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.hukhong.honcot.BoneUnequipPayload(r[1]));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(scroll - sy * ROW, Math.max(0, contentH - ch)));
        return true;
    }

    // ------------------------------------------------------------------ pháp khí các đạo (Hồn Tinh)
    private static List<Path> artifactPaths() {
        List<Path> l = new ArrayList<>();
        for (Path p : ClientCache.cultivation().paths().stream().sorted().toList())
            if (com.hukhong.artifact.ArtifactMath.hasArtifact(p)) l.add(p);
        return l;
    }

    private static int artifactStage(Path p) {
        var pl = Minecraft.getInstance().player;
        return pl == null ? 0 : pl.getData(com.hukhong.artifact.ArtifactAttachments.ARTIFACT.get()).get(p);
    }

    private static Component artifactLabel(Path p) {
        return Component.literal("[").append(Component.translatableWithFallback("hukhong.artifact." + p.id, artifactVi(p)))
                .append(" " + artifactStage(p) + "/" + com.hukhong.artifact.ArtifactMath.MAX_STAGE + "]");
    }

    private static String artifactVi(Path p) {
        return switch (p) { case DAO -> "Pháp Bảo"; case THE -> "Bá Thể"; case MA -> "Ma Hồn"; case PHAP -> "Pháp Khí"; default -> "?"; };
    }

    private void artifactTooltip(Path p) {
        int st = artifactStage(p);
        tooltip.add(Component.translatableWithFallback("hukhong.artifact." + p.id, artifactVi(p)).copy().withColor(p.argb & 0xFFFFFF));
        tooltip.add(Component.translatable("hukhong.banmenh.stage." + st));
        var lb = com.hukhong.linhbao.LinhBaoType.of(p);
        if (lb != null && minecraft != null && minecraft.player != null) { // chi so bao vat ban menh (Dao Linh / Linh The / Ma Cong)
            double ap = minecraft.player.getAttributeValue(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.ATTACK_POWER));
            double sp = minecraft.player.getAttributeValue(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.SPELL_POWER));
            tooltip.addAll(com.hukhong.linhbao.LinhBaoText.lines(lb, st, ap, sp));
        }
        if (st >= com.hukhong.artifact.ArtifactMath.MAX_STAGE) {
            tooltip.add(t("hukhong.artifact.evolve.max", "Pháp khí đã đạt Thần phẩm."));
        } else {
            tooltip.add(t("hukhong.artifact.tip.cost", "Tiến hóa: %s Hồn Tinh, cần cấp %s.",
                    com.hukhong.artifact.ArtifactMath.tinhCost(st + 1), com.hukhong.banmenh.BanMenhMath.STAGE_LEVELS[st + 1]).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip.add(t("hukhong.artifact.tip.click", "Bấm để tiến hóa (hoặc Shift + chuột phải bằng Hồn Tinh).").copy().withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    private static String pathLang(Path p) {
        return switch (p) {
            case KIEM -> "hukhong.path.sword";
            case DAO -> "hukhong.path.dao";
            case THE -> "hukhong.path.body";
            case MA -> "hukhong.path.demon";
            case PHAP -> "hukhong.path.phap";
        };
    }
}
