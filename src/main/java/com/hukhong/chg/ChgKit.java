package com.hukhong.chg;

import com.hukhong.events.BossScaling;

/** Toán thuần (không phụ thuộc MC) cho 4 Chấp Hành Giả (S1 §4): chỉ số, bộ kỹ năng theo pha, scaling, vị trí hang ổ, cờ. */
public final class ChgKit {
    private ChgKit() {}

    public enum Shape { AOE, DASH, RAIN, DRAIN, HEAL, SUMMON }
    public enum Fx { NONE, KNOCK, SLOW, BLIND, WEAK, NAUSEA }
    /** Nội tại pha 3: ARMOR (Hống), REGEN (Từ Âm), SWIFT (Lăng Uyên), LEECH (Cơ Vô Tận). */
    public enum Passive { ARMOR, REGEN, SWIFT, LEECH }

    public record Skill(String id, Shape shape, double radius, double dmgMul, int windup, int minPhase, Fx fx, String anim) {}

    public record Def(int idx, String id, int level, double hp, double dmg, double armor, double speed,
                      int baseCd, Passive passive, Skill[] skills) {
        public String entityId() { return "chg_" + idx; }
    }

    public static final int COUNT = 4;
    public static final int DESCENT_TICKS = 60, DESCENT_HEIGHT = 40, PHASE_INVUL_TICKS = 40;
    /** Hệ số theo pha 1..3. */
    public static final double[] PHASE_DMG = {1.0, 1.15, 1.35};
    public static final double[] PHASE_SPEED = {1.0, 1.1, 1.2};
    public static final double[] PHASE_CD = {1.0, 0.8, 0.6};
    public static final double SWIFT_CD_EXTRA = 0.75, ARMOR_BONUS = 10, REGEN_FRAC_PER_SEC = 0.004, LEECH_FRAC = 0.25;
    public static final int LAIR_RADIUS = 18, LAIR_REVEAL_DIST = 80, LAIR_TRIGGER_DIST = 22;
    public static final int LAIR_RESPAWN_TICKS = 24000, EVENT_COOLDOWN_TICKS = 36000, EVENT_CHECK_TICKS = 1200;
    public static final double EVENT_CHANCE = 0.05;

    private static Skill s(String id, Shape sh, double r, double m, int w, int p, Fx fx, String a) {
        return new Skill(id, sh, r, m, w, p, fx, a);
    }

    private static final Def[] DEFS = {
            new Def(1, "thach_hong", 67, 3200, 24, 18, 0.25, 100, Passive.ARMOR, new Skill[]{
                    s("da_chan", Shape.AOE, 7, 1.3, 30, 1, Fx.KNOCK, "skill_a"),
                    s("dia_chan", Shape.AOE, 13, 0.8, 40, 2, Fx.SLOW, "skill_b"),
                    s("da_mua", Shape.RAIN, 8, 1.0, 45, 3, Fx.NONE, "skill_c")}),
            new Def(2, "bach_tu_am", 78, 4400, 28, 20, 0.28, 90, Passive.REGEN, new Skill[]{
                    s("quang_tru", Shape.RAIN, 8, 0.9, 35, 1, Fx.BLIND, "skill_a"),
                    s("dong_hoa", Shape.AOE, 10, 0.6, 40, 2, Fx.WEAK, "skill_b"),
                    s("hoi_sinh_quang", Shape.HEAL, 6, 0.5, 50, 3, Fx.WEAK, "skill_c")}),
            new Def(3, "lang_uyen", 86, 5600, 36, 22, 0.32, 80, Passive.SWIFT, new Skill[]{
                    s("kiem_thiet", Shape.DASH, 3.5, 1.4, 20, 1, Fx.KNOCK, "skill_a"),
                    s("kiem_vu", Shape.RAIN, 9, 1.0, 35, 2, Fx.NONE, "skill_b"),
                    s("kiem_vuc", Shape.AOE, 15, 1.1, 70, 3, Fx.SLOW, "skill_c")}),
            new Def(4, "co_vo_tan", 98, 8000, 48, 26, 0.30, 90, Passive.LEECH, new Skill[]{
                    s("huyet_hap", Shape.DRAIN, 8, 0.9, 35, 1, Fx.WEAK, "skill_a"),
                    s("trieu_ma", Shape.SUMMON, 0, 0, 40, 2, Fx.NONE, "skill_b"),
                    s("tam_ma", Shape.AOE, 12, 0.7, 45, 3, Fx.NAUSEA, "skill_c")}),
    };

