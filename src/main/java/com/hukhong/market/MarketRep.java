package com.hukhong.market;

import com.hukhong.currency.WalletAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Uy Danh Cho: tong Tu Tinh (Ha) nguoi choi da tieu o cho -> 5 bac. Phan thuan (tier/discount/rotation) kiem thu duoc;
 * attachment {@link WalletAttachment#MARKET_SPENT} dong bo xuong client (nhu vi), client doc bang {@link #spent(Player)}.
 */
public final class MarketRep {
    private MarketRep() {}

    /** Nguong tong Ha da tieu de dat bac 0..4 (Khach La, Khach Quen, Khach Quy, Thuong Khach, Ton Khach). */
    public static final long[] THRESHOLDS = {0, 1_000L, 8_000L, 40_000L, 200_000L};
    public static final int TIERS = THRESHOLDS.length, SPECIAL_TIER = 3;
    /** Giam gia theo bac (%): 0 / 5 / 10 / 10 / 10. */
    private static final int[] DISCOUNT = {0, 5, 10, 10, 10};
    /** Mot ngay Lich = 96000 tick thoi gian Overworld (4 ngay MC). */
    public static final long LICH_DAY_TICKS = 96_000L;

    public static int tierOf(long spent) {
        int t = 0;
        for (int i = 1; i < TIERS; i++) if (spent >= THRESHOLDS[i]) t = i;
        return t;
    }

    public static int discountPct(int tier) { return DISCOUNT[Math.max(0, Math.min(TIERS - 1, tier))]; }

    public static long discounted(long price, int tier) {
        if (price <= 0) return price;
        return Math.max(1, price * (100 - discountPct(tier)) / 100);
    }

    /** Ha con thieu de len bac ke (0 neu da toi da). */
    public static long toNext(long spent) {
        int t = tierOf(spent);
        return t >= TIERS - 1 ? 0 : THRESHOLDS[t + 1] - spent;
    }

    /** Tien do 0..1 trong bac hien tai (1 neu toi da). */
    public static double progress(long spent) {
        int t = tierOf(spent);
        if (t >= TIERS - 1) return 1.0;
        return (double) (spent - THRESHOLDS[t]) / (THRESHOLDS[t + 1] - THRESHOLDS[t]);
    }

    /** Ngay Lich (chi so) tu gameTime Overworld: hang Quay Dac Biet xoay vong khi doi so nay. */
    public static long lichDay(long overworldTime) { return Math.floorDiv(Math.max(0, overworldTime), LICH_DAY_TICKS); }

    public static String tierKey(int tier) { return "hukhong.market.tier." + Math.max(0, Math.min(TIERS - 1, tier)); }

    // ---- attachment ----
    public static long spent(Player p) { return p.getData(WalletAttachment.MARKET_SPENT.get()); }

    public static int tier(Player p) { return tierOf(spent(p)); }

    /** Cong Ha da tieu; len bac thi thong bao + am thanh. Server. */
    public static void addSpent(ServerPlayer p, long ha) {
        if (ha <= 0) return;
        long before = spent(p);
        int t0 = tierOf(before);
        long after = before + ha;
        p.setData(WalletAttachment.MARKET_SPENT.get(), after);
        int t1 = tierOf(after);
        if (t1 > t0) {
            p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("hukhong.market.tier_up",
                    net.minecraft.network.chat.Component.translatable(tierKey(t1))).withStyle(net.minecraft.ChatFormatting.GOLD));
            if (t1 == SPECIAL_TIER) p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("hukhong.market.special_unlocked")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            ServerLevel l = p.serverLevel();
            l.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.1f);
            l.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.3f);
        }
    }
}
