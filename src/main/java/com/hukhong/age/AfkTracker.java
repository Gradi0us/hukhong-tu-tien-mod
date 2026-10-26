package com.hukhong.age;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** AFK đơn giản: không di chuyển/xoay camera trong 5 phút. Gọi check() định kỳ (mỗi 20 tick). */
public final class AfkTracker {
    private AfkTracker() {}

    public static final long AFK_TICKS = 5L * 60 * 20;

    private record State(Vec3 pos, float yRot, float xRot, long lastActive) {}

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Cập nhật trạng thái và trả true nếu đang AFK. */
    public static boolean check(ServerPlayer p) {
        long now = p.tickCount;
        Vec3 pos = p.position();
        State s = STATES.get(p.getUUID());
        if (s == null || s.pos().distanceToSqr(pos) > 1.0E-4 || s.yRot() != p.getYRot()
                || s.xRot() != p.getXRot() || now < s.lastActive()) {
            STATES.put(p.getUUID(), new State(pos, p.getYRot(), p.getXRot(), now));
            return false;
        }
        return now - s.lastActive() >= AFK_TICKS;
    }

    public static void remove(ServerPlayer p) { STATES.remove(p.getUUID()); }
}
