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

    public static final double BOTTLENECK_EXP_MULT = 0.25;
    /** Bình Cảnh nhẹ (S3 §1.3.2): thanh cấp 94 (vào 95) và 98 (vào 99) dài x3, vạch 1/3 = điểm dùng Tiểu Phá Đan. */
    public static final int LIGHT_BAR_MULT = 3;

    public static boolean isLightBottleneckLevel(int level) { return level == 94 || level == 98; }

    /** Kích thước thanh EXP thực của cấp (x3 ở 94/98). */
    public static long barSize(int level) {
        return isLightBottleneckLevel(level) ? expToNext(level) * LIGHT_BAR_MULT : expToNext(level);
    }

    /** Đang ở Bình Cảnh nhẹ: cấp 94/98 và EXP đã chạm vạch 1/3. */
    public static boolean inLightBottleneck(CultivationData d) {
        return isLightBottleneckLevel(d.level()) && d.exp() >= expToNext(d.level());
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
        if (d.inBottleneck()) amount = Math.max(1, Math.round(amount * BOTTLENECK_EXP_MULT));
        long exp = d.exp() + amount;
        int level = d.level();
        while (level < CultivationData.MAX_LEVEL) {
            long need = barSize(level);
            if (exp < need) break;
            exp -= need;
            level++;
            if (CultivationData.isMilestoneLevel(level) || level >= CultivationData.MAX_LEVEL) { exp = 0; break; } // khóa mốc
        }
        return level == d.level() ? d.withExp(exp) : d.withLevel(level).withExp(exp);
    }

    /** Qua Bình Cảnh: lên ngay cấp mốc (thuần). Không đổi nếu không ở Bình Cảnh. */
    public static CultivationData applyBreakthrough(CultivationData d) {
        return d.inBottleneck() ? d.withLevel(d.level() + 1) : d;
    }

    /** Tiểu Phá Đan qua Bình Cảnh nhẹ: lên ngay cấp kế (95/99, khóa mốc). Không đổi nếu chưa tới vạch 1/3. */
    public static CultivationData applyLightBreakthrough(CultivationData d, int lightLevel) {
        return d.level() == lightLevel && inLightBottleneck(d) ? d.withLevel(d.level() + 1) : d;
    }

    public static boolean tryLightBreakthrough(ServerPlayer p, int lightLevel) {
        CultivationData d = get(p);
        if (d.level() != lightLevel || !inLightBottleneck(d)) return false;
        update(p, applyLightBreakthrough(d, lightLevel));
        return true;
    }

    /** Phẩm đan cần cho Bình Cảnh hiện tại (1 = cấp 9, 2 = cấp 19...), 0 nếu không ở Bình Cảnh. */
    public static int requiredPillTier(CultivationData d) { return d.inBottleneck() ? (d.level() + 1) / 10 : 0; }

    /** Phá Bình Cảnh bằng đan phẩm `tier`. true nếu thành công. */
    public static boolean tryBreakthrough(ServerPlayer p, int tier) {
        CultivationData d = get(p);
        if (!d.inBottleneck() || requiredPillTier(d) != tier) return false;
        update(p, applyBreakthrough(d));
        return true;
    }

    public static boolean isMilestoneLocked(ServerPlayer p) { return get(p).milestoneLocked(); }

    /** Mở khóa mốc hiện tại (EXP chảy lại). true nếu đang khóa. */
    public static boolean unlockMilestone(ServerPlayer p) {
        CultivationData d = get(p);
        if (!d.milestoneLocked()) return false;
        if (com.hukhong.trial.TrialMath.requiresTrial(d.level())) { // Khảo Hạch bắt buộc từ mốc 90+
            com.hukhong.trial.TrialService.begin(p);
            return true;
        }
        update(p, d.withMilestoneUnlocked(true));
        return true;
    }

    /** Mở khóa mốc sau khi qua Khảo Hạch (chỉ TrialService gọi). */
    public static void forceUnlockMilestone(ServerPlayer p) {
        CultivationData d = get(p);
        if (d.milestoneLocked()) update(p, d.withMilestoneUnlocked(true));
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
        if (next.level() > old.level()) {
            levelUpEffect(p, next.realmIndex() != old.realmIndex());
            if (next.level() % 10 == 0) com.hukhong.breakthrough.BreakthroughEffects.play(p, next);
        }
        SyncPayload.send(p);
        if (next.level() != old.level()) com.hukhong.stats.StatService.refresh(p);
        // Luồng truyện: qua/mở khóa mốc 10 cấp -> sang chương kế (story/flow/StoryFlow)
        if (next.level() != old.level() || next.milestoneLocked() != old.milestoneLocked()) com.hukhong.story.flow.StoryFlow.onProgress(p);
    }

    private static void levelUpEffect(ServerPlayer p, boolean newRealm) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(newRealm ? com.hukhong.vfx.ModParticles.THANH_QUANG.get() : com.hukhong.vfx.ModParticles.LINH_KHI.get(),
                p.getX(), p.getY() + 1.0, p.getZ(), newRealm ? 40 : 20, 0.5, 0.8, 0.5, 0.1);
        sl.playSound(null, p.blockPosition(),
                newRealm ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
