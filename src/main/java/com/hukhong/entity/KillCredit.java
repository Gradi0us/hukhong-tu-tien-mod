package com.hukhong.entity;

import com.hukhong.HuKhong;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Chia EXP giết quái (2026-11-06):
 * - Chơi một mình: như cũ, người giết nhận đủ.
 * - Nhiều người: người giết nhận đủ; ai có hỗ trợ (gây ≥5% sát thương) nhận phần hỗ trợ 30-80% theo tỉ lệ sát thương.
 * - Quái bị lính thủ vệ / thứ không phải người chơi kết liễu: người gây nhiều sát thương nhất nhận đủ như người giết.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class KillCredit {
    private KillCredit() {}

    public static final double ASSIST_MIN_SHARE = 0.05, ASSIST_BASE = 0.3, ASSIST_PER_SHARE = 0.5, ASSIST_RANGE = 64;

    private static final Map<LivingEntity, Map<ServerPlayer, Float>> DMG = new WeakHashMap<>();

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !(v instanceof HuMob) || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        record(v, p, e.getNewDamage());
    }

    public static void record(LivingEntity v, ServerPlayer who, float dmg) {
        if (dmg > 0) DMG.computeIfAbsent(v, k -> new HashMap<>()).merge(who, dmg, Float::sum);
    }

    /** Người nhận EXP và hệ số (1 = đủ). Gọi lúc quái chết; xóa bản ghi sát thương. */
    public static Map<ServerPlayer, Double> shares(LivingEntity v, ServerPlayer killer) {
        Map<ServerPlayer, Float> dmg = DMG.remove(v);
        Map<ServerPlayer, Double> out = new LinkedHashMap<>();
        List<ServerPlayer> hitters = new ArrayList<>();
        float total = 0;
        if (dmg != null) for (var en : dmg.entrySet()) {
            ServerPlayer p = en.getKey();
            if (p.isRemoved() || p.level() != v.level() || p.distanceTo(v) > ASSIST_RANGE) continue;
            hitters.add(p);
            total += en.getValue();
        }
        ServerPlayer main = killer;
        if (main == null) { // lính thủ vệ / môi trường kết liễu: người gây nhiều sát thương nhất
            float best = 0;
            for (ServerPlayer p : hitters) { float d = dmg.get(p); if (d > best) { best = d; main = p; } }
        }
        if (main == null) return out;
        out.put(main, 1.0);
        for (ServerPlayer p : hitters) {
            if (p == main) continue;
            double share = total <= 0 ? 0 : dmg.get(p) / total;
            if (share >= ASSIST_MIN_SHARE) out.put(p, Math.min(0.8, ASSIST_BASE + ASSIST_PER_SHARE * share));
        }
        return out;
    }
}
