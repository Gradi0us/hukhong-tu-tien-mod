package com.hukhong.story.chain;

import java.util.List;

/**
 * Toan thuan (khong phu thuoc MC) cho 7 cung truyen thanh (docs/DESIGN-v2-quest-chains.md, muc "Cung truyen thanh"):
 * bang cung, chuyen buoc theo ngu canh ({@link Ctx}), nguon Di thu C. Engine that nam o {@link CityArcs}.
 * Moi cung 3 buoc, la chuoi QuestChains (co {@code qc.<id>.<n>}, {@code qc.<id>.done}); tuyen chinh khong bao gio bi chan.
 */
public final class CityArcKit {
    private CityArcKit() {}

    /**
     * @param id        id chuoi (khoa co qc.*)
     * @param city      chi so thanh (CityDef.ALL)
     * @param cityId    id thanh
     * @param npc       NPC giao viec (dialogue/NPC.json, city = cityId) - dung o thanh qua DialogueService
     * @param chMin     chuong bat dau (giao tu chuong nay tro di, khong bao gio dong)
     * @param chMax     chuong cuoi cua cua so "dung chuong" - dung cho Lich (lane truyen)
     * @param minLevel  cap toi thieu de nhan
     * @param docs      Van Kien trao khi xong (a/b/c); phan tu dau di qua StoryHooks.complete, con lai StoryService.collect
     * @param exp       EXP khi xong
     * @param setsFlags co dat khi xong (tham chieu boi thoai / noi dung sau)
     * @param modaoKey  cau Mo Dao binh luan (xep qua StoryHooks)
     * @param calEvent  id su kien Lich lane truyen
     */
    public record Arc(String id, int city, String cityId, String npc, int chMin, int chMax, int minLevel, List<String> docs, long exp,
                      List<String> setsFlags, String modaoKey, String calEvent) {
        public String doneFlag() { return QuestChains.doneFlag(id); }
        public String hintKey(int stage) { return "hukhong.qc." + id + ".hint." + stage; }
    }

    public static final String CHO_MAU = "cho_mau", HAC_TRIEU = "hac_trieu", DAU_GIA = "dau_gia", LOI_VU = "loi_vu",
            DAI_HOI = "dai_hoi", KIEM_TRUNG = "kiem_trung", HOANG_DO = "hoang_do";

    public static final List<Arc> ARCS = List.of(
            new Arc(CHO_MAU, 3, "huyet_nguyet_phuong", "chu_cho_den", 4, 4, 30, List.of("a7", "c1"), 900,
                    List.of("wl.black_market_open"), "hukhong.dlg.modao.wl.arc_cho_mau", "arc_cho_mau"),
            new Arc(HAC_TRIEU, 4, "dan_ha_thanh", "hai_tuong", 5, 5, 40, List.of("c2"), 1100,
                    List.of("wl.sea_threat"), "hukhong.dlg.modao.wl.arc_hac_trieu", "arc_hac_trieu"),
            new Arc(DAU_GIA, 5, "co_xao_thanh", "dau_gia_su", 5, 6, 40, List.of("b7", "c3"), 1200,
                    List.of("wl.auction_open"), "hukhong.dlg.modao.wl.arc_dau_gia", "arc_dau_gia"),
            new Arc(LOI_VU, 6, "lac_nhat_thanh", "lao_gia_bi_an", 7, 7, 60, List.of("c4"), 1600,
                    List.of("wl.chg1_path"), "hukhong.dlg.modao.wl.arc_loi_vu", "arc_loi_vu"),
            new Arc(DAI_HOI, 7, "thanh_van_thanh", "tong_chu_thanh_van", 8, 8, 70, List.of("b8", "c5"), 2000,
                    List.of("wl.tournament_won"), "hukhong.dlg.modao.wl.arc_dai_hoi", "arc_dai_hoi"),
            new Arc(KIEM_TRUNG, 8, "bac_han_thanh", "kiem_si", 9, 9, 80, List.of("c6", "c7"), 2400,
                    List.of("wl.chg3_path"), "hukhong.dlg.modao.wl.arc_kiem_trung", "arc_kiem_trung"),
            new Arc(HOANG_DO, 9, "thien_khai_hoang_do", "hoang_toc_sot", 10, 10, 90, List.of("a9", "c8", "c9"), 3000,
                    List.of("wl.hoang_do_retaken"), "hukhong.dlg.modao.wl.arc_hoang_do", "arc_hoang_do"));

    public static Arc byId(String id) {
        for (Arc a : ARCS) if (a.id().equals(id)) return a;
        return null;
    }

    public static Arc byNpc(String npc) {
        for (Arc a : ARCS) if (a.npc().equals(npc)) return a;
        return null;
    }

    public static boolean isArc(String chain) { return byId(chain) != null; }

    public static final int PEARLS_NEEDED = 5, NIGHT_FROM = 13000, NIGHT_TO = 23000;
    public static final int NEAR_CITY = 90, REACH = 20;

