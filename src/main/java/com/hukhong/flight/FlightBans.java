package com.hukhong.flight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Boss thế giới luôn cấm bay (PLAN §10): thực thể gắn tag "hukhong_world_boss" trong 64 block. */
public final class FlightBans {
    private FlightBans() {}

    public static final String WORLD_BOSS_TAG = "hukhong_world_boss";
    public static final double RADIUS = 64.0;

    public static boolean isFlightBanned(ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(RADIUS);
        return !p.serverLevel().getEntities(p, box, e -> e.isAlive() && e.getTags().contains(WORLD_BOSS_TAG)).isEmpty();
    }
}
