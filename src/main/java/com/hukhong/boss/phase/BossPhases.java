package com.hukhong.boss.phase;

import com.hukhong.boss.phase.BossPhaseConfig.ArenaEffect;
import com.hukhong.boss.phase.BossPhaseConfig.Config;
import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseConfig.Pal;
import com.hukhong.boss.phase.BossPhaseConfig.Phase;
import com.hukhong.boss.phase.BossPhaseConfig.Shape;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cấu hình pha mặc định cho mọi boss dùng khung {@link BossPhaseDirector} (định nghĩa trong code).
 * Mọi boss 3 pha: dưới 66% HP vào pha 2 (bất tử + cutscene ~5s, to 1.25x, +2 chiêu), dưới 33% vào pha 3 (~6s, 1.5x, +1 chiêu tối thượng có vòng cảnh báo).
 * Phụ đề: khoá lang {@code hukhong.boss.sub.<id>.<pha>.<n>}; tên chiêu: {@code hukhong.boss.move.<id>}.
 */
public final class BossPhases {
    private BossPhases() {}

    public static final float P2_BELOW = 0.66f, P3_BELOW = 0.33f;
    public static final int P2_TICKS = 100, P3_TICKS = 120;

    private static final Map<String, Config> ALL = new HashMap<>();

    public static Config get(String id) {
        Config c = ALL.get(id);
        if (c == null) throw new IllegalArgumentException("Khong co cau hinh pha cho boss " + id);
        return c;
    }

    public static boolean has(String id) { return ALL.containsKey(id); }

    public static Set<String> ids() { return ALL.keySet(); }

    // ------------------------------------------------------------ dựng cấu hình
    private static Move m(String id, Shape s, Pal p, double r, double dmg, int windup, Hit h, boolean ult) {
        return new Move(id, s, p, r, dmg, windup, h, ult);
    }

