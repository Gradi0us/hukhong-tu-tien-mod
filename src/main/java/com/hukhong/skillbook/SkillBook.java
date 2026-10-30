package com.hukhong.skillbook;

import com.hukhong.core.Path;
import com.hukhong.skill.Skill;

/** Logic thuần: một phím (Q/E/R/T) của một trang đạo đang trỏ tới kỹ năng nào. Dùng chung server + client (HUD). */
public final class SkillBook {
    private SkillBook() {}

    /** Kết quả: đúng một trong hai khác null. */
    public record Resolved(Skill base, BookSkill book) {
        public String id() { return base != null ? base.icon : book.id; }
        public String langKey() { return base != null ? base.langKey() : book.langKey(); }
        public int cost() { return base != null ? base.cost : book.cost; }
        public int baseCooldown() { return base != null ? base.cooldownTicks : book.cooldownTicks; }
        public String icon() { return base != null ? base.icon : book.id; }
    }

    /** Id có hợp lệ để gán vào trang `page` không (kỹ năng nền của đạo, hoặc bí tịch đã học dùng được trên đạo đó). */
    public static boolean assignable(SkillBookData d, Path page, String id) {
        if (page == null || id == null) return false;
        Skill s = Skill.byId(id);
        if (s != null) return s.path == page;
        BookSkill b = BookSkill.byId(id);
        return b != null && b.usableOn(page) && d.learned(id);
    }

    /** Phím `slot` của trang `page`: gán thủ công (còn hợp lệ) > mặc định của đạo (nếu chưa bị gán sang phím khác). */
    public static Resolved resolve(SkillBookData d, Path page, int slot) {
        if (page == null || slot < 0 || slot > 3) return null;
        String id = d.slot(page, slot);
        if (id != null && assignable(d, page, id)) return of(id);
        Skill def = Skill.of(page, slot);
        if (def == null) return null;
        for (int i = 0; i < 4; i++) {
            if (i == slot) continue;
            String o = d.slot(page, i);
            if (def.icon.equals(o) && assignable(d, page, o)) return null; // đã bị chuyển sang phím khác
        }
        return new Resolved(def, null);
    }

    /** Như {@link #resolve(SkillBookData, Path, int)} nhưng phím R luôn nhường Bản Mệnh khi đã mở khóa (cấp tu luyện đủ). */
    public static Resolved resolve(SkillBookData d, Path page, int slot, int playerLevel) {
        if (slot == 2 && com.hukhong.skill.BanMenhSkill.of(page, playerLevel) != null) return null;
        return resolve(d, page, slot);
    }

    /**
     * Như {@link #resolve(SkillBookData, Path, int, int)}, thêm cờ lockT: Pháp Tu Quang/Lôi/Hỏa/Thủy có phím T cố định là
     * Tinh Hà Quang Trụ (Skill.PHAP_ULT) - mọi gán thủ công trên phím T bị bỏ qua, và phap_ult gán sang phím khác cũng không có hiệu lực.
     */
    public static Resolved resolve(SkillBookData d, Path page, int slot, int playerLevel, boolean lockT) {
        if (!lockT || page != Path.PHAP) return resolve(d, page, slot, playerLevel);
        if (slot == 3) return new Resolved(Skill.PHAP_ULT, null);
        if (slot < 0 || slot > 2) return null;
        if (slot == 2 && com.hukhong.skill.BanMenhSkill.of(page, playerLevel) != null) return null;
        String id = d.slot(page, slot);
        if (id != null && !Skill.PHAP_ULT.icon.equals(id) && assignable(d, page, id)) return of(id);
        Skill def = Skill.of(page, slot);
        if (def == null) return null;
        for (int i = 0; i < 3; i++) { // phím T bị bỏ qua khi tìm "đã bị chuyển sang phím khác"
            if (i == slot) continue;
            String o = d.slot(page, i);
            if (def.icon.equals(o) && assignable(d, page, o)) return null;
        }
        return new Resolved(def, null);
    }

    /** Mặt nạ phím cố định: bit i = phím i luôn là kỹ năng mặc định của đạo (không gán/tháo được). T = 8, E = 2. */
    public static final int LOCK_E = 2, LOCK_T = 8;

    /** Nhiều phím cố định cùng lúc (Thổ: E + T). lockMask = 0 => như resolve thường. */
    public static Resolved resolve(SkillBookData d, Path page, int slot, int playerLevel, int lockMask) {
        if (lockMask == 0 || page != Path.PHAP) return resolve(d, page, slot, playerLevel);
        if (slot < 0 || slot > 3) return null;
        if ((lockMask >> slot & 1) != 0) {
            Skill f = Skill.of(page, slot);
            return f == null ? null : new Resolved(f, null);
        }
        if (slot == 2 && com.hukhong.skill.BanMenhSkill.of(page, playerLevel) != null) return null;
        String id = d.slot(page, slot);
        if (id != null && !fixedId(page, lockMask, id) && assignable(d, page, id)) return of(id);
        Skill def = Skill.of(page, slot);
        if (def == null) return null;
        for (int i = 0; i < 4; i++) {
            if (i == slot || (lockMask >> i & 1) != 0) continue;
            String o = d.slot(page, i);
            if (def.icon.equals(o) && assignable(d, page, o)) return null;
        }
        return new Resolved(def, null);
    }

    /** Id này là kỹ năng mặc định của một phím đang bị khoá? */
    public static boolean fixedId(Path page, int lockMask, String id) {
        for (int i = 0; i < 4; i++) {
            if ((lockMask >> i & 1) == 0) continue;
            Skill f = Skill.of(page, i);
            if (f != null && f.icon.equals(id)) return true;
        }
        return false;
    }

    /** Phím có gán thủ công hợp lệ không (R cần biết để nhường cho Bản Mệnh). */
    public static boolean hasAssignment(SkillBookData d, Path page, int slot) {
        String id = d.slot(page, slot);
        return id != null && assignable(d, page, id);
    }

    /** Phẩm tính chi phí Cường Hóa: bí tịch theo phẩm của nó; kỹ năng nền: tối thượng = Huyền, còn lại = Phàm. */
    public static SkillGrade gradeOf(String id) {
        BookSkill b = BookSkill.byId(id);
        if (b != null) return b.grade;
        Skill s = Skill.byId(id);
        return s != null && s.slot == 3 ? SkillGrade.HUYEN : SkillGrade.PHAM;
    }

    public static net.minecraft.network.chat.Component nameOf(String id) {
        Skill s = Skill.byId(id);
        if (s != null) return net.minecraft.network.chat.Component.translatable(s.langKey());
        BookSkill b = BookSkill.byId(id);
        return b == null ? net.minecraft.network.chat.Component.literal(String.valueOf(id))
                : net.minecraft.network.chat.Component.translatableWithFallback(b.langKey(), b.viName);
    }

    public static Resolved of(String id) {
        Skill s = Skill.byId(id);
        if (s != null) return new Resolved(s, null);
        BookSkill b = BookSkill.byId(id);
        return b == null ? null : new Resolved(null, b);
    }
}
