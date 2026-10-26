package com.hukhong.flight;

import com.hukhong.age.AgeMath;
import com.hukhong.age.AgeService;
import com.hukhong.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Điều kiện cấm bay: boss thế giới (PLAN §10, tag "hukhong_world_boss" trong 64 block), Thiên Nhân Ngũ Suy GĐ V (S6 §1.5),
 * và Ngự Kiếm khi đang trúng hiệu ứng Hư Hóa (S2 §2.1).
 */
public final class FlightBans {
    private FlightBans() {}

    public static final String WORLD_BOSS_TAG = "hukhong_world_boss";
    public static final double RADIUS = 64.0;

    public static boolean isFlightBanned(ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(RADIUS);
        return !p.serverLevel().getEntities(p, box, e -> e.isAlive() && e.getTags().contains(WORLD_BOSS_TAG)).isEmpty();
    }

    /** Khóa lang của lý do cấm bay (null = được bay) với kiểu bay mode. */
    public static String banKey(ServerPlayer p, FlightMode mode) {
        if (AgeMath.flightBanned(AgeService.stageOf(p))) return "hukhong.flight.banned_age";
        if (mode == FlightMode.KIEM && p.hasEffect(ModEffects.HU_HOA)) return "hukhong.flight.banned_hu_hoa";
        if (isFlightBanned(p)) return "hukhong.flight.banned";
        return null;
    }
}
