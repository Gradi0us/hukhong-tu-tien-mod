package com.hukhong.client.render.orbit;

import com.hukhong.banmenh.BanMenhMath;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Trạng thái phi kiếm phía client: mỗi chủ có tối đa một đòn đang bay. */
public final class OrbitState {
    private OrbitState() {}

    public record Strike(int targetId, long start, int slot) {}

    private static final int TOTAL = BanMenhMath.STRIKE_DELAY_TICKS + BanMenhMath.STRIKE_RETURN_TICKS;
    private static final Map<Integer, Strike> STRIKES = new ConcurrentHashMap<>();

    public static void onFx(int ownerId, int targetId, int slot) {
        var level = Minecraft.getInstance().level;
        if (level != null) STRIKES.put(ownerId, new Strike(targetId, level.getGameTime(), slot));
    }

    /** Đòn đang bay của chủ (null nếu không có / đã xong). now = gameTime + partial. */
    public static Strike get(int ownerId, double now) {
        Strike s = STRIKES.get(ownerId);
        if (s == null) return null;
        if (now - s.start() >= TOTAL) { STRIKES.remove(ownerId, s); return null; }
        return s;
    }

    public static void clear() { STRIKES.clear(); }
}