    public static Def def(int idx) { return DEFS[Math.max(1, Math.min(COUNT, idx)) - 1]; }

    public static int phaseIdx(int phase) { return Math.max(1, Math.min(3, phase)) - 1; }

    /** Hệ số cộng (ADD_MULTIPLIED_BASE) cho sát thương: scaling người chơi x hệ số pha, trừ 1. */
    public static double damageModifier(int players, int phase) {
        return BossScaling.dmgMul(players) * PHASE_DMG[phaseIdx(phase)] - 1;
    }

    public static double hpModifier(int players) { return BossScaling.hpMul(players) - 1; }

    public static double speedModifier(int phase) { return PHASE_SPEED[phaseIdx(phase)] - 1; }

    /** Hồi chiêu (tick) giữa hai kỹ năng. */
    public static int cooldown(Def d, int phase) {
        double c = d.baseCd() * PHASE_CD[phaseIdx(phase)];
        if (phase >= 3 && d.passive() == Passive.SWIFT) c *= SWIFT_CD_EXTRA;
        return (int) Math.round(c);
    }

    /** Số kỹ năng dùng được ở pha {@code phase}. */
    public static int skillsAvailable(Def d, int phase) {
        int n = 0;
        for (Skill k : d.skills()) if (k.minPhase() <= phase) n++;
        return n;
    }

    /** Số điểm rơi của chiêu RAIN: 4 + 2 mỗi pha. */
    public static int rainStrikes(int phase) { return 4 + 2 * (phaseIdx(phase) + 1); }

    /** Số tay sai của SUMMON (tối đa 6 cùng lúc, kiểm tra ở entity). */
    public static int summonCount(int phase) { return 1 + phaseIdx(phase) + 1; }

    // ------------------------------------------------------------ cờ truyện (StoryData.lair)
    /** bit0/bit1 = gợi ý hang ổ chuỗi A/B (đã có); bit (1+idx) = đã hạ CHG idx. */
    public static int defeatedBit(int idx) { return 1 << (1 + idx); }
    public static boolean defeated(int lair, int idx) { return (lair & defeatedBit(idx)) != 0; }
    /** Hang ổ I/II lộ qua Văn Kiện (chuỗi A -> I, chuỗi B -> II). */
    public static boolean revealed(int lair, int idx) { return idx >= 1 && idx <= 2 && (lair & (1 << (idx - 1))) != 0; }

    /** Sự kiện ngẫu nhiên: người chơi cấp [lv-3, lv+15] của CHG. */
    public static boolean eventEligible(int playerLevel, int idx) {
        int lv = def(idx).level();
        return playerLevel >= lv - 3 && playerLevel <= lv + 15;
    }

    /** Hang ổ chỉ kích hoạt boss với người chơi cấp >= lv-15. */
    public static boolean lairEligible(int playerLevel, int idx) { return playerLevel >= def(idx).level() - 15; }

    // ------------------------------------------------------------ vị trí hang ổ
    private static final int[] DIST = {4500, 7000, 9500, 12000};

    /** Vị trí (x, z) tất định theo seed: mỗi hang một góc phần tư riêng, xa dần theo cấp; luôn trong worldborder 15000. */
    public static int[] lairXZ(long seed, int idx) {
        long h = seed * 0x9E3779B97F4A7C15L + idx * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31; h *= 0x94D049BB133111EBL; h ^= h >>> 29;
        double jitter = ((h & 0xFFFF) / 65535.0 - 0.5) * Math.toRadians(50);
        double ang = Math.toRadians(45 + 90 * (idx - 1)) + jitter;
        double dist = DIST[idx - 1] + (((h >>> 16) & 0xFFFF) / 65535.0 - 0.5) * 800;
        return new int[]{(int) Math.round(Math.cos(ang) * dist), (int) Math.round(Math.sin(ang) * dist)};
    }
}
