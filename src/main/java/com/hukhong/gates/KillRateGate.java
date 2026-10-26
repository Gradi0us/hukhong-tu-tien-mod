package com.hukhong.gates;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Chống farm: >30 con cùng loài / 64 block / 10 phút -> thưởng giảm dần tới sàn x0,1. */
public final class KillRateGate {
    private KillRateGate() {}

    static final int COUNT = 30, WINDOW = 12_000, CAP = 256;
    static final double RADIUS = 64;
    static final float FLOOR = 0.1f;
    private static final Map<ResourceLocation, ArrayDeque<long[]>> KILLS = new HashMap<>();

    public static void clear() { KILLS.clear(); }

    public static float multiplier(LivingEntity v) {
        if (v.getServer() == null) return 1f;
        long now = v.getServer().getTickCount();
        var q = KILLS.computeIfAbsent(BuiltInRegistries.ENTITY_TYPE.getKey(v.getType()), k -> new ArrayDeque<>());
        while (!q.isEmpty() && (now - q.peekFirst()[0] > WINDOW || q.peekFirst()[0] > now)) q.pollFirst();
        int n = 0;
        for (long[] k : q) {
            double dx = k[1] - v.getX(), dz = k[2] - v.getZ();
            if (dx * dx + dz * dz <= RADIUS * RADIUS) n++;
        }
        if (q.size() >= CAP) q.pollFirst();
        q.addLast(new long[]{now, (long) v.getX(), (long) v.getZ()});
        return n < COUNT ? 1f : Math.max(FLOOR, 1f - 0.9f * (n - COUNT + 1) / COUNT);
    }
}
