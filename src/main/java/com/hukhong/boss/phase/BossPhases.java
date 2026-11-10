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

    /** Pha 2: 2 chiêu riêng + chém liên hoàn + nhảy đập (bộ chiêu kiểu Elden Ring dùng chung, màu theo boss). */
    private static Phase p2(String id, double size, int lines, Set<ArenaEffect> arena, String music, Move a, Move b) {
        return new Phase(2, P2_BELOW, 1.25f, P2_TICKS, 45, camReveal(P2_TICKS, 45, size * 1.25), subs(id, 2, lines, P2_TICKS),
                List.of(a, b,
                        m("er_combo", Shape.COMBO, a.pal(), 5.5 + size * 0.4, 0.8, 26, a.hit(), false),
                        m("er_leap", Shape.LEAP, a.pal(), 4.5, 1.2, 34, Hit.KNOCK_UP, false)),
                arena, music, 0.85f);
    }

    /** Pha 3: tối thượng riêng + xoay chém + sóng 3 lớp + thiên kiếm giáng (tối thượng, có cutscene). */
    private static Phase p3(String id, double size, int lines, Set<ArenaEffect> arena, String music, Move ult) {
        return new Phase(3, P3_BELOW, 1.5f, P3_TICKS, 55, camCrescendo(P3_TICKS, 55, size * 1.5), subs(id, 3, lines, P3_TICKS),
                List.of(ult,
                        m("er_spin", Shape.SPIN, ult.pal(), 6.5, 0.7, 30, ult.hit(), false),
                        m("er_nova", Shape.NOVA, ult.pal(), 18, 0.9, 36, ult.hit(), false),
                        m("er_sky_sword", Shape.SKY_SWORD, ult.pal(), 6, 2.4, 84, Hit.KNOCK_UP, true)),
                arena, music, 0.7f);
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
        // Hu Vuong (kieu Rykard + Godrick): bo chieu RIENG - HuVuong tu thi trien qua startOwnMove (HuVuongMoves).
        huVuong();
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
        // CHG I Thach Hong (kieu Godfrey/Hoarah Loux): bo chieu RIENG - ChapHanhGia tu thi trien qua startOwnMove (ChgOneMoves).
        chgOne();
        // CHG II Bach Tu Am (kieu Maliketh, chuong tu + Hac Kiem): bo chieu RIENG - ChapHanhGia/ChgMaliketh tu thi trien.
        chg2();
        // CHG III Lang Uyen (kieu Morgott, vuc sau): bo chieu RIENG - tu thi trien qua startOwnMove (Chg3Moves).
        chg3();
        // CHG IV (kieu Mohg, mau): bo chieu RIENG - ChapHanhGia(idx 4) tu thi trien qua startOwnMove (Chg4Moves).
        chg4();
        // Da Khuyet (kieu Malenia, trang/mau): bo chieu RIENG - DaKhuyet tu thi trien qua PhasedBoss.startOwnMove (khong dung er_* chung).
        daKhuyet();

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

    /** Da Khuyet: P2 "Nguyet Duc" (moc canh trang khuyet), P3 "Huyet Nguyet Khai Hoa" (no hoa trang mau). Thoai: hukhong.dialog.dakhuyet.*. */
    private static void daKhuyet() {
        String id = "da_khuyet";
        double s = 3.8;
        int t2 = 120, r2 = 60, t3 = 120, r3 = 70;
        List<SubLine> s2 = List.of(new SubLine(8, 44, "hukhong.dialog.dakhuyet.p2.1"), new SubLine(44, 84, "hukhong.dialog.dakhuyet.p2.2"),
                new SubLine(84, 116, "hukhong.dialog.dakhuyet.p2.3"));
        List<SubLine> s3 = List.of(new SubLine(8, 44, "hukhong.dialog.dakhuyet.p3.1"), new SubLine(44, 84, "hukhong.dialog.dakhuyet.p3.2"),
                new SubLine(84, 116, "hukhong.dialog.dakhuyet.p3.3"));
        // P2: quy goi -> canh trang khuyet bung ra (camera thap tu sau lung nhin len canh) -> keo xa
        double a = s * 1.25;
        List<CamPath.Key> c2 = List.of(k(0, 0, 0.5, 2.2, 60, 0.6, 0, 0, a), k(40, 1.2, 0.3, -1.6, 55, 0.8, 0, -8, a),
                k(r2, 0.2, 0.2, -2.4, 70, 0.9, 0, -14, a), k(95, -2.0, 1.2, 2.2, 62, 0.8, 0, 0, a), k(t2, 0, 1.0, 3.8, 72, 0.7, 0, 0, a));
        // P3: bay len, troi do - camera tu duoi dat nhin len "doa hoa" mau roi vong ra xa
        double b = s * 1.5;
        List<CamPath.Key> c3 = List.of(k(0, 0, 0.2, 1.6, 50, 0.9, 0, 0, b), k(35, 1.8, 0.1, 1.2, 64, 1.4, 0, -12, b),
                k(r3, 0, 0.0, 2.6, 80, 2.0, 0, -18, b), k(100, -2.4, 1.6, 1.8, 70, 1.2, 0, 0, b), k(t3, 0, 1.6, 4.4, 78, 0.8, 0, 0, b));
        Phase two = new Phase(2, P2_BELOW, 1.25f, t2, r2, c2, s2, List.of(
                m("dk_thuy_dieu", Shape.COMBO, Pal.MOON, 14, 0.5, 30, Hit.SLOW, false),     // Thuy Dieu Vu (Waterfowl): 3 dot luot chem
                m("dk_dam_xuyen", Shape.LINE, Pal.MOON, 13, 1.3, 22, Hit.KNOCK_UP, false),  // dam lao toi
                m("dk_lien_tram", Shape.COMBO, Pal.MOON, 5.5, 0.9, 20, Hit.NONE, false),     // chem lien hoan, nhat cuoi tre
                m("dk_lao_khong", Shape.LEAP, Pal.MOON, 4.5, 1.4, 34, Hit.KNOCK_UP, false)), // nhay lao tu tren khong
                fx(ArenaEffect.PURPLE_SKY), null, 0.85f);
        Phase three = new Phase(3, P3_BELOW, 1.5f, t3, r3, c3, s3, List.of(
                m("dk_huyet_hoa", Shape.DONUT, Pal.BLOOD, 16, 2.4, 90, Hit.WEAK, true),        // Huyet Nguyet Khai Hoa (Scarlet Aeonia)
                m("dk_thuy_dieu_huyet", Shape.COMBO, Pal.BLOOD, 15, 0.55, 26, Hit.WEAK, false), // Thuy Dieu Vu mau: de lai vung mau
                m("dk_nguyet_luan", Shape.LINE, Pal.MOON, 18, 1.0, 24, Hit.SLOW, false),       // 3 luoi trang khuyet quat
                m("dk_song_lao", Shape.LEAP, Pal.BLOOD, 4.5, 1.2, 30, Hit.KNOCK_UP, false)),   // lao 2 lan
                fx(ArenaEffect.BLOOD_RAIN, ArenaEffect.CRACKED_FLOOR), "tam_ma", 0.7f);
        ALL.put(id, new Config(id, Pal.MOON, s, List.of(p1(), two, three)));
    }

    /** CHG I: P2 "Hong Thu" (trut giap, vat lon kieu Hoarah Loux), P3 "Liet Dia" (dat nut dung nham). Thoai: hukhong.dialog.chg1.*. */
    private static void chgOne() {
        String id = "chg_1";
        double s = 3.8;
        int t2 = 120, r2 = 60, t3 = 120, r3 = 70;
        List<SubLine> s2 = List.of(new SubLine(8, 44, "hukhong.dialog.chg1.p2.1"), new SubLine(44, 84, "hukhong.dialog.chg1.p2.2"),
                new SubLine(84, 116, "hukhong.dialog.chg1.p2.3"));
        List<SubLine> s3 = List.of(new SubLine(8, 44, "hukhong.dialog.chg1.p3.1"), new SubLine(44, 84, "hukhong.dialog.chg1.p3.2"),
                new SubLine(84, 116, "hukhong.dialog.chg1.p3.3"));
        double a = s * 1.25, b = s * 1.5;
        // P2: cui dau quang riu -> tieng gam (camera thap truoc mat) -> keo ra
        List<CamPath.Key> c2 = List.of(k(0, 0, 0.5, 2.0, 58, 0.7, 0, 0, a), k(40, 1.6, 0.2, 1.4, 52, 0.8, 0, -6, a),
                k(r2, 0, 0.3, 1.6, 70, 0.8, 0, -10, a), k(95, -2.2, 1.0, 2.0, 62, 0.7, 0, 0, a), k(t2, 0, 1.0, 3.8, 72, 0.6, 0, 0, a));
        // P3: dam dat -> nut dung nham, camera tu mat dat vong ra sau lung roi lui xa
        List<CamPath.Key> c3 = List.of(k(0, 0, 0.2, 1.4, 48, 0.9, 0, 0, b), k(35, 2.0, 0.1, 1.0, 62, 1.0, 0, -8, b),
                k(r3, 1.6, 1.2, -1.8, 70, 0.8, 0, -4, b), k(100, -2.4, 1.4, 1.6, 70, 0.8, 0, 0, b), k(t3, 0, 1.6, 4.4, 78, 0.6, 0, 0, b));
        Phase two = new Phase(2, P2_BELOW, 1.25f, t2, r2, c2, s2, List.of(
                m("chg1_cam_na", Shape.LINE, Pal.SAND, 7, 1.8, 16, Hit.KNOCK_UP, false),     // vo + nhac bong + quat xuong
                m("chg1_dia_liet", Shape.LINE, Pal.SAND, 16, 1.0, 24, Hit.KNOCK_UP, false),  // 3 khe nut quat
                m("chg1_hoanh_tao", Shape.COMBO, Pal.SAND, 6.5, 0.9, 14, Hit.NONE, false),   // 2 nhat quet + bo doc
                m("chg1_phi_phu", Shape.LEAP, Pal.SAND, 4.5, 1.4, 30, Hit.KNOCK_UP, false)),  // nhay bo riu
                fx(ArenaEffect.CRACKED_FLOOR), null, 0.85f);
        Phase three = new Phase(3, P3_BELOW, 1.5f, t3, r3, c3, s3, List.of(
                m("chg1_chan_thien", Shape.DONUT, Pal.SAND, 18, 2.4, 80, Hit.KNOCK_UP, true), // Chan Thien Liet Dia (Earthshaker)
                m("chg1_ho_bo", Shape.SHOCK, Pal.SAND, 15, 0.9, 20, Hit.KNOCK_UP, false),     // 3 vong dam lan
                m("chg1_nem_da", Shape.METEOR, Pal.SAND, 3.5, 1.2, 24, Hit.SLOW, false),      // nem da 2 lan
                m("chg1_dap_dat", Shape.SHOCK, Pal.SAND, 12, 1.0, 18, Hit.KNOCK_UP, false)),  // dam 2 nhip
                fx(ArenaEffect.VOID_PILLARS), "su_do", 0.7f);
        ALL.put(id, new Config(id, Pal.SAND, s, List.of(p1(), two, three)));
    }

    /** CHG II: P2 "Hac Kiem xuat vo" (rut kiem bong), P3 "Tu Chung" (chuong tu ngan). Thoai: hukhong.dialog.chg2.*. */
    private static void chg2() {
        String id = "chg_2";
        double s = 3.8;
        List<SubLine> s2 = List.of(new SubLine(8, 40, "hukhong.dialog.chg2.p2.1"), new SubLine(40, 70, "hukhong.dialog.chg2.p2.2"),
                new SubLine(70, 96, "hukhong.dialog.chg2.p2.3"));
        List<SubLine> s3 = List.of(new SubLine(8, 44, "hukhong.dialog.chg2.p3.1"), new SubLine(44, 84, "hukhong.dialog.chg2.p3.2"),
                new SubLine(84, 116, "hukhong.dialog.chg2.p3.3"));
        Phase two = new Phase(2, P2_BELOW, 1.25f, P2_TICKS, 45, camReveal(P2_TICKS, 45, s * 1.25), s2, List.of(
                m("chg2_trao", Shape.COMBO, Pal.HOLY, 5, 0.6, 12, Hit.NONE, false),          // Thu Trao Lien Hoan
                m("chg2_nem_da", Shape.METEOR, Pal.HOLY, 3.5, 1.0, 26, Hit.SLOW, false),     // Phi Thach
                m("chg2_hac_kiem", Shape.COMBO, Pal.VOID, 8, 1.3, 22, Hit.NONE, false),      // Hac Kiem Hoanh Tao
                m("chg2_tu_tram", Shape.LINE, Pal.VOID, 18, 0.9, 12, Hit.NONE, false)),      // Tu Menh Phi Tram
                fx(ArenaEffect.VOID_PILLARS), null, 0.85f);
        Phase three = new Phase(3, P3_BELOW, 1.5f, P3_TICKS, 55, camCrescendo(P3_TICKS, 55, s * 1.5), s3, List.of(
                m("chg2_hac_vu", Shape.DONUT, Pal.VOID, 16, 1.8, 90, Hit.KNOCK_UP, true),    // Hac Kiem Thien Vu (toi thuong)
                m("chg2_ha_khong", Shape.LEAP, Pal.VOID, 5, 1.5, 34, Hit.KNOCK_UP, false),   // Hac Ung Giang
                m("chg2_chuong_tu", Shape.SHOCK, Pal.HOLY, 9, 1.0, 30, Hit.SLOW, false),     // Tu Chung
                m("chg2_nhay", Shape.LEAP, Pal.HOLY, 4, 1.2, 24, Hit.KNOCK_UP, false)),      // Thu Duoc
                fx(ArenaEffect.CRACKED_FLOOR), "su_do", 0.7f);
        ALL.put(id, new Config(id, Pal.HOLY, s, List.of(p1(), two, three)));
    }

    /** CHG III Lang Uyen: P2 "Uyen Mon Khai" (vuc mo sau lung), P3 "Huyet Uyen Trao" (mau nguyen trao). Thoai: hukhong.dialog.chg3.*. */
    private static void chg3() {
        String id = "chg_3";
        double s = 3.8;
        int t2 = 110, r2 = 50, t3 = 120, r3 = 65;
        List<SubLine> s2 = List.of(new SubLine(8, 40, "hukhong.dialog.chg3.p2.1"), new SubLine(40, 76, "hukhong.dialog.chg3.p2.2"),
                new SubLine(76, 106, "hukhong.dialog.chg3.p2.3"));
        List<SubLine> s3 = List.of(new SubLine(8, 44, "hukhong.dialog.chg3.p3.1"), new SubLine(44, 84, "hukhong.dialog.chg3.p3.2"),
                new SubLine(84, 116, "hukhong.dialog.chg3.p3.3"));
        double a = s * 1.25, b = s * 1.5;
        // P2: can mat -> sau lung nhin vet nut vuc mo ra -> keo xa
        List<CamPath.Key> c2 = List.of(k(0, 0, 0.6, 2.2, 58, 0.7, 0, 0, a), k(30, -1.4, 0.4, -1.8, 60, 0.8, 0, -6, a),
                k(r2, 0.3, 0.5, -2.6, 72, 0.9, 0, -10, a), k(85, 2.0, 1.0, 2.0, 62, 0.7, 0, 0, a), k(t2, 0, 1.1, 3.8, 72, 0.6, 0, 0, a));
        // P3: tu mat dat nhin len, mau nguyen trao thanh gai quanh than -> vong ra xa
        List<CamPath.Key> c3 = List.of(k(0, 0, 0.1, 1.5, 48, 0.9, 0, 0, b), k(35, 1.8, 0.1, 1.0, 62, 1.2, 0, -14, b),
                k(r3, 0, 0.0, 2.4, 80, 1.6, 0, -18, b), k(100, -2.4, 1.5, 1.8, 70, 1.0, 0, 0, b), k(t3, 0, 1.6, 4.4, 78, 0.7, 0, 0, b));
        Phase two = new Phase(2, P2_BELOW, 1.25f, t2, r2, c2, s2, List.of(
                m("c3_phi_dao", Shape.LINE, Pal.VOID, 20, 0.6, 24, Hit.SLOW, false),         // Uyen Nhan Tri Phat: phi dao tre
                m("c3_cu_kiem", Shape.COMBO, Pal.VOID, 10, 1.1, 22, Hit.KNOCK_UP, false),     // Uyen Kiem Hien: cu kiem hien giua chuoi
                m("c3_huyet_tri", Shape.PILLARS, Pal.BLOOD, 2.6, 1.0, 30, Hit.KNOCK_UP, false), // Huyet Tri Uyen Thu: vung trao gai
                m("c3_uyen_chuy", Shape.LEAP, Pal.VOID, 5, 1.3, 40, Hit.KNOCK_UP, false)),    // Uyen Chuy Giang: cu chuy tre
                fx(ArenaEffect.PURPLE_SKY), null, 0.85f);
        Phase three = new Phase(3, P3_BELOW, 1.5f, t3, r3, c3, s3, List.of(
                m("c3_van_mau", Shape.PULL_BURST, Pal.VOID, 20, 2.5, 100, Hit.KNOCK_UP, true),  // Van Mau Uyen Giang (toi thuong)
                m("c3_huyet_kiem", Shape.LINE, Pal.BLOOD, 18, 1.2, 24, Hit.WEAK, false),       // Huyet Kiem Uyen Lang: song kiem mau
                m("c3_phi_dao_vu", Shape.LINE, Pal.VOID, 22, 0.6, 24, Hit.WEAK, false),        // Uyen Nhan Vu: quat phi dao
                m("c3_song_chuy", Shape.COMBO, Pal.VOID, 9, 1.2, 50, Hit.KNOCK_UP, false)),    // Song Chuy Uyen Chan
                fx(ArenaEffect.BLOOD_RAIN, ArenaEffect.CRACKED_FLOOR), "su_do", 0.7f);
        ALL.put(id, new Config(id, Pal.VOID, s, List.of(p1(), two, three)));
    }

    /** CHG IV "Co Vo Tan": P2 canh mau bung, P3 huyet diem; toi thuong Hu Vo (Nihil). Thoai: hukhong.dialog.chg4.*. */
    private static void chg4() {
        String id = "chg_4";
        double s = 3.8;
        int t2 = 120, r2 = 60, t3 = 120, r3 = 70;
        List<SubLine> s2 = List.of(new SubLine(8, 44, "hukhong.dialog.chg4.p2.1"), new SubLine(44, 84, "hukhong.dialog.chg4.p2.2"),
                new SubLine(84, 116, "hukhong.dialog.chg4.p2.3"));
        List<SubLine> s3 = List.of(new SubLine(8, 44, "hukhong.dialog.chg4.p3.1"), new SubLine(44, 84, "hukhong.dialog.chg4.p3.2"),
                new SubLine(84, 116, "hukhong.dialog.chg4.p3.3"));
        double a = s * 1.25;
        // P2: quy mot goi tren dinh ba -> canh mau bung (camera sau lung nhin len) -> keo xa
        List<CamPath.Key> c2 = List.of(k(0, 0, 0.4, 2.0, 58, 0.6, 0, 0, a), k(40, -1.4, 0.3, -1.8, 56, 0.8, 0, -8, a),
                k(r2, 0, 0.2, -2.6, 72, 1.0, 0, -14, a), k(95, 2.0, 1.2, 2.0, 62, 0.8, 0, 0, a), k(t2, 0, 1.0, 3.8, 72, 0.7, 0, 0, a));
        // P3: huyet diem bung quanh than - camera sat dat vong quanh roi lui xa
        double b = s * 1.5;
        List<CamPath.Key> c3 = List.of(k(0, 0, 0.2, 1.4, 48, 0.9, 0, 0, b), k(35, 2.0, 0.1, 0.8, 62, 1.2, 0, -10, b),
                k(r3, 0, 0.3, -2.4, 76, 1.0, 0, -6, b), k(100, -2.4, 1.4, 1.8, 70, 1.0, 0, 0, b), k(t3, 0, 1.6, 4.4, 78, 0.8, 0, 0, b));
        Phase two = new Phase(2, P2_BELOW, 1.25f, t2, r2, c2, s2, List.of(
                m("c4_huyet_luan", Shape.NOVA, Pal.BLOOD, 12, 1.0, 30, Hit.KNOCK_UP, false), // vong huyet diem
                m("c4_lao_canh", Shape.LEAP, Pal.BLOOD, 5, 1.3, 28, Hit.KNOCK_UP, false),    // canh mau + lao tu tren khong
                m("c4_quet", Shape.COMBO, Pal.BLOOD, 6.5, 0.9, 14, Hit.WEAK, false),         // 2 nhat quet dinh ba
                m("c4_dam", Shape.LINE, Pal.BLOOD, 14, 1.2, 18, Hit.KNOCK_UP, false)),       // dam luot
                fx(ArenaEffect.BLOOD_RAIN), null, 0.85f);
        Phase three = new Phase(3, P3_BELOW, 1.5f, t3, r3, c3, s3, List.of(
                m("c4_nihil", Shape.DONUT, Pal.BLOOD, 20, 0.8, 100, Hit.WEAK, true),         // Hu Vo (Nihil)
                m("c4_huyet_tuyen", Shape.PILLARS, Pal.BLOOD, 2.8, 1.0, 24, Hit.KNOCK_UP, false), // suoi mau duoi chan
                m("c4_huyet_diem", Shape.LINE, Pal.BLOOD, 16, 1.0, 24, Hit.KNOCK_UP, false), // vet huyet diem tren dat
                m("c4_tam_lien", Shape.COMBO, Pal.BLOOD, 6, 0.8, 10, Hit.KNOCK_UP, false)),  // dam - quet - nen tre
                fx(ArenaEffect.CRACKED_FLOOR, ArenaEffect.PURPLE_SKY), "su_do", 0.7f);
        ALL.put(id, new Config(id, Pal.BLOOD, s, List.of(p1(), two, three)));
    }

    private static void hv(String key, Pal aura, double size, Set<ArenaEffect> a2, Set<ArenaEffect> a3, String music3, int l2, int l3, Move a, Move b, Move ult) {
        String id = "hvf_" + key;
        reg(id, aura, size, p2(id, size, l2, a2, null, a, b), p3(id, size, l3, a3, music3, ult));
    }

    /** Hu Vuong: P2 "Ghep Thu" (tay ghep moc ra, kieu Godrick), P3 "Thon Thien" (tay rong + ran dung nham, kieu Rykard). Thoai: hukhong.dialog.huvuong.*. */
    private static void huVuong() {
        String id = "hu_vuong";
        double s = 4.0;
        List<SubLine> s2 = List.of(new SubLine(8, 40, "hukhong.dialog.huvuong.p2.1"), new SubLine(40, 72, "hukhong.dialog.huvuong.p2.2"),
                new SubLine(72, 96, "hukhong.dialog.huvuong.p2.3"));
        List<SubLine> s3 = List.of(new SubLine(8, 44, "hukhong.dialog.huvuong.p3.1"), new SubLine(44, 84, "hukhong.dialog.huvuong.p3.2"),
                new SubLine(84, 116, "hukhong.dialog.huvuong.p3.3"));
        Phase two = new Phase(2, P2_BELOW, 1.25f, P2_TICKS, 45, camReveal(P2_TICKS, 45, s * 1.25), s2, List.of(
                m("hvg_xa_ngoan", Shape.LINE, Pal.VOID, 16, 1.1, 22, Hit.SLOW, false),       // Xa Than Nuot Hon: ran can + keo
                m("hvg_phong_phu", Shape.COMBO, Pal.FROST, 7.5, 1.0, 18, Hit.KNOCK_UP, false), // Phong Phu Dap: riu gio + dam
                m("hvg_cot_tra", Shape.COMBO, Pal.VOID, 7.5, 0.8, 12, Hit.NONE, false),       // Ghep Thu Loan Vu: 3 nhat quet
                m("hvg_dung_nham", Shape.LINE, Pal.BLOOD, 20, 0.9, 20, Hit.KNOCK_UP, false)), // Dung Nham Cuon Cuon
                fx(ArenaEffect.PURPLE_SKY, ArenaEffect.VOID_PILLARS), null, 0.85f);
        Phase three = new Phase(3, P3_BELOW, 1.5f, P3_TICKS, 55, camCrescendo(P3_TICKS, 55, s * 1.5), s3, List.of(
                m("hvg_thon_thien", Shape.PULL_BURST, Pal.VOID, 18, 2.6, 90, Hit.KNOCK_UP, true), // Hu Khong Thon Thien (toi thuong)
                m("hvg_long_tuc", Shape.LINE, Pal.BLOOD, 12, 0.35, 28, Hit.NONE, false),          // Long Tuc: tay rong phun lua
                m("hvg_lau_cot", Shape.PILLARS, Pal.VOID, 3.5, 1.2, 24, Hit.KNOCK_UP, false),     // Lau Cot Phun Phat: so hu khong
                m("hvg_dia_chan", Shape.DONUT, Pal.BLOOD, 11, 0.9, 16, Hit.KNOCK_UP, false)),     // Dia Chan Tam Dap
                fx(ArenaEffect.CRACKED_FLOOR), "tam_ma", 0.7f);
        ALL.put(id, new Config(id, Pal.VOID, s, List.of(p1(), two, three)));
    }
}
