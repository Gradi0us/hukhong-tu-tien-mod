package com.hukhong.endgame;

import com.hukhong.HuKhong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Điểm truy cập DUY NHẤT tới Tầng Thế Giới (1 = mặc định, 2 = sau khi chọn kết thúc).
 * Hệ số quái/thưởng đọc qua {@link #hpMul()}, {@link #dmgMul()}, {@link #rewardMul()}; HuHoa gọi các hàm này.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class WorldTier {
    private WorldTier() {}

    private static volatile int tier = EndgameKit.MIN_TIER;

    /** Tầng Thế Giới hiện tại (1..2). */
    public static int get() { return tier; }
    public static double hpMul() { return EndgameKit.hpMul(tier); }
    public static double dmgMul() { return EndgameKit.dmgMul(tier); }
    public static double rewardMul() { return EndgameKit.rewardMul(tier); }

    /** Nâng (không bao giờ hạ) tầng thế giới và lưu. */
    public static void raiseTo(MinecraftServer server, int t) {
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        WorldTierData d = WorldTierData.get(ow);
        if (EndgameKit.clampTier(t) > d.tier()) d.setTier(t);
        tier = d.tier();
    }

    /** Lệnh thử: đặt thẳng (kể cả hạ). */
    public static void force(MinecraftServer server, int t) {
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        WorldTierData d = WorldTierData.get(ow);
        d.setTier(t);
        tier = d.tier();
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        ServerLevel ow = e.getServer().getLevel(Level.OVERWORLD);
        tier = ow == null ? EndgameKit.MIN_TIER : WorldTierData.get(ow).tier();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { tier = EndgameKit.MIN_TIER; }
}
