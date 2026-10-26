package com.hukhong.society;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Ghế Bế Quan của Linh Khí Đài mỗi tông (R22: số chỗ có hạn). Thuần Java, test được. */
public final class AltarSeats {
    private final Map<String, Set<UUID>> seated = new HashMap<>();

    /** Giữ/nhận ghế; true nếu người này đang có ghế sau lời gọi. */
    public synchronized boolean tryTake(String altar, UUID who, int cap) {
        Set<UUID> s = seated.computeIfAbsent(altar, k -> new LinkedHashSet<>());
        if (s.contains(who)) return true;
        if (s.size() >= cap) return false;
        s.add(who);
        return true;
    }

    public synchronized void release(UUID who) {
        for (Set<UUID> s : seated.values()) s.remove(who);
    }

    public synchronized int used(String altar) { return seated.getOrDefault(altar, Set.of()).size(); }
}
