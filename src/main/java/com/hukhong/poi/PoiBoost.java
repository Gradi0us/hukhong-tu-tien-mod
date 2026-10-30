package com.hukhong.poi;

import com.hukhong.core.CultivationService;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * He so EXP tu luyen tam thoi cua dia diem (Linh Tuyen x1.3 khi ngam, Thac Linh Khi x1.5 khi thien): goi tu CultivationService.addExp,
 * dong thoi cap them chut EXP nen moi giay de he so co tac dung ngay ca khi khong danh quai. Chi server, khong luu (het hieu luc 3s sau khi roi vung).
 */
public final class PoiBoost {
    private PoiBoost() {}

    public static final double SOAK = 1.3, MEDITATE = 1.5;
    private record B(double mult, long until) {}
    private static final Map<UUID, B> ACTIVE = new HashMap<>();

    public static void set(ServerPlayer p, double mult, long now) { ACTIVE.put(p.getUUID(), new B(mult, now + 60)); }

    public static double mult(ServerPlayer p) {
        B b = ACTIVE.get(p.getUUID());
        return b != null && b.until() > p.level().getGameTime() ? b.mult() : 1.0;
    }

    /** Thuan: EXP sau he so (lam tron xuong, khong thap hon goc). */
    public static long apply(long amount, double mult) { return amount <= 0 ? amount : Math.max(amount, (long) Math.floor(amount * mult)); }

    public static long boost(ServerPlayer p, long amount) {
        double m = mult(p);
        return m > 1.0 ? apply(amount, m) : amount;
    }

    /** EXP nen moi giay khi dang ngam / thien: expToNext * 0.0004 * (he so - 1) * 2. */
    public static long trickle(int level, double mult) {
        return Math.max(1, (long) Math.floor(CultivationService.expToNext(level) * 0.0008 * (mult - 1.0)));
    }

    public static void clear(UUID id) { ACTIVE.remove(id); }
}