    private static List<SubLine> subs(String id, int phase, int lines, int total) {
        List<SubLine> out = new ArrayList<>();
        int start = 10, span = Math.max(20, (total - 14) / Math.max(1, lines));
        for (int i = 0; i < lines; i++)
            out.add(new SubLine(start + i * span, start + (i + 1) * span, "hukhong.boss.sub." + id + "." + phase + "." + (i + 1)));
        return out;
    }

    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double yaw, double pitch, double s) {
        return new CamPath.Key(t, (float) (dx * s), (float) (dy * s), (float) (dz * s), (float) yaw, (float) pitch, (float) fov, (float) (lookH * s));
    }

    /** Pha 2: cận mặt rồi góc thấp lộ diện, kéo ra. s = kích thước boss (block). */
    private static List<CamPath.Key> camReveal(int T, int reveal, double s) {
        return List.of(
                k(0, 0, 0.6, 2.4, 72, 0.5, 0, 0, s),
                k((int) (T * 0.30), 0.4, 0.75, 1.1, 48, 0.8, 0, 0, s),
                k(reveal, -1.6, 0.3, 1.9, 58, 0.7, 0, 4, s),
                k((int) (T * 0.78), 1.8, 0.9, 2.6, 62, 0.65, 0, 0, s),
                k(T, 0, 1.0, 3.6, 70, 0.6, 0, 0, s));
    }

    /** Pha 3: cực cận, vòng quanh, ra sau lưng, lùi xa. */
    private static List<CamPath.Key> camCrescendo(int T, int reveal, double s) {
        return List.of(
                k(0, 0, 0.3, 1.3, 40, 0.9, 0, 0, s),
                k((int) (T * 0.25), 1.6, 0.5, 1.7, 55, 0.8, 0, 0, s),
                k(reveal, 2.2, 1.4, 0.0, 60, 0.7, 0, -4, s),
                k((int) (T * 0.72), -1.5, 0.2, -2.2, 65, 0.7, 0, 3, s),
                k(T, 0, 1.4, 4.2, 78, 0.6, 0, 0, s));
    }

    private static Phase p1() {
        return new Phase(1, 1f, 1f, 0, 0, List.of(), List.of(), List.of(), EnumSet.noneOf(ArenaEffect.class), null, 1f);
    }

    private static Phase p2(String id, double size, int lines, Set<ArenaEffect> arena, String music, Move a, Move b) {
        return new Phase(2, P2_BELOW, 1.25f, P2_TICKS, 45, camReveal(P2_TICKS, 45, size * 1.25), subs(id, 2, lines, P2_TICKS),
                List.of(a, b), arena, music, 0.85f);
    }

    private static Phase p3(String id, double size, int lines, Set<ArenaEffect> arena, String music, Move ult) {
        return new Phase(3, P3_BELOW, 1.5f, P3_TICKS, 55, camCrescendo(P3_TICKS, 55, size * 1.5), subs(id, 3, lines, P3_TICKS),
                List.of(ult), arena, music, 0.7f);
    }

    private static Set<ArenaEffect> fx(ArenaEffect... e) {
        Set<ArenaEffect> s = EnumSet.noneOf(ArenaEffect.class);
        for (ArenaEffect x : e) s.add(x);
        return s;
    }

    private static void reg(String id, Pal aura, double size, Phase two, Phase three) {
        ALL.put(id, new Config(id, aura, size, List.of(p1(), two, three)));
    }

    static {
        // Hu Vuong (mau): P2 vuong mien vo + troi tim + tru Hu Khong; P3 san nut + Hu Khong Nuot Troi
        reg("hu_vuong", Pal.VOID, 4.0,
                p2("hu_vuong", 4.0, 2, fx(ArenaEffect.PURPLE_SKY, ArenaEffect.VOID_PILLARS), null,
                        m("hu_tru", Shape.PILLARS, Pal.VOID, 2.4, 1.0, 40, Hit.KNOCK_UP, false),
                        m("vo_anh_tram", Shape.LINE, Pal.VOID, 18, 1.1, 30, Hit.SLOW, false)),
                p3("hu_vuong", 4.0, 3, fx(ArenaEffect.CRACKED_FLOOR), "tam_ma",
                        m("hu_nuot_troi", Shape.PULL_BURST, Pal.VOID, 20, 2.6, 90, Hit.KNOCK_UP, true)));
        // Yeu vuong sa mac
        reg("sa_hat_vuong", Pal.SAND, 5.0,
                p2("sa_hat_vuong", 5.0, 1, fx(ArenaEffect.CRACKED_FLOOR), null,
                        m("cat_lun", Shape.PILLARS, Pal.SAND, 2.6, 1.0, 36, Hit.KNOCK_UP, false),
                        m("duoi_doc", Shape.LINE, Pal.TOXIC, 16, 1.1, 30, Hit.POISON, false)),
                p3("sa_hat_vuong", 5.0, 2, fx(ArenaEffect.PURPLE_SKY), "bicanh_boss",
                        m("bao_cat", Shape.DONUT, Pal.SAND, 18, 2.2, 90, Hit.BLIND, true)));
        // Yeu vuong tuyet
        reg("bang_nhan", Pal.FROST, 4.5,
                p2("bang_nhan", 4.5, 1, fx(ArenaEffect.CRACKED_FLOOR), null,
                        m("bang_tru", Shape.PILLARS, Pal.FROST, 2.4, 1.0, 36, Hit.FREEZE, false),
                        m("bang_tram", Shape.LINE, Pal.FROST, 16, 1.1, 30, Hit.SLOW, false)),
                p3("bang_nhan", 4.5, 2, fx(ArenaEffect.VOID_PILLARS), "bicanh_boss",
                        m("tuyet_diet", Shape.PULL_BURST, Pal.FROST, 19, 2.4, 90, Hit.FREEZE, true)));
        // Yeu vuong dam lay
        reg("doc_mau", Pal.TOXIC, 4.0,
                p2("doc_mau", 4.0, 1, fx(ArenaEffect.CRACKED_FLOOR), null,
                        m("mua_doc", Shape.METEOR, Pal.TOXIC, 3.0, 0.9, 44, Hit.POISON, false),
                        m("trung_no", Shape.PILLARS, Pal.TOXIC, 2.4, 1.0, 36, Hit.WEAK, false)),
                p3("doc_mau", 4.0, 2, fx(ArenaEffect.BLOOD_RAIN), "tam_ma",
                        m("suong_doc", Shape.DONUT, Pal.TOXIC, 20, 2.2, 90, Hit.POISON, true)));
        // 4 Chap Hanh Gia (de tu cua Da Khuyet)
        reg("chg_1", Pal.SAND, 3.8,
                p2("chg_1", 3.8, 1, fx(ArenaEffect.CRACKED_FLOOR), null,
                        m("da_trao", Shape.PILLARS, Pal.SAND, 2.6, 1.0, 36, Hit.KNOCK_UP, false),
                        m("chan_dong_song", Shape.SHOCK, Pal.SAND, 11, 1.0, 40, Hit.KNOCK_UP, false)),
                p3("chg_1", 3.8, 2, fx(ArenaEffect.VOID_PILLARS), "su_do",
                        m("hong_thien", Shape.PULL_BURST, Pal.SAND, 18, 2.4, 90, Hit.KNOCK_UP, true)));
        reg("chg_2", Pal.HOLY, 3.8,
                p2("chg_2", 3.8, 1, fx(ArenaEffect.VOID_PILLARS), null,
                        m("quang_tram", Shape.LINE, Pal.HOLY, 18, 1.0, 30, Hit.BLIND, false),
                        m("quang_vu", Shape.METEOR, Pal.HOLY, 3.0, 0.9, 44, Hit.WEAK, false)),
                p3("chg_2", 3.8, 2, fx(ArenaEffect.CRACKED_FLOOR), "su_do",
                        m("thien_quang_diet", Shape.DONUT, Pal.HOLY, 18, 2.2, 90, Hit.BLIND, true)));
        reg("chg_3", Pal.MOON, 3.8,
                p2("chg_3", 3.8, 1, fx(ArenaEffect.CRACKED_FLOOR), null,
                        m("kiem_ha", Shape.LINE, Pal.MOON, 20, 1.2, 26, Hit.KNOCK_UP, false),
                        m("kiem_trao", Shape.PILLARS, Pal.MOON, 2.2, 1.0, 34, Hit.KNOCK_UP, false)),
                p3("chg_3", 3.8, 2, fx(ArenaEffect.PURPLE_SKY), "su_do",
                        m("kiem_vuc_diet", Shape.PULL_BURST, Pal.MOON, 20, 2.5, 90, Hit.SLOW, true)));
        reg("chg_4", Pal.BLOOD, 3.8,
                p2("chg_4", 3.8, 1, fx(ArenaEffect.BLOOD_RAIN), null,
                        m("huyet_tru", Shape.PILLARS, Pal.BLOOD, 2.4, 1.0, 36, Hit.WEAK, false),
                        m("huyet_vu", Shape.METEOR, Pal.BLOOD, 3.0, 0.9, 44, Hit.NAUSEA, false)),
                p3("chg_4", 3.8, 2, fx(ArenaEffect.CRACKED_FLOOR, ArenaEffect.PURPLE_SKY), "su_do",
                        m("huyet_hai", Shape.DONUT, Pal.BLOOD, 19, 2.3, 90, Hit.WEAK, true)));
        // Da Khuyet: moi pha la mot mat trang khuyet dan; P3 trang mau
        reg("da_khuyet", Pal.MOON, 3.8,
                p2("da_khuyet", 3.8, 2, fx(ArenaEffect.PURPLE_SKY), null,
                        m("khuyet_tram", Shape.LINE, Pal.MOON, 20, 1.2, 28, Hit.BLIND, false),
                        m("nguyet_lien", Shape.SHOCK, Pal.MOON, 12, 1.0, 40, Hit.SLOW, false)),
                p3("da_khuyet", 3.8, 3, fx(ArenaEffect.BLOOD_RAIN, ArenaEffect.VOID_PILLARS), "tam_ma",
                        m("nguyet_thuc", Shape.PULL_BURST, Pal.MOON, 22, 2.6, 100, Hit.BLIND, true)));

        // 8 Hu Vuong dang (PLAN-v2 12.6): moi dang 1 mau chu de + 2 chieu pha 2 + 1 chieu toi thuong pha 3 (chieu rieng tung dang o HvSkill)
        hv("huyet_ma", Pal.BLOOD, 4.1, fx(ArenaEffect.BLOOD_RAIN), fx(ArenaEffect.CRACKED_FLOOR, ArenaEffect.PURPLE_SKY), "tam_ma", 2, 2,
                m("hv_ma_tru", Shape.PILLARS, Pal.BLOOD, 2.4, 1.0, 36, Hit.WEAK, false), m("hv_ma_vu", Shape.METEOR, Pal.BLOOD, 3.0, 0.9, 44, Hit.NAUSEA, false),
                m("hv_ma_hai", Shape.DONUT, Pal.BLOOD, 19, 2.3, 90, Hit.WEAK, true));
        hv("cot_long", Pal.BONE, 4.5, fx(ArenaEffect.CRACKED_FLOOR), fx(ArenaEffect.VOID_PILLARS), "bicanh_boss", 2, 2,
                m("hv_long_tru", Shape.PILLARS, Pal.BONE, 2.6, 1.0, 36, Hit.KNOCK_UP, false), m("hv_long_tram", Shape.LINE, Pal.BONE, 20, 1.2, 28, Hit.SLOW, false),
                m("hv_long_no", Shape.PULL_BURST, Pal.BONE, 20, 2.5, 90, Hit.KNOCK_UP, true));
        hv("moc_yeu", Pal.WOOD, 4.1, fx(ArenaEffect.CRACKED_FLOOR), fx(ArenaEffect.BLOOD_RAIN), "tam_ma", 2, 2,
                m("hv_moc_tru", Shape.PILLARS, Pal.WOOD, 2.4, 1.0, 36, Hit.SLOW, false), m("hv_moc_vu", Shape.METEOR, Pal.WOOD, 3.0, 0.9, 44, Hit.POISON, false),
                m("hv_moc_lam", Shape.DONUT, Pal.WOOD, 20, 2.2, 90, Hit.POISON, true));
        hv("bang_phach", Pal.FROST, 4.0, fx(ArenaEffect.PURPLE_SKY), fx(ArenaEffect.CRACKED_FLOOR), "bicanh_boss", 2, 2,
                m("hv_bang_tru", Shape.PILLARS, Pal.FROST, 2.4, 1.0, 36, Hit.FREEZE, false), m("hv_bang_tram", Shape.LINE, Pal.FROST, 18, 1.1, 30, Hit.SLOW, false),
                m("hv_bang_diet", Shape.PULL_BURST, Pal.FROST, 19, 2.4, 90, Hit.FREEZE, true));
        hv("sa_tran", Pal.SAND, 4.6, fx(ArenaEffect.CRACKED_FLOOR), fx(ArenaEffect.PURPLE_SKY), "bicanh_boss", 2, 2,
                m("hv_sa_tru", Shape.PILLARS, Pal.SAND, 2.6, 1.0, 36, Hit.KNOCK_UP, false), m("hv_sa_tram", Shape.LINE, Pal.SAND, 18, 1.1, 30, Hit.BLIND, false),
                m("hv_sa_bao", Shape.DONUT, Pal.SAND, 18, 2.2, 90, Hit.BLIND, true));
        hv("doc_chuong", Pal.TOXIC, 4.2, fx(ArenaEffect.CRACKED_FLOOR), fx(ArenaEffect.BLOOD_RAIN), "tam_ma", 2, 2,
                m("hv_doc_vu", Shape.METEOR, Pal.TOXIC, 3.0, 0.9, 44, Hit.POISON, false), m("hv_doc_tru", Shape.PILLARS, Pal.TOXIC, 2.4, 1.0, 36, Hit.WEAK, false),
                m("hv_doc_no", Shape.DONUT, Pal.TOXIC, 20, 2.2, 90, Hit.POISON, true));
        hv("hai_uyen", Pal.SEA, 4.6, fx(ArenaEffect.PURPLE_SKY), fx(ArenaEffect.VOID_PILLARS), "bicanh_boss", 2, 2,
                m("hv_hai_tru", Shape.PILLARS, Pal.SEA, 2.4, 1.0, 36, Hit.SLOW, false), m("hv_hai_tram", Shape.LINE, Pal.SEA, 20, 1.2, 28, Hit.SLOW, false),
                m("hv_hai_nuot", Shape.PULL_BURST, Pal.SEA, 20, 2.5, 90, Hit.SLOW, true));
        // Thien Dien: 3 mat, phap thay doi theo pha
        hv("thien_dien", Pal.HOLY, 4.0, fx(ArenaEffect.PURPLE_SKY, ArenaEffect.VOID_PILLARS), fx(ArenaEffect.CRACKED_FLOOR, ArenaEffect.BLOOD_RAIN), "tam_ma", 3, 3,
                m("hv_td_tru", Shape.PILLARS, Pal.VOID, 2.4, 1.0, 36, Hit.KNOCK_UP, false), m("hv_td_tram", Shape.LINE, Pal.HOLY, 22, 1.2, 28, Hit.BLIND, false),
                m("hv_td_diet", Shape.PULL_BURST, Pal.VOID, 22, 2.8, 100, Hit.BLIND, true));
    }

    private static void hv(String key, Pal aura, double size, Set<ArenaEffect> a2, Set<ArenaEffect> a3, String music3, int l2, int l3, Move a, Move b, Move ult) {
        String id = "hvf_" + key;
        reg(id, aura, size, p2(id, size, l2, a2, null, a, b), p3(id, size, l3, a3, music3, ult));
    }
}
