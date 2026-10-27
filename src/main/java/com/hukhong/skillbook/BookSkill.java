package com.hukhong.skillbook;

import com.hukhong.core.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kỹ năng học thêm từ Bí Tịch / Công Pháp (S2 §2.5 mở rộng). Mỗi đạo 4 kỹ năng (Phàm/Huyền/Địa/Thiên) + 3 kỹ năng chung.
 * coef: hệ số nhân Công (attack_power) hoặc Phép lực (spell_power, đạo Đạo); trước khi nhân hệ số Cường Hóa.
 * Thuần dữ liệu để kiểm thử; hiệu ứng nằm ở {@link BookSkillEffects}.
 */
public enum BookSkill {
    // ---- Kiếm Tu
    PHA_KHONG_TRAM("pha_khong_tram", Path.KIEM, SkillGrade.PHAM, 12, 100, 0.90, "Phá Không Trảm",
            "Kiếm khí xuyên thẳng 14 block, 90% Công mỗi mục tiêu trúng."),
    PHI_KIEM_TRUY_HON("phi_kiem_truy_hon", Path.KIEM, SkillGrade.HUYEN, 40, 300, 0.80, "Phi Kiếm Truy Hồn",
            "Gọi 4 thanh phi kiếm tự tìm mục tiêu trong 14 block, mỗi thanh 80% Công."),
    KIEM_VU_HOA("kiem_vu_hoa", Path.KIEM, SkillGrade.DIA, 70, 600, 0.45, "Kiếm Vũ Hoa Lạc",
            "Mưa kiếm tại điểm ngắm (tầm 14), bán kính 6 trong 3 giây, 6 nhịp x 45% Công."),
    THIEN_KIEM_GIANG("thien_kiem_giang", Path.KIEM, SkillGrade.THIEN, 100, 1500, 5.00, "Thiên Kiếm Giáng Thế",
            "Chùm kiếm quang rộng 2,5 block dài 22 block, 500% Công."),
    // ---- Đạo Tu (Phép lực)
    THANH_TAM_CHU("thanh_tam_chu", Path.DAO, SkillGrade.PHAM, 10, 200, 0.50, "Thanh Tâm Chú",
            "Gỡ hiệu ứng xấu và hồi lượng máu bằng 50% Phép lực cho bản thân (cả đồng đội gần nếu bạn không phải Ma tu)."),
    THIEN_LOI_CHU("thien_loi_chu", Path.DAO, SkillGrade.HUYEN, 35, 240, 1.60, "Thiên Lôi Chú",
            "Giáng sét xuống điểm ngắm (tầm 20), bán kính 3, 160% Phép lực."),
    QUANG_TRAN("quang_tran", Path.DAO, SkillGrade.DIA, 60, 700, 0.40, "Quang Trận",
            "Trận ánh sáng bán kính 6 kéo dài 4 giây: 8 nhịp x 40% Phép lực lên quái, hồi máu người trong trận."),
    THIEN_PHAT("thien_phat", Path.DAO, SkillGrade.THIEN, 100, 1500, 5.50, "Thiên Phạt",
            "Sau 1,5 giây cột thánh quang trút xuống điểm ngắm, bán kính 6, 550% Phép lực."),
    // ---- Thể Tu
    LONG_HO_QUYEN("long_ho_quyen", Path.THE, SkillGrade.PHAM, 8, 80, 1.60, "Long Hổ Quyền",
            "Cú đấm quạt 4 block phía trước, 160% Công, hất lùi."),
    THIET_SON_KHAO("thiet_son_khao", Path.THE, SkillGrade.HUYEN, 30, 240, 1.40, "Thiết Sơn Kháo",
            "Húc thẳng 7 block, 140% Công mục tiêu trên đường, được giảm sát thương 3 giây."),
    BA_THE_CHAN("ba_the_chan", Path.THE, SkillGrade.DIA, 60, 500, 2.60, "Bá Thể Chấn",
            "Dậm đất bán kính 6, 260% Công, làm chậm 2 giây, nhận lá chắn nhỏ."),
    PHA_THIEN_QUYEN("pha_thien_quyen", Path.THE, SkillGrade.THIEN, 90, 1400, 5.00, "Phá Thiên Quyền",
            "Bật nhảy rồi giáng xuống bán kính 7, 500% Công, hất tung."),
    // ---- Ma Tu
    HUYET_TE_CHU("huyet_te_chu", Path.MA, SkillGrade.PHAM, 10, 100, 1.10, "Huyết Tế Chú",
            "Tia huyết quang 10 block, 110% Công, hồi 30% sát thương gây ra (tối đa 5% máu)."),
    MA_ANH("ma_anh", Path.MA, SkillGrade.HUYEN, 25, 200, 0.80, "Ma Ảnh",
            "Lướt 8 block hóa thành bóng, bất tử 0,4 giây, 80% Công mục tiêu trên đường."),
    HUYET_TE_DAI("huyet_te_dai", Path.MA, SkillGrade.DIA, 60, 700, 0.35, "Huyết Tế Đài",
            "Vùng huyết bán kính 6 trong 5 giây: 10 nhịp x 35% Công, hút máu hồi cho bản thân."),
    THON_THIEN_MA_CONG("thon_thien_ma_cong", Path.MA, SkillGrade.THIEN, 90, 1600, 4.50, "Thôn Thiên Ma Công",
            "Bộc phát ma khí bán kính 8, 450% Công, hồi 12% máu tối đa cho bản thân."),
    // ---- Chung (mọi đạo)
    LANG_BA_VI_BO("lang_ba_vi_bo", null, SkillGrade.PHAM, 10, 120, 0.0, "Lăng Ba Vi Bộ",
            "Lướt nhanh 9 block theo hướng nhìn (tránh va chạm)."),
    HO_THAN_CUONG_KHI("ho_than_cuong_khi", null, SkillGrade.PHAM, 15, 400, 0.20, "Hộ Thân Cương Khí",
            "Lá chắn hấp thụ bằng 20% máu tối đa trong 8 giây."),
    HOI_XUAN_THUAT("hoi_xuan_thuat", null, SkillGrade.HUYEN, 30, 900, 0.25, "Hồi Xuân Thuật",
            "Hồi 25% máu tối đa cho bản thân.");

