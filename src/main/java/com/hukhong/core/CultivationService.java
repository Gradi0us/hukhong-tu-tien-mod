package com.hukhong.core;

import com.hukhong.network.SyncPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class CultivationService {
    private CultivationService() {}

    /** EXP cần để lên từ cấp L sang L+1: E(L) = 40 * L^2.15. */
    public static long expToNext(int level) {
        return Math.round(40.0 * Math.pow(level, 2.15));
    }

    public static int resourceMaxFor(int level) { return 100 + 10 * (level - 1); }

    /**
     * Cộng EXP (thuần, không side effect) — dùng cho test.
     * Dừng khi chạm mốc khóa (10/20/...) hoặc cấp 100.
     * TODO Bình Cảnh: ở cấp 9/19/... cần Phá Cảnh Đan/Linh Vật, nếu không cần thêm rất nhiều EXP (bottleneck flag).
     * TODO Ấn Hồn Đài: mở khóa mốc sau khi chọn Lõi.
     */
    public static CultivationData applyExp(CultivationData d, long amount) {
        if (amount <= 0 || d.milestoneLocked() || d.level() >= CultivationData.MAX_LEVEL) return d;
        long exp = d.exp() + amount;
        int level = d.level();
        while (level < CultivationData.MAX_LEVEL) {
            long need = expToNext(level);
            if (exp < need) break;
            exp -= need;
            level++;
            if (level % 10 == 0 || level >= CultivationData.MAX_LEVEL) { exp = 0; break; } // khóa mốc
        }
        return level == d.level() ? d.withExp(exp) : d.withLevel(level).withExp(exp);
    }

    public static void addExp(ServerPlayer p, long amount) {
        update(p, applyExp(get(p), amount));
    }

    public static void setLevel(ServerPlayer p, int level) {
        update(p, get(p).withLevel(Math.max(1, Math.min(CultivationData.MAX_LEVEL, level))));
    }

    public static CultivationData get(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION); }

    private static void update(ServerPlayer p, CultivationData next) {
        CultivationData old = get(p);
        p.setData(ModAttachments.CULTIVATION, next);
        if (next.level() > old.level()) levelUpEffect(p, next.realmIndex() != old.realmIndex());
        SyncPayload.send(p);
    }

    private static void levelUpEffect(ServerPlayer p, boolean newRealm) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(newRealm ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD,
                p.getX(), p.getY() + 1.0, p.getZ(), newRealm ? 40 : 20, 0.5, 0.8, 0.5, 0.1);
        sl.playSound(null, p.blockPosition(),
                newRealm ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
