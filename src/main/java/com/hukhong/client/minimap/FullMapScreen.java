package com.hukhong.client.minimap;

import com.hukhong.HuKhong;
import com.hukhong.network.MapMarkersPayload;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Bản đồ lớn: vùng đã quét (MapCache), cuộn chuột để thu phóng, kéo chuột trái để di chuyển, bấm marker/danh sách để đặt
 * waypoint, chuột phải để đặt điểm bất kỳ. Danh sách thành/tông bên trái (kể cả nơi xa ngoài vùng đã quét).
 */
public final class FullMapScreen extends Screen {
    private static final int TEX = 256;
    private static final int[] STEPS = {1, 2, 4, 8, 16};
    private static final ResourceLocation TEX_RL = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "fullmap_dyn");
    private static DynamicTexture tex;

    private int stepIdx = 1;
    private double cx, cz;
    private int texCx = Integer.MIN_VALUE, texCz, texStep, texVer = -1;
    private long lastBuild;
    private int mapX, mapY, mapSize;
    private int listW;
    private boolean dragging;
    private double dragMoved;

    public FullMapScreen() {
        super(Component.translatable("hukhong.map.title"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) { cx = mc.player.getX(); cz = mc.player.getZ(); }
    }

    @Override
    protected void init() {
        mapSize = Math.max(128, Math.min(height - 40, width - 180));
        mapX = width - mapSize - 14;
        mapY = (height - mapSize) / 2 + 4;
        listW = Math.max(100, mapX - 24);
        if (tex == null) {
            tex = new DynamicTexture(TEX, TEX, false);
            minecraft.getTextureManager().register(TEX_RL, tex);
        }
        texCx = Integer.MIN_VALUE;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xE0100A16);
    }

    private int step() { return STEPS[stepIdx]; }
    private float scale() { return mapSize / (float) TEX; }
    private int sx(double bx) { return mapX + mapSize / 2 + Math.round((float) ((bx - texCx) / texStep * scale())); }
    private int sy(double bz) { return mapY + mapSize / 2 + Math.round((float) ((bz - texCz) / texStep * scale())); }

    private void rebuild() {
        NativeImage img = tex.getPixels();
        if (img == null) return;
        texCx = (int) Math.floor(cx);
        texCz = (int) Math.floor(cz);
        texStep = step();
        MapCache.render(img, texCx, texCz, texStep, 0);
        tex.upload();
        texVer = MapCache.version();
        lastBuild = minecraft.level != null ? minecraft.level.getGameTime() : 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        long now = minecraft.level != null ? minecraft.level.getGameTime() : 0;
        boolean moved = texCx != (int) Math.floor(cx) || texCz != (int) Math.floor(cz) || texStep != step();
        if (moved || (texVer != MapCache.version() && now - lastBuild >= 10)) rebuild();

        Font font = minecraft.font;
        g.drawCenteredString(font, title, mapX + mapSize / 2, mapY - 14, 0xFFE8C060);
        // khung + bản đồ
        g.fill(mapX - 3, mapY - 3, mapX + mapSize + 3, mapY + mapSize + 3, MapIcons.GOLD_DK);
        g.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, MapIcons.INK);
        g.blit(TEX_RL, mapX, mapY, mapSize, mapSize, 0, 0, TEX, TEX, TEX, TEX);
        g.enableScissor(mapX, mapY, mapX + mapSize, mapY + mapSize);

        var player = minecraft.player;
        MapMarkersPayload.Marker hover = null;
        boolean vis = MinimapState.markersVisible();
        if (vis) {
            int best = 7 * 7;
            for (MapMarkersPayload.Marker m : MinimapState.markers()) {
                int ix = sx(m.x()), iy = sy(m.z());
                int d = (mx - ix) * (mx - ix) + (my - iy) * (my - iy);
                if (d <= best) { best = d; hover = m; }
            }
            for (MapMarkersPayload.Marker m : MinimapState.markers()) {
                int ix = sx(m.x()), iy = sy(m.z());
                if (ix < mapX - 8 || iy < mapY - 8 || ix > mapX + mapSize + 8 || iy > mapY + mapSize + 8) continue;
                boolean hot = m == hover || MinimapState.isWaypoint(m);
                if (m.kind() == MapMarkersPayload.GUILD) MapIcons.guild(g, ix, iy, hot);
                else if (m.kind() == MapMarkersPayload.CITY) MapIcons.city(g, ix, iy, m.built(), hot);
                else MapIcons.sect(g, ix, iy, m.built(), hot);
            }
        }
        var w = MinimapState.waypoint();
        if (w != null) MapIcons.waypoint(g, sx(w.x() + 0.5), sy(w.z() + 0.5), now);
        if (player != null) MapIcons.arrow(g, sx(player.getX()), sy(player.getZ()), player.getYRot() + 180f, MapIcons.WHITE, MapIcons.INK);
        g.disableScissor();

        // toạ độ dưới con trỏ
        if (mx >= mapX && mx < mapX + mapSize && my >= mapY && my < mapY + mapSize) {
            int bx = (int) Math.floor(texCx + (mx - mapX - mapSize / 2) / scale() * texStep);
            int bz = (int) Math.floor(texCz + (my - mapY - mapSize / 2) / scale() * texStep);
            g.drawString(font, "X " + bx + "  Z " + bz, mapX, mapY + mapSize + 6, 0xFFE0D4B0, true);
        }
        g.drawString(font, "1:" + texStep, mapX + mapSize - font.width("1:" + texStep), mapY + mapSize + 6, 0xFFA09070, true);

        // danh sách bên trái
        int lx = 12, ly = mapY;
        g.drawString(font, Component.translatable("hukhong.map.list"), lx, ly - 14, 0xFFE8C060, true);
        List<MapMarkersPayload.Marker> sorted = vis && player != null
                ? MinimapState.sortedByDistance(player.getX(), player.getZ()) : new ArrayList<>();
        if (sorted.isEmpty()) {
            g.drawString(font, Component.translatable("hukhong.map.no_markers"), lx, ly, 0xFF908070, true);
        }
        int rowH = 11, maxRows = Math.max(1, (mapSize - 40) / rowH);
        for (int i = 0; i < sorted.size() && i < maxRows; i++) {
            MapMarkersPayload.Marker m = sorted.get(i);
            int ry = ly + i * rowH;
            boolean over = mx >= lx && mx < lx + listW && my >= ry && my < ry + rowH;
            if (over || MinimapState.isWaypoint(m)) g.fill(lx - 2, ry - 1, lx + listW, ry + rowH - 1, over ? 0x60FFFFFF : 0x40E8C060);
            if (m.kind() == MapMarkersPayload.GUILD) MapIcons.guild(g, lx + 4, ry + 4, false);
            else if (m.kind() == MapMarkersPayload.CITY) MapIcons.city(g, lx + 4, ry + 4, m.built(), false);
            else MapIcons.sect(g, lx + 4, ry + 4, m.built(), false);
            int dist = (int) Math.sqrt(MinimapState.dist2(m, player.getX(), player.getZ()));
            String d = dist >= 1000 ? String.format("%.1fkm", dist / 1000.0) : dist + "m";
            int dw = font.width(d);
            String name = font.plainSubstrByWidth(Component.translatable(m.nameKey()).getString(), listW - dw - 20);
            g.drawString(font, name, lx + 12, ry, m.built() ? 0xFFF4EEDC : 0xFF908878, true);
            g.drawString(font, d, lx + listW - dw, ry, 0xFFB0A080, true);
        }
        // gợi ý phím
        float sc = 0.75f;
        g.pose().pushPose();
        g.pose().translate(lx, mapY + mapSize - 30, 0);
        g.pose().scale(sc, sc, 1f);
        int hy = 0;
        for (var line : font.split(Component.translatable("hukhong.map.help"), (int) (listW / sc))) {
            g.drawString(font, line, 0, hy, 0xFF8A8070, false);
            hy += 9;
        }
        g.pose().popPose();

        if (hover != null) {
            int dist = (int) Math.sqrt(MinimapState.dist2(hover, player.getX(), player.getZ()));
            g.renderTooltip(font, List.of(
                    Component.translatable(hover.nameKey()).getVisualOrderText(),
                    Component.literal(hover.x() + ", " + hover.z() + "  (" + dist + "m)").getVisualOrderText(),
                    Component.translatable(hover.built() ? "hukhong.map.built" : "hukhong.map.approx").getVisualOrderText(),
                    Component.translatable("hukhong.map.click").getVisualOrderText()), mx, my);
        }
    }

    // ---- nhập liệu ----

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean onMap = mx >= mapX && mx < mapX + mapSize && my >= mapY && my < mapY + mapSize;
        Minecraft mc = Minecraft.getInstance();
        if (button == 0 && MinimapState.markersVisible() && mc.player != null) {
            // danh sách
            List<MapMarkersPayload.Marker> sorted = MinimapState.sortedByDistance(mc.player.getX(), mc.player.getZ());
            int rowH = 11, maxRows = Math.max(1, (mapSize - 40) / rowH);
            for (int i = 0; i < sorted.size() && i < maxRows; i++) {
                int ry = mapY + i * rowH;
                if (mx >= 12 && mx < 12 + listW && my >= ry && my < ry + rowH) {
                    toggleWaypoint(sorted.get(i));
                    cx = sorted.get(i).x(); cz = sorted.get(i).z();
                    return true;
                }
            }
            // marker trên bản đồ
            if (onMap) {
                MapMarkersPayload.Marker pick = null;
                int best = 7 * 7;
                for (MapMarkersPayload.Marker m : MinimapState.markers()) {
                    int d = (int) ((mx - sx(m.x())) * (mx - sx(m.x())) + (my - sy(m.z())) * (my - sy(m.z())));
                    if (d <= best) { best = d; pick = m; }
                }
                if (pick != null) { toggleWaypoint(pick); return true; }
            }
        }
        if (onMap && button == 1) {
            int bx = (int) Math.floor(texCx + (mx - mapX - mapSize / 2) / scale() * texStep);
            int bz = (int) Math.floor(texCz + (my - mapY - mapSize / 2) / scale() * texStep);
            MinimapState.setWaypoint(new MinimapState.Waypoint(null, bx, bz));
            return true;
        }
        if (onMap && button == 0) { dragging = true; dragMoved = 0; return true; }
        return super.mouseClicked(mx, my, button);
    }

    private static void toggleWaypoint(MapMarkersPayload.Marker m) {
        if (MinimapState.isWaypoint(m)) MinimapState.clearWaypoint();
        else MinimapState.setWaypoint(MinimapState.of(m));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            cx -= dx / scale() * texStep;
            cz -= dy / scale() * texStep;
            dragMoved += Math.abs(dx) + Math.abs(dy);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0) {
            stepIdx = Math.max(0, Math.min(STEPS.length - 1, stepIdx + (sy < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (MapKeys.FULL.matches(key, scan) || MapKeys.SIZE.matches(key, scan)) {
            if (MapKeys.SIZE.matches(key, scan)) MapEvents.suppressSizeUntilRelease();
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_C) {
            if (minecraft.player != null) { cx = minecraft.player.getX(); cz = minecraft.player.getZ(); }
            return true;
        }
        if (key == GLFW.GLFW_KEY_X || key == GLFW.GLFW_KEY_DELETE) {
            MinimapState.clearWaypoint();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
