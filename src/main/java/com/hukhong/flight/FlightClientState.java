package com.hukhong.flight;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Trạng thái bay phía client (không import lớp client -> an toàn trên server). */
public final class FlightClientState {
    private FlightClientState() {}

    public record Info(FlightMode mode, int pairs) {}

    private static final Map<Integer, Info> MAP = new ConcurrentHashMap<>();

    public static void set(int entityId, int mode, int pairs) {
        FlightMode m = FlightMode.byOrdinal(mode);
        if (m == null) MAP.remove(entityId); else MAP.put(entityId, new Info(m, pairs));
    }
    public static Info get(int entityId) { return MAP.get(entityId); }
    public static Map<Integer, Info> all() { return MAP; }
    public static void clear() { MAP.clear(); }
}
