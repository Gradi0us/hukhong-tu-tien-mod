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

    private static volatile int tier = EndgameKit.MIN_TIER, variant = EndgameKit.VARIANT_NONE;

    /** Tầng Thế Giới hiện tại (1..2). */
    public static int get() { return tier; }
    public static int variant() { return variant; }
    public static double hpMul() { return EndgameKit.hpMul(tier, variant); }
    public static double dmgMul() { return EndgameKit.dmgMul(tier, variant); }
    public static double rewardMul() { return EndgameKit.rewardMul(tier); }

    /** Nâng (không bao giờ hạ) tầng thế giới và lưu. */
    public static void raiseTo(MinecraftServer server, int t) {
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        WorldTierData d = WorldTierData.get(ow);
        if (EndgameKit.clampTier(t) > d.tier()) d.setTier(t);
        tier = d.tier();
    }

    /** Đặt biến thể Tầng II (Linh Khí Phục Hưng) và lưu; cũng nâng tầng lên 2 nếu chưa. */
    public static void setVariant(MinecraftServer server, int v) {
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        raiseTo(server, 2);
        WorldTierData d = WorldTierData.get(ow);
        d.setVariant(v);
        variant = v;
    }

    /** EXP tu luyen nhan he so khi dung trong vung (Hu Hoa cu) o bien the Linh Khi Phuc Hung. */
    public static long spiritBoost(net.minecraft.server.level.ServerPlayer p, long amount) {
        if (variant != EndgameKit.VARIANT_LINH_KHI || amount <= 0 || p.level().dimension() != Level.OVERWORLD) return amount;
        boolean in = false;
        for (com.hukhong.zone.ZoneData.Zone z : com.hukhong.zone.ZoneData.snapshot())
            if (Math.hypot(p.getX() - z.cx, p.getZ() - z.cz) <= z.radius) { in = true; break; }
        return Math.round(amount * EndgameKit.expMul(tier, variant, in));
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
        variant = ow == null ? EndgameKit.VARIANT_NONE : WorldTierData.get(ow).variant();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { tier = EndgameKit.MIN_TIER; variant = EndgameKit.VARIANT_NONE; }
}