    /** Gio ban dem (tick trong ngay MC 0..23999). */
    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= NIGHT_FROM && t < NIGHT_TO;
    }

    /** Ngu canh chuyen buoc: engine dien tu the gioi, kit chi quyet dinh buoc ke. */
    public record Ctx(boolean night, boolean nearCity, boolean reached, boolean cleared, int pearls, boolean sceneDone,
                      boolean stormSurvived, boolean rivalDone, boolean modaoDone, boolean retaken) {
        public static final Ctx NONE = new Ctx(false, false, false, false, 0, false, false, false, false, false);
        public Ctx withNight(boolean v) { return new Ctx(v, nearCity, reached, cleared, pearls, sceneDone, stormSurvived, rivalDone, modaoDone, retaken); }
        public Ctx withNearCity(boolean v) { return new Ctx(night, v, reached, cleared, pearls, sceneDone, stormSurvived, rivalDone, modaoDone, retaken); }
        public Ctx withReached(boolean v) { return new Ctx(night, nearCity, v, cleared, pearls, sceneDone, stormSurvived, rivalDone, modaoDone, retaken); }
        public Ctx withCleared(boolean v) { return new Ctx(night, nearCity, reached, v, pearls, sceneDone, stormSurvived, rivalDone, modaoDone, retaken); }
        public Ctx withPearls(int v) { return new Ctx(night, nearCity, reached, cleared, v, sceneDone, stormSurvived, rivalDone, modaoDone, retaken); }
        public Ctx withScene(boolean v) { return new Ctx(night, nearCity, reached, cleared, pearls, v, stormSurvived, rivalDone, modaoDone, retaken); }
        public Ctx withStorm(boolean v) { return new Ctx(night, nearCity, reached, cleared, pearls, sceneDone, v, rivalDone, modaoDone, retaken); }
        public Ctx withRival(boolean v) { return new Ctx(night, nearCity, reached, cleared, pearls, sceneDone, stormSurvived, v, modaoDone, retaken); }
        public Ctx withModao(boolean v) { return new Ctx(night, nearCity, reached, cleared, pearls, sceneDone, stormSurvived, rivalDone, v, retaken); }
        public Ctx withRetaken(boolean v) { return new Ctx(night, nearCity, reached, cleared, pearls, sceneDone, stormSurvived, rivalDone, modaoDone, v); }
    }

    /** Buoc ke tiep (1..3) hoac 9 = xong; tra {@code stage} neu chua du dieu kien. Buoc "noi chuyen" (hac_trieu 3, loi_vu 3, hoang_do 3) do thoai chot. */
    public static int advance(String arc, int stage, Ctx c) {
        return switch (arc) {
            case CHO_MAU -> switch (stage) {
                case 1 -> c.night() && c.nearCity() ? 2 : 1;
                case 2 -> c.reached() ? 3 : 2;
                case 3 -> c.cleared() ? 9 : 3;
                default -> stage;
            };
            case HAC_TRIEU -> switch (stage) {
                case 1 -> c.cleared() ? 2 : 1;
                case 2 -> c.pearls() >= PEARLS_NEEDED ? 3 : 2;
                default -> stage; // 3 = thoai Hai Tuong
            };
            case DAU_GIA -> switch (stage) {
                case 1 -> c.sceneDone() ? 2 : 1;
                case 2 -> c.reached() ? 3 : 2;
                case 3 -> c.cleared() ? 9 : 3;
                default -> stage;
            };
            case LOI_VU -> switch (stage) {
                case 1 -> c.stormSurvived() ? 3 : c.reached() ? 2 : 1;
                case 2 -> c.stormSurvived() ? 3 : 2;
                default -> stage; // 3 = bao lai Lao Gia
            };
            case DAI_HOI -> switch (stage) {
                case 1, 2 -> c.cleared() ? stage + 1 : stage;
                case 3 -> c.rivalDone() ? 9 : 3;
                default -> stage;
            };
            case KIEM_TRUNG -> switch (stage) {
                case 1 -> c.reached() ? 2 : 1;
                case 2 -> c.cleared() ? 3 : 2;
                case 3 -> c.modaoDone() ? 9 : 3;
                default -> stage;
            };
            case HOANG_DO -> switch (stage) {
                case 1 -> c.nearCity() ? 2 : 1;
                case 2 -> c.retaken() ? 3 : 2;
                default -> stage; // 3 = thoai Hoang Toc
            };
            default -> stage;
        };
    }

    /** Dieu kien giao: chua bat dau, chua xong, du chuong + cap. */
    public static boolean offerApplies(Arc a, List<String> flags, int chapter, int level) {
        return chapter >= a.chMin() && level >= a.minLevel() && !flags.contains(a.doneFlag()) && QuestChains.stageOf(flags, a.id()) == 0;
    }

    // ------------------------------------------------------------------ nguon Di thu C (c1..c10)
    /** Nguon co dinh cua tung Di thu C (de kiem thu "du 10"); vung xam thuc >= 450 la nguon bo sung ngau nhien (ZoneMath.clueFor). */
    /** Di thu C trao khi ha Chap Hanh Gia idx (chi Chap Hanh Gia III), hoac null. */
    public static String chgClue(int idx) { return idx == 3 ? "c10" : null; }

    /** Tat ca Di thu C ma cac nguon co dinh (cung + Chap Hanh Gia III) bao phu. */
    public static java.util.Set<String> guaranteedC() {
        java.util.Set<String> s = new java.util.TreeSet<>();
        for (Arc a : ARCS) for (String d : a.docs()) if (d.startsWith("c")) s.add(d);
        for (int i = 1; i <= 4; i++) { String c = chgClue(i); if (c != null) s.add(c); }
        return s;
    }
}
