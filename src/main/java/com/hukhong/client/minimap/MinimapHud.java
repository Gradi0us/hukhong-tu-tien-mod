package com.hukhong.client.minimap;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.network.MapMarkersPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Minimap góc phải trên: địa hình từ MapCache (bắc ở trên, 1 block = 1 px), mũi tên người chơi, marker thành/tông,
 * waypoint (kẹp ở mép khi ngoài tầm) và dòng toạ độ/khoảng cách bên dưới.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MinimapHud {
    private MinimapHud() {}

    private static final int TEX = 128;
    private static final ResourceLocation TEX_RL = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "minimap_dyn");
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "minimap");

    private static DynamicTexture tex;
    private static int lastCx = Integer.MIN_VALUE, lastCz, lastVer = -1, lastSize = -1;
    private static boolean lastRound;
    private static long lastTick = Long.MIN_VALUE;
    private static boolean ready;

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(LAYER, MinimapHud::render);
    }

    /** Gọi mỗi client tick: dựng lại texture khi người chơi di chuyển/cache đổi/đổi cỡ-hình (tối đa 5 lần/giây). */
    static void refresh(Minecraft mc) {
        int s = MinimapState.sizePx();
        if (s == 0 || mc.player == null || mc.level == null) return;
        if (tex == null) {
            tex = new DynamicTexture(TEX, TEX, false);
            mc.getTextureManager().register(TEX_RL, tex);
        }
        long now = mc.level.getGameTime();
        int cx = mc.player.blockPosition().getX(), cz = mc.player.blockPosition().getZ();
        boolean round = ClientConfig.minimapShape() == ClientConfig.MinimapShape.ROUND;
        boolean dirty = cx != lastCx || cz != lastCz || MapCache.version() != lastVer || s != lastSize || round != lastRound;
        if (!dirty && now - lastTick < 20) return;
        if (now - lastTick < 4 && s == lastSize && round == lastRound) return;
        var img = tex.getPixels();
        if (img == null) return;
        MapCache.render(img, cx, cz, 1, round ? s : 0);
        tex.upload();
        lastCx = cx; lastCz = cz; lastVer = MapCache.version(); lastSize = s; lastRound = round; lastTick = now;
        ready = true;
    }

    private static int effectsOffset(Minecraft mc) {
        boolean ben = false, harm = false;
        for (MobEffectInstance m : mc.player.getActiveEffects()) {
            if (!m.getEffect().value().isBeneficial()) harm = true; else ben = true;
        }
        return harm ? 52 : ben ? 26 : 0;
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.minimapOn() || mc.player == null || mc.level == null || mc.options.hideGui) return;
        int s = MinimapState.sizePx();
        if (s == 0 || !ready) return;
        boolean round = ClientConfig.minimapShape() == ClientConfig.MinimapShape.ROUND;
        Font font = mc.font;
        int bx = g.guiWidth() - s - 9, by = 9 + effectsOffset(mc);
        int cx = bx + s / 2, cy = by + s / 2, r = s / 2;
        long tick = mc.level.getGameTime();

        // khung
        if (round) {
            MapIcons.fillCircle(g, cx, cy, r + 3, MapIcons.INK);
            MapIcons.fillCircle(g, cx, cy, r + 2, MapIcons.GOLD_DK);
            MapIcons.fillCircle(g, cx, cy, r + 1, MapIcons.INK);
        } else {
            g.fill(bx - 3, by - 3, bx + s + 3, by + s + 3, MapIcons.INK);
            g.fill(bx - 2, by - 2, bx + s + 2, by + s + 2, MapIcons.GOLD_DK);
            g.fill(bx - 1, by - 1, bx + s + 1, by + s + 1, MapIcons.INK);
        }
        // địa hình (cắt vùng giữa texture)
        int u = (TEX - s) / 2;
        g.blit(TEX_RL, bx, by, s, s, u, u, s, s, TEX, TEX);

        // ping: vòng sóng mở rộng quanh người chơi
        long pt = tick - MinimapState.pingStart();
        if (pt >= 0 && pt < 24) {
            int a = (int) (200 * (1f - pt / 24f));
            MapIcons.outline(g, cx, cy, Math.max(2, (int) (r * pt / 24f)), (a << 24) | 0xE8C060);
        }

        // marker thành/tông
        double px = Math.floor(mc.player.getX()), pz = Math.floor(mc.player.getZ());
        int lim = r - 4;
        if (ClientConfig.minimapMobs()) drawMobs(g, mc, cx, cy, px, pz, lim, round);
        if (MinimapState.markersVisible()) {
            for (MapMarkersPayload.Marker m : MinimapState.markers()) {
                boolean wp = MinimapState.isWaypoint(m);
                double dx = m.x() - px, dz = m.z() - pz;
                if (inside(dx, dz, lim, round)) {
                    int ix = cx + (int) Math.round(dx), iy = cy + (int) Math.round(dz);
                    if (m.kind() == MapMarkersPayload.GUILD) MapIcons.guild(g, ix, iy, wp);
                    else if (MinimapState.isPort(m)) MapIcons.port(g, ix, iy, m.built(), wp);
                    else if (m.kind() == MapMarkersPayload.CITY) MapIcons.city(g, ix, iy, m.built(), wp);
                    else MapIcons.sect(g, ix, iy, m.built(), wp);
                } else if (dx * dx + dz * dz <= (double) MinimapState.DISCOVER * MinimapState.DISCOVER) { // ngoài tầm: mũi tên ở mép + khoảng cách
                    int[] e = edge(dx, dz, lim, round);
                    int col = MinimapState.color(m);
                    float ang = (float) Math.toDegrees(Math.atan2(dx, -dz));
                    MapIcons.arrow(g, cx + e[0], cy + e[1], ang, col, MapIcons.INK);
                    String dtxt = MinimapState.distText((int) Math.sqrt(dx * dx + dz * dz));
                    float sc = 0.5f;
                    int tw = (int) (font.width(dtxt) * sc);
                    int tx = cx + e[0] - tw / 2 - (int) Math.round(Math.signum(e[0]) * (tw / 2 + 5) * Math.abs(e[0]) / Math.max(1, lim));
                    int ty2 = cy + e[1] - 2 - (int) Math.round(Math.signum(e[1]) * 7 * Math.abs(e[1]) / (double) Math.max(1, lim));
                    g.pose().pushPose();
                    g.pose().translate(tx, ty2, 0);
                    g.pose().scale(sc, sc, 1f);
                    g.drawString(font, dtxt, 0, 0, col, true);
                    g.pose().popPose();
                }
            }
        }
        // waypoint
        var w = MinimapState.waypoint();
        double wdx = 0, wdz = 0;
        if (w != null) {
            wdx = w.x() - px; wdz = w.z() - pz;
            if (inside(wdx, wdz, lim, round)) {
                MapIcons.waypoint(g, cx + (int) Math.round(wdx), cy + (int) Math.round(wdz), tick);
            } else {
                int[] e = edge(wdx, wdz, lim, round);
                MapIcons.arrow(g, cx + e[0], cy + e[1], (float) Math.toDegrees(Math.atan2(wdx, -wdz)), MapIcons.RED, MapIcons.INK);
            }
        }
        // mục tiêu chính: kim cương vàng / mũi tên vàng ở mép
        var obj = com.hukhong.client.objective.ClientObjective.sameDimTarget();
        double odx = 0, odz = 0;
        if (obj != null) {
            odx = obj.getX() + 0.5 - px; odz = obj.getZ() + 0.5 - pz;
            if (inside(odx, odz, lim, round)) {
                MapIcons.objective(g, cx + (int) Math.round(odx), cy + (int) Math.round(odz), tick);
            } else {
                int[] e = edge(odx, odz, lim, round);
                MapIcons.arrow(g, cx + e[0], cy + e[1], (float) Math.toDegrees(Math.atan2(odx, -odz)), MapIcons.GOLD, MapIcons.INK);
            }
        }
        // người chơi
        MapIcons.arrow(g, cx, cy, mc.player.getYRot() + 180f, MapIcons.WHITE, MapIcons.INK);
        // mốc bắc + 4 chấm trang trí
        g.pose().pushPose();
        g.pose().translate(cx, by + 2, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(font, "N", -3, 0, 0xFFE8C060, true);
        g.pose().popPose();
        if (round) {
            g.fill(cx - 1, by - 3, cx + 1, by - 1, MapIcons.GOLD);
            g.fill(cx - 1, by + s + 1, cx + 1, by + s + 3, MapIcons.GOLD);
            g.fill(bx - 3, cy - 1, bx - 1, cy + 1, MapIcons.GOLD);
            g.fill(bx + s + 1, cy - 1, bx + s + 3, cy + 1, MapIcons.GOLD);
        } else {
            for (int[] c : new int[][]{{bx - 3, by - 3}, {bx + s + 1, by - 3}, {bx - 3, by + s + 1}, {bx + s + 1, by + s + 1}})
                g.fill(c[0], c[1], c[0] + 2, c[1] + 2, MapIcons.GOLD);
        }

        // dòng chữ dưới: toạ độ + waypoint
        int right = g.guiWidth() - 9;
        int ty = by + s + 6;
        String pos = mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ();
        text(g, font, pos, right, ty, 0xFFE0D4B0);
        if (w != null) {
            ty += 9;
            int dist = (int) Math.sqrt(wdx * wdx + wdz * wdz);
            String label = w.label().getString() + " " + (dist >= 1000 ? String.format("%.1fkm", dist / 1000.0) : dist + "m");
            int tw = (int) (font.width(label) * 0.75f);
            float rel = (float) (Math.toDegrees(Math.atan2(wdx, -wdz)) - (mc.player.getYRot() + 180f));
            MapIcons.arrow(g, right - tw - 8, ty + 3, rel, MapIcons.RED, MapIcons.INK);
            text(g, font, label, right, ty, 0xFFFF9A8A);
        }
        if (obj != null && w == null) { // mục tiêu chính là waypoint đang hoạt động khi không có ping B
            ty += 9;
            String label = "◆ " + com.hukhong.client.objective.ClientObjective.data().text().getString() + " " + MinimapState.distText((int) Math.sqrt(odx * odx + odz * odz));
            text(g, font, label, right, ty, MapIcons.GOLD);
        }
        // danh sách tự phát hiện: thành/cảng/tông trong 1000 block, gần nhất trước
        if (MinimapState.markersVisible()) {
            int shown = 0;
            for (MapMarkersPayload.Marker m : MinimapState.sortedByDistance(px, pz)) {
                double dd = Math.sqrt(MinimapState.dist2(m, px, pz));
                if (dd > MinimapState.DISCOVER) break;
                if (shown++ >= 5) break;
                ty += 9;
                text(g, font, Component.translatable(m.nameKey()).getString() + " " + MinimapState.distText((int) dd), right, ty, MinimapState.color(m));
            }
        }
    }

    private static void text(GuiGraphics g, Font font, String s, int right, int y, int color) {
        float sc = 0.75f;
        g.pose().pushPose();
        g.pose().translate(right - font.width(s) * sc, y, 0);
        g.pose().scale(sc, sc, 1f);
        g.drawString(font, s, 0, 0, color, true);
        g.pose().popPose();
    }

    private static boolean inside(double dx, double dz, int lim, boolean round) {
        return round ? dx * dx + dz * dz <= (double) lim * lim : Math.abs(dx) <= lim && Math.abs(dz) <= lim;
    }

    /** Điểm trên mép (offset từ tâm) theo hướng (dx,dz). */
    private static int[] edge(double dx, double dz, int lim, boolean round) {
        double k = round ? Math.sqrt(dx * dx + dz * dz) : Math.max(Math.abs(dx), Math.abs(dz));
        if (k < 1e-6) return new int[]{0, 0};
        return new int[]{(int) Math.round(dx / k * lim), (int) Math.round(dz / k * lim)};
    }

    /** Quái thù địch gần (chấm đỏ; boss = đầu lâu to, vẽ sau cùng). Chỉ dữ liệu client đã biết, tối đa 64 chấm. */
    private static void drawMobs(GuiGraphics g, Minecraft mc, int cx, int cy, double px, double pz, int lim, boolean round) {
        java.util.List<int[]> bosses = new java.util.ArrayList<>();
        int n = 0;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le) || !(e instanceof net.minecraft.world.entity.monster.Enemy)
                    || !le.isAlive() || le.isInvisible() || Math.abs(le.getY() - mc.player.getY()) > 48) continue;
            double dx = le.getX() - px, dz = le.getZ() - pz;
            if (!inside(dx, dz, lim, round)) continue;
            int ix = cx + (int) Math.round(dx), iy = cy + (int) Math.round(dz);
            boolean isBoss = le.getMaxHealth() >= 100f || le instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                    || le instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
            if (isBoss) bosses.add(new int[]{ix, iy});
            else { MapIcons.mobDot(g, ix, iy); if (++n >= 64) break; }
        }
        for (int[] b : bosses) MapIcons.bossSkull(g, b[0], b[1]);
    }
}
