package com.hukhong.endgame;

import com.hukhong.chg.ChgKit;

/** Toán thuần (không phụ thuộc MC) cho Bước 25-26: Tầng Thế Giới, điều kiện cổng, boss cuối, đảo, cutscene. */
public final class EndgameKit {
    private EndgameKit() {}

    // ------------------------------------------------------------ điều kiện cổng
    public static final int REQUIRED_LEVEL = 99;
    public static final int MIN_TIER = 1, MAX_TIER = 2;

    public static boolean allChgDefeated(int lair) {
        for (int i = 1; i <= ChgKit.COUNT; i++) if (!ChgKit.defeated(lair, i)) return false;
        return true;
    }

    public static boolean gatewayEligible(int lair, int level) { return level >= REQUIRED_LEVEL && allChgDefeated(lair); }

    // ------------------------------------------------------------ Tầng Thế Giới (hệ số quái / thưởng)
    public static int clampTier(int t) { return Math.max(MIN_TIER, Math.min(MAX_TIER, t)); }
    private static final double[] HP = {1.0, 1.5}, DMG = {1.0, 1.3}, REWARD = {1.0, 1.5};
    public static double hpMul(int tier) { return HP[clampTier(tier) - 1]; }
    public static double dmgMul(int tier) { return DMG[clampTier(tier) - 1]; }
    public static double rewardMul(int tier) { return REWARD[clampTier(tier) - 1]; }

    // ------------------------------------------------------------ boss cuối
    /** Chỉ số gốc boss cuối (Bước 27: BalanceSimTest khóa TTK theo các hằng này). */
    public static final double BOSS_HP = 510000, BOSS_DMG = 60, BOSS_ARMOR = 10;
    public static final int PHASES = 4;
    /** Cấp hiển thị của boss cuối + Dạ Khuyết (tên / thanh máu). */
    public static final int BOSS_LEVEL = 100;
    public static final double[] PHASE_DMG = {1.0, 1.15, 1.3, 1.5};
    public static final double[] PHASE_SPEED = {1.0, 1.05, 1.1, 1.2};
    public static final double[] PHASE_CD = {1.0, 0.85, 0.7, 0.55};
    public static final int BASE_CD = 90, PHASE_INVUL_TICKS = 50, INTRO_TICKS = 80;
    public static final int DA_KHUYET_PHASE = 3;

    /** Pha theo tỉ lệ máu: 1 (&gt;75%), 2 (&gt;50%), 3 (&gt;25%), 4 (còn lại). */
    public static int phase(float hpFrac) { return hpFrac > 0.75f ? 1 : hpFrac > 0.5f ? 2 : hpFrac > 0.25f ? 3 : 4; }
    public static int phaseIdx(int phase) { return Math.max(1, Math.min(PHASES, phase)) - 1; }

    public enum Shape { PULL, SLAM, BEAM, ERASE, SUMMON, METEOR }

    public record Skill(String id, Shape shape, double radius, double dmgMul, int windup, int minPhase, String anim) {}

    /** Sắp theo minPhase tăng dần. */
    public static final Skill[] SKILLS = {
            new Skill("tra_lai", Shape.PULL, 18, 0.6, 40, 1, "skill_b"),
            new Skill("vong_chan", Shape.SLAM, 10, 1.2, 35, 1, "skill_a"),
            new Skill("vong_ho", Shape.BEAM, 34, 1.4, 55, 2, "skill_c"),
            new Skill("xoa_net", Shape.ERASE, 20, 0.7, 45, 3, "skill_b"),
            new Skill("hu_sinh", Shape.SUMMON, 0, 0, 40, 3, "skill_b"),
            new Skill("tich_diet", Shape.METEOR, 15, 1.0, 60, 4, "skill_c"),
    };

    public static int skillsAvailable(int phase) {
        int n = 0;
        for (Skill s : SKILLS) if (s.minPhase() <= phase) n++;
        return n;
    }

    public static int cooldown(int phase) { return (int) Math.round(BASE_CD * PHASE_CD[phaseIdx(phase)]); }

    public static double damageModifier(int players, int phase) {
        return com.hukhong.events.BossScaling.dmgMul(players) * PHASE_DMG[phaseIdx(phase)] - 1;
    }

    public static double speedModifier(int phase) { return PHASE_SPEED[phaseIdx(phase)] - 1; }
    /** Số tia của Vòng Hở: bằng số pha (tối đa 4), chia đều 360 độ. */
    public static int beamCount(int phase) { return Math.max(1, Math.min(4, phase)); }
    public static int meteors(int phase) { return 6 + 2 * phase; }
    public static int summonCount(int phase) { return 2 + phase; }

    // ------------------------------------------------------------ đảo lơ lửng (tất định)
    public static final int ISLAND_COUNT = 8, MAIN_RADIUS = 22, CENTER_Y = 64;

    /** {dx, dy, dz, radius} của đảo vệ tinh i (0..7): cách tâm 38..56, lệch cao -10..+16, bán kính 4..8. */
    public static int[] island(int i) {
        long h = (i + 1) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        double ang = Math.toRadians(45.0 * i + (((h & 0xFF) / 255.0) - 0.5) * 20);
        double dist = 38 + ((h >>> 8) & 0xFF) / 255.0 * 18;
        int dy = -10 + (int) (((h >>> 16) & 0xFF) / 255.0 * 26);
        int r = 4 + (int) (((h >>> 24) & 0xFF) / 255.0 * 4);
        return new int[]{(int) Math.round(Math.cos(ang) * dist), dy, (int) Math.round(Math.sin(ang) * dist), r};
    }

    /** Độ dày đảo tại khoảng cách d so với tâm (bán kính r): 0 nếu ngoài đảo. */
    public static int depth(double d, int r) { return d > r ? 0 : 2 + (int) ((r - d) * 0.5); }

    // ------------------------------------------------------------ cutscene
    public static final int ENDING_SEAL = 1, ENDING_ABSORB = 2;
    public static final int CUTSCENE_TICKS = 600, FADE_IN = 40, FADE_OUT = 80, LINES = 3;

    public static boolean validEnding(int e) { return e == ENDING_SEAL || e == ENDING_ABSORB; }

    /** Độ mờ lớp phủ 0..1: vào dần, giữ, ra dần. */
    public static float overlay(int t) {
        if (t < 0) return 0f;
        if (t < FADE_IN) return t / (float) FADE_IN * 0.55f;
        if (t >= CUTSCENE_TICKS - FADE_OUT)
            return Math.min(1f, 0.55f + (t - (CUTSCENE_TICKS - FADE_OUT)) / (float) FADE_OUT * 0.45f);
        return 0.55f;
    }

    /** Dòng chữ đang hiện (0..LINES-1) hoặc -1 (đang ở tiêu đề/khoảng trống). */
    public static int line(int t) {
        int span = (CUTSCENE_TICKS - FADE_IN - FADE_OUT) / LINES;
        if (t < FADE_IN || t >= CUTSCENE_TICKS - FADE_OUT) return -1;
        return Math.min(LINES - 1, (t - FADE_IN) / span);
    }

    /** Độ sáng chữ của dòng hiện tại 0..1 (hiện/tắt dần 15 tick đầu/cuối mỗi đoạn). */
    public static float lineAlpha(int t) {
        int span = (CUTSCENE_TICKS - FADE_IN - FADE_OUT) / LINES;
        if (line(t) < 0) return 0f;
        int local = (t - FADE_IN) % span;
        return Math.min(1f, Math.min(local, span - local) / 15f);
    }
}
