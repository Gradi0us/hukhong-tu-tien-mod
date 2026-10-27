package com.hukhong.client.minimap;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.network.MapMarkersPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/** Tick client: quét bản đồ, xử lý phím M/N/B, làm tươi texture minimap, tới nơi thì xóa waypoint. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MapEvents {
    private MapEvents() {}

    /** Giữ M quá số tick này = mở bản đồ lớn. */
    private static final int HOLD_TICKS = 12;
    private static int sizeHeld;
    private static boolean suppress;

    /** Màn bản đồ lớn đóng bằng M: bỏ qua phím M cho tới khi nhả (tránh đổi cỡ nhầm). */
    static void suppressSizeUntilRelease() { suppress = true; }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            drain();
            sizeHeld = 0;
            return;
        }
        if (!ClientConfig.minimapOn()) { // tắt qua config: nuốt phím, không quét
            drain();
            return;
        }
        MapCache.tick(mc);
        keys(mc);
        MinimapHud.refresh(mc);
        if (mc.level.getGameTime() % 40 == 0 && MinimapState.markersVisible()) {
            double px = mc.player.getX(), pz = mc.player.getZ();
            for (var m : MinimapState.markers()) {
                if (MinimapState.dist2(m, px, pz) <= (double) MinimapState.DISCOVER * MinimapState.DISCOVER && MinimapState.discover(m))
                    mc.gui.setOverlayMessage(Component.translatable("hukhong.map.discovered", Component.translatable(m.nameKey()),
                            MinimapState.distText((int) Math.sqrt(MinimapState.dist2(m, px, pz)))), false);
            }
        }
        arrival(mc);
    }

    private static void drain() {
        while (MapKeys.SIZE.consumeClick()) { }
        while (MapKeys.FULL.consumeClick()) { }
        while (MapKeys.PING.consumeClick()) { }
    }

    private static void keys(Minecraft mc) {
        boolean noScreen = mc.screen == null;
        while (MapKeys.FULL.consumeClick()) {
            if (noScreen) { mc.setScreen(new FullMapScreen()); noScreen = false; }
        }
        // M: bấm nhanh = đổi cỡ (nhả phím), giữ = bản đồ lớn
        while (MapKeys.SIZE.consumeClick()) { }
        if (suppress) {
            if (!MapKeys.SIZE.isDown()) suppress = false;
            sizeHeld = 0;
        } else if (noScreen && MapKeys.SIZE.isDown()) {
            sizeHeld++;
            if (sizeHeld == HOLD_TICKS) mc.setScreen(new FullMapScreen());
        } else {
            if (sizeHeld > 0 && sizeHeld < HOLD_TICKS && noScreen) MinimapState.cycleSize();
            sizeHeld = 0;
        }
        while (MapKeys.PING.consumeClick()) {
            if (mc.screen == null) ping(mc);
        }
    }

    /** B: waypoint -> thành/tông gần nhất; bấm tiếp = mục gần kế tiếp; Shift+B = xóa waypoint. */
    private static void ping(Minecraft mc) {
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            MinimapState.clearWaypoint();
            return;
        }
        if (!MinimapState.markersVisible()) {
            mc.gui.setOverlayMessage(Component.translatable("hukhong.map.no_markers"), false);
            return;
        }
        List<MapMarkersPayload.Marker> l = MinimapState.sortedByDistance(mc.player.getX(), mc.player.getZ());
        if (l.isEmpty()) {
            mc.gui.setOverlayMessage(Component.translatable("hukhong.map.no_markers"), false);
            return;
        }
        int pick = 0;
        for (int i = 0; i < l.size(); i++) if (MinimapState.isWaypoint(l.get(i))) { pick = (i + 1) % l.size(); break; }
        MapMarkersPayload.Marker m = l.get(pick);
        MinimapState.setWaypoint(MinimapState.of(m));
        MinimapState.ping(mc.level.getGameTime());
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f));
        int dist = (int) Math.sqrt(MinimapState.dist2(m, mc.player.getX(), mc.player.getZ()));
        mc.gui.setOverlayMessage(Component.translatable("hukhong.map.ping", Component.translatable(m.nameKey()), dist), false);
    }

    private static void arrival(Minecraft mc) {
        var w = MinimapState.waypoint();
        if (w == null || mc.level.getGameTime() % 10 != 0) return;
        double dx = w.x() + 0.5 - mc.player.getX(), dz = w.z() + 0.5 - mc.player.getZ();
        if (dx * dx + dz * dz < 10 * 10) {
            mc.gui.setOverlayMessage(Component.translatable("hukhong.map.arrived", w.label()), false);
            MinimapState.clearWaypoint();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        MinimapState.clearAll();
        sizeHeld = 0;
    }
}