    /** Id ngắn dùng trong data / component / payload. Không trùng {@link com.hukhong.skill.Skill#icon}. */
    public final String id;
    /** null = kỹ năng chung, mọi đạo đều học và gắn được. */
    public final Path path;
    public final SkillGrade grade;
    public final int cost, cooldownTicks;
    public final double coef;
    public final String viName, viDesc;

    BookSkill(String id, Path path, SkillGrade grade, int cost, int cooldownTicks, double coef, String viName, String viDesc) {
        this.id = id;
        this.path = path;
        this.grade = grade;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.coef = coef;
        this.viName = viName;
        this.viDesc = viDesc;
    }

    public String langKey() { return "hukhong.skill." + id; }
    public String descKey() { return "hukhong.skill." + id + ".desc"; }

    /** Công Pháp = kỹ năng chung hoặc Đạo; Bí Tịch = võ kỹ các đạo còn lại. */
    public boolean congPhap() { return path == null || path == Path.DAO; }

    /** Nhân Phép lực thay vì Công. */
    public boolean spell() { return path == Path.DAO; }

    public int minLevel() { return grade.minLevel; }

    public boolean usableOn(Path page) { return path == null || path == page; }

    public static BookSkill byId(String id) {
        if (id == null) return null;
        for (BookSkill s : values()) if (s.id.equals(id)) return s;
        return null;
    }

    public static List<BookSkill> ofGrade(SkillGrade g) {
        List<BookSkill> l = new ArrayList<>();
        for (BookSkill s : values()) if (s.grade == g) l.add(s);
        return l;
    }

    /** Kỹ năng của một đạo (không gồm kỹ năng chung). */
    public static List<BookSkill> ofPath(Path p) {
        List<BookSkill> l = new ArrayList<>();
        for (BookSkill s : values()) if (s.path == p) l.add(s);
        return l;
    }

    public static List<BookSkill> universal() { return ofPath(null); }

    /** Kỹ năng người sở hữu các đạo `paths` học được (đạo của mình + chung). */
    public static List<BookSkill> learnableBy(Set<Path> paths) {
        List<BookSkill> l = new ArrayList<>();
        for (BookSkill s : values()) if (s.path == null || paths.contains(s.path)) l.add(s);
        return l;
    }

    public String toString() { return name().toLowerCase(Locale.ROOT); }
}
