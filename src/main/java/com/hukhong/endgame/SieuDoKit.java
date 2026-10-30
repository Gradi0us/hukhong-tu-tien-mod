package com.hukhong.endgame;

import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.CamPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Toan thuan (khong phu thuoc MC) cho ket thuc thu ba "Sieu Do" (ending_truth, PLAN-v2 muc 11.2): dieu kien mo, may trang thai
 * Tam Chien (4 tan anh CHG) -> Giu 4 Manh 60s -> man ket, song Hu Khong, kich ban cutscene. Engine that: {@link SieuDo}.
 */
public final class SieuDoKit {
    private SieuDoKit() {}

    // ------------------------------------------------------------------ dieu kien
    public static final int DOCS_NEEDED = 10, HU_ANH_BELOW = 30, SHADES = 4;
    /** Bit thieu: Di thu C / Hu Anh / 4 CHG / vung giai phong. */
    public static final int M_DOCS = 1, M_HU_ANH = 2, M_CHG = 4, M_ZONES = 8;

    public record Status(int docsC, int huAnhAvg, int chgDefeated, int zonesLiberated, int zonesTotal) {
        public int zonesNeeded() { return SieuDoKit.zonesNeeded(zonesTotal); }

        public int missing() {
            int m = 0;
            if (docsC < DOCS_NEEDED) m |= M_DOCS;
            if (huAnhAvg >= HU_ANH_BELOW) m |= M_HU_ANH;
            if (chgDefeated < SHADES) m |= M_CHG;
            if (zonesLiberated < zonesNeeded()) m |= M_ZONES;
            return m;
        }

        public boolean met() { return missing() == 0; }
    }

    /** Nua so vung xam thuc (lam tron xuong, toi thieu 1 neu co vung); 0 neu the gioi khong co vung. */
    public static int zonesNeeded(int total) { return total <= 0 ? 0 : Math.max(1, total / 2); }

    // ------------------------------------------------------------------ may trang thai
    public enum Phase { INTRO, TAM_CHIEN, GIU_MANH, OUTRO, DONE, FAILED }

    public static final int INTRO_TICKS = 240, HOLD_TICKS = 1200, OUTRO_TICKS = 420;
    public static final int WAVE_PERIOD = 100, WAVE_WARN = 40;
    public static final double HOLD_RADIUS = 9.0, BAND_HALF = 1.8;
    public static final double[] WAVE_RADII = {2.5, 5.5, 8.5};
    public static final double WAVE_DAMAGE_FRAC = 0.22;

    /**
     * Buoc ke tiep: INTRO het gio -> TAM_CHIEN; TAM_CHIEN het tan anh -> GIU_MANH; GIU_MANH du {@link #HOLD_TICKS} -> OUTRO; OUTRO het gio -> DONE.
     * Chu nhan chet / roi arena (ownerOk=false) o bat ky pha dang chay -> FAILED. {@code t} = tick da troi trong pha hien tai.
     */
    public static Phase next(Phase cur, int t, int shadesAlive, int hold, boolean ownerOk) {
        if (cur == Phase.DONE || cur == Phase.FAILED) return cur;
        if (!ownerOk && cur != Phase.OUTRO) return Phase.FAILED;
        return switch (cur) {
            case INTRO -> t >= INTRO_TICKS ? Phase.TAM_CHIEN : cur;
            case TAM_CHIEN -> t > 0 && shadesAlive <= 0 ? Phase.GIU_MANH : cur;
            case GIU_MANH -> hold >= HOLD_TICKS ? Phase.OUTRO : cur;
            case OUTRO -> t >= OUTRO_TICKS ? Phase.DONE : cur;
            default -> cur;
        };
    }

    /** Dem giu Manh: chi tang khi chu nhan dung trong vong; roi vong thi dung lai (khong mat tien do). */
    public static int holdAfter(int hold, boolean inside) { return inside ? Math.min(HOLD_TICKS, hold + 1) : hold; }

    // ------------------------------------------------------------------ song Hu Khong
    public static double waveRadius(int waveIdx) { return WAVE_RADII[Math.floorMod(waveIdx, WAVE_RADII.length)]; }

