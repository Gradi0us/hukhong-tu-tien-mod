package com.hukhong.client.minimap;

import com.hukhong.network.MapMarkersPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Trạng thái client của minimap: marker thành/tông từ server, cỡ minimap, waypoint, hiệu ứng ping. */
public final class MinimapState {
    private MinimapState() {}

    /** Cạnh minimap (px GUI) theo chế độ; chế độ cuối = ẩn. */
    public static final int[] SIZES = {64, 96, 128};
    public static final int HIDDEN = SIZES.length;

    /** Điểm đánh dấu: key = key dịch tên thành/tông, hoặc null cho điểm tự chọn. */
    public record Waypoint(String key, int x, int z) {
        public Component label() {
            return key == null ? Component.translatable("hukhong.map.custom") : Component.translatable(key);
        }
    }

    private static List<MapMarkersPayload.Marker> markers = List.of();
    private static int sizeMode = 1;
    private static Waypoint waypoint;
    private static long pingStart = Long.MIN_VALUE;

    public static void setMarkers(List<MapMarkersPayload.Marker> m) { markers = List.copyOf(m); }
    public static List<MapMarkersPayload.Marker> markers() { return markers; }

    public static int sizeMode() { return sizeMode; }
    public static void cycleSize() { sizeMode = (sizeMode + 1) % (HIDDEN + 1); }
    public static int sizePx() { return sizeMode >= HIDDEN ? 0 : SIZES[sizeMode]; }

    public static Waypoint waypoint() { return waypoint; }
    public static void setWaypoint(Waypoint w) { waypoint = w; }
    public static void clearWaypoint() { waypoint = null; }

    public static Waypoint of(MapMarkersPayload.Marker m) { return new Waypoint(m.nameKey(), m.x(), m.z()); }

    public static boolean isWaypoint(MapMarkersPayload.Marker m) {
        return waypoint != null && waypoint.x() == m.x() && waypoint.z() == m.z();
    }

    public static void ping(long gameTime) { pingStart = gameTime; }
    public static long pingStart() { return pingStart; }

    /** Marker sắp theo khoảng cách XZ tới (px,pz), gần nhất trước. */
    public static List<MapMarkersPayload.Marker> sortedByDistance(double px, double pz) {
        List<MapMarkersPayload.Marker> l = new ArrayList<>(markers);
        l.sort(Comparator.comparingDouble(m -> dist2(m, px, pz)));
        return l;
    }

    public static double dist2(MapMarkersPayload.Marker m, double px, double pz) {
        double dx = m.x() + 0.5 - px, dz = m.z() + 0.5 - pz;
        return dx * dx + dz * dz;
    }

    /** Marker thành/tông chỉ hợp lệ ở Overworld. */
    public static boolean markersVisible() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension() == Level.OVERWORLD;
    }

    public static void clearAll() {
        markers = List.of();
        waypoint = null;
        pingStart = Long.MIN_VALUE;
        MapCache.clear();
    }
}
