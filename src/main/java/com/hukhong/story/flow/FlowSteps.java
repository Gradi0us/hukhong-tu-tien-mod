package com.hukhong.story.flow;

import java.util.ArrayList;
import java.util.List;

/**
 * Bảng bước truyện tuyến chính (DESIGN-v1-story-flow.md). Tuyến tính theo thứ tự khai báo;
 * bước có group != "all" chỉ áp cho xuất thân tương ứng (city / sect / village).
 */
public final class FlowSteps {
    private FlowSteps() {}

    public enum Kind { TALK, KILL, REACH, ALTAR, LINH_KHI, MILESTONE, LEVEL, SECT, DOCS, LAIR_SEEN, CHG, INVASION, BICANH, DUEL, GATE, ENDING }

    /** n: số lượng / cấp / chỉ số CHG; arg: id thành / npc. */
    public record Step(String id, int chapter, Kind kind, int n, String arg, String group) {
        public String objKey() { return "hukhong.flow.obj." + id; }
    }

    public static final List<Step> ALL = new ArrayList<>();

    private static void add(String id, int ch, Kind k, int n, String arg, String group) { ALL.add(new Step(id, ch, k, n, arg, group)); }

    private static void add(String id, int ch, Kind k, int n, String arg) { add(id, ch, k, n, arg, "all"); }

    /** Cuối mỗi chương: tu luyện tới mốc rồi đột phá ở Ấn Hồn Đài. */
    private static void tail(int ch) {
        add("c" + ch + "_grind", ch, Kind.LEVEL, ch * 10, "");
        add("c" + ch + "_ms", ch, Kind.MILESTONE, ch * 10, "");
    }

    static {
        // Chương 0: mở đầu theo xuất thân
        add("pro_city_raid", 0, Kind.KILL, 5, "hu_thi", "city");
        add("pro_city_road", 0, Kind.REACH, 60, "luu_ly_cang", "city");
        add("pro_sect_talk", 0, Kind.TALK, 0, "su_phu", "sect");
        add("pro_sect_meditate", 0, Kind.LINH_KHI, 0, "", "sect");
        add("pro_sect_kill", 0, Kind.KILL, 5, "hu_thi", "sect");
        add("pro_sect_road", 0, Kind.REACH, 60, "luu_ly_cang", "sect");
        add("pro_vil_raid", 0, Kind.KILL, 5, "hu_thi", "village");
        add("pro_vil_road", 0, Kind.REACH, 60, "luu_ly_cang", "village");
        add("pro_meet", 0, Kind.TALK, 0, "mo_dao");
        add("pro_altar", 0, Kind.ALTAR, 0, "");
        // Chương 1: Hạt Bụi (1-9)
        add("c1_talk", 1, Kind.TALK, 0, "mo_dao");
        tail(1);
        // Chương 2: Nhập Đạo (10-19)
        add("c2_sect", 2, Kind.SECT, 0, "");
        add("c2_rival", 2, Kind.TALK, 0, "hang_chieu");
        tail(2);
        // Chương 3: Tông Sư (20-29)
        add("c3_bicanh", 3, Kind.BICANH, 0, "co_tien_thanh");
        add("c3_docs", 3, Kind.DOCS, 1, "");
        tail(3);
        // Chương 4: Bóng Dạ (30-39)
        add("c4_docs", 4, Kind.DOCS, 2, "");
        add("c4_hua_co", 4, Kind.TALK, 0, "hua_co");
        tail(4);
        // Chương 5: Cơ Duyên Bị Cướp (40-49)
        add("c5_invasion", 5, Kind.INVASION, 0, "luu_ly_cang");
        add("c5_rival", 5, Kind.DUEL, 0, "hang_chieu_r");
        tail(5);
        // Chương 6: Địa Môn - lộ hang ổ (50-59)
        add("c6_lair", 6, Kind.LAIR_SEEN, 0, "");
        add("c6_modao", 6, Kind.TALK, 0, "mo_dao");
        tail(6);
        // Chương 7: Thạch Hống (60-69)
        add("c7_chg1", 7, Kind.CHG, 1, "");
        tail(7);
        // Chương 8: Từ Bi (70-79)
        add("c8_rival", 8, Kind.DUEL, 0, "hang_chieu_r");
        add("c8_chg2", 8, Kind.CHG, 2, "");
        tail(8);
        // Chương 9: Kiếm Vong (80-89)
        add("c9_chg3", 9, Kind.CHG, 3, "");
        add("c9_modao", 9, Kind.TALK, 0, "mo_dao");
        tail(9);
        // Chương 10: Khảo Hạch (90-100)
        add("c10_grind", 10, Kind.LEVEL, 98, "");
        add("c10_chg4", 10, Kind.CHG, 4, "");
        add("c10_gate", 10, Kind.GATE, 0, "");
        add("c10_end", 10, Kind.ENDING, 0, "");
    }

    public static Step get(String id) {
        for (Step s : ALL) if (s.id().equals(id)) return s;
        return null;
    }

    /** Nhóm prologue theo id xuất thân. */
    public static String groupOf(String origin) {
        return switch (origin == null ? "" : origin) {
            case "city", "noble" -> "city";
            case "sect" -> "sect";
            default -> "village";
        };
    }

    /** Bước kế tiếp áp dụng cho nhóm, hoặc null nếu hết luồng. */
    public static Step next(Step cur, String group) {
        int i = ALL.indexOf(cur);
        for (int j = i + 1; j < ALL.size(); j++) {
            Step s = ALL.get(j);
            if (s.group().equals("all") || s.group().equals(group)) return s;
        }
        return null;
    }

    /** Bước đầu tiên của chương (theo nhóm). */
    public static Step firstOf(int chapter, String group) {
        for (Step s : ALL) if (s.chapter() == chapter && (s.group().equals("all") || s.group().equals(group))) return s;
        return null;
    }

    /** Chương theo cảnh giới: mốc khóa chưa mở vẫn ở chương cũ. */
    public static int chapterFor(int level, boolean locked) {
        int raw = level / 10 + 1;
        if (locked && level % 10 == 0 && level <= 90) raw--;
        return Math.max(1, Math.min(10, raw));
    }
}