    /** Song trung neu dung tren dai [r-BAND_HALF, r+BAND_HALF] va chan cham dat (nhay de ne). */
    public static boolean waveHits(double dist, double radius, boolean onGround) {
        return onGround && Math.abs(dist - radius) <= BAND_HALF;
    }

    /** Chi so song tai tick {@code t} cua pha giu Manh, va giai doan: [idx, tick trong chu ky]. */
    public static int waveIndex(int t) { return t / WAVE_PERIOD; }

    public static int waveTick(int t) { return t % WAVE_PERIOD; }

    /** Tick (trong chu ky) song bat dau canh bao / no. */
    public static boolean waveWarning(int tickInCycle) { return tickInCycle >= WAVE_PERIOD - WAVE_WARN && tickInCycle < WAVE_PERIOD - 1; }

    public static boolean waveBurst(int tickInCycle) { return tickInCycle == WAVE_PERIOD - 1; }

    public static float waveDamage(float maxHealth) { return Math.max(4f, maxHealth * (float) WAVE_DAMAGE_FRAC); }

    // ------------------------------------------------------------------ tan anh CHG
    /** Cap tan anh: yeu hon ban goc, kep [20, 65] (SuDo toi da 65). */
    public static int shadeLevel(int chgLevel) { return Math.max(20, Math.min(65, chgLevel - 20)); }

    public static final double SHADE_HP_FACTOR = 0.5;

    /** Tick (tu dau pha Tam Chien) tan anh thu i (0..3) len tieng ky uc. */
    public static int shadeLineTick(int i) { return 30 + i * 90; }

    public static String shadeLineKey(int i) { return "hukhong.sieudo.shade." + (i + 1); }

    public static String shadeNameKey() { return "hukhong.sieudo.shade.name"; }

    // ------------------------------------------------------------------ kich ban cutscene (camera quanh Da Khuyet, phu de)
    public static final List<CamPath.Key> INTRO_KEYS = List.of(
            new CamPath.Key(0, 0, 3, 14, 0, 0, 62, 2.6f),
            new CamPath.Key(110, 3, 3, 8, 0, 2, 56, 3.0f),
            new CamPath.Key(INTRO_TICKS, 0, 2.5f, 5, 0, 0, 50, 3.2f));
    public static final List<SubLine> INTRO_SUBS = List.of(
            new SubLine(10, 80, "hukhong.sieudo.sub.intro.1"),
            new SubLine(85, 160, "hukhong.sieudo.sub.intro.2"),
            new SubLine(165, INTRO_TICKS, "hukhong.sieudo.sub.intro.3"));

    public static final List<CamPath.Key> OUTRO_KEYS = List.of(
            new CamPath.Key(0, 0, 2.5f, 7, 0, 0, 60, 3.0f),
            new CamPath.Key(200, -3, 6, 14, 0, -6, 66, 5.0f),
            new CamPath.Key(OUTRO_TICKS, 0, 14, 24, 0, -22, 78, 12f));
    /** Cau cuoi: 1-3 Da Khuyet, 4 Mo Dao (ve / tan hon, xem {@link #modaoKey}), 5 Hang Chieu (chi khi rival_lost). */
    public static List<SubLine> outroSubs(boolean modaoSoul, boolean rivalLost) {
        List<SubLine> l = new ArrayList<>();
        l.add(new SubLine(10, 90, "hukhong.sieudo.sub.outro.1"));
        l.add(new SubLine(95, 170, "hukhong.sieudo.sub.outro.2"));
        l.add(new SubLine(175, 250, "hukhong.sieudo.sub.outro.3"));
        l.add(new SubLine(255, rivalLost ? 330 : OUTRO_TICKS, modaoKey(modaoSoul)));
        if (rivalLost) l.add(new SubLine(335, OUTRO_TICKS, "hukhong.sieudo.sub.outro.hang_chieu"));
        return l;
    }

    public static String modaoKey(boolean soul) { return soul ? "hukhong.sieudo.sub.outro.modao_soul" : "hukhong.sieudo.sub.outro.modao_back"; }

    // ------------------------------------------------------------------ thuong
    public static final String TITLE_FLAG = "title.sieu_do_gia", BANMENH_FLAG = "banmenh.thu_ho", YEU_TOC_FLAG = "wl.yeu_toc_rising";
}
