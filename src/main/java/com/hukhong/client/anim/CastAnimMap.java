package com.hukhong.client.anim;

import com.hukhong.skill.BanMenhSkill;
import com.hukhong.skill.Skill;
import com.hukhong.skillbook.BookSkill;

import java.util.EnumMap;
import java.util.Map;

/**
 * BẢNG ÁNH XẠ DUY NHẤT: mỗi kỹ năng -> đúng một tư thế. Thuần dữ liệu (an toàn nạp trên server).
 * Thêm kỹ năng mới ở enum nào thì phải thêm dòng ở đây (CastAnimMapTest sẽ fail nếu thiếu).
 */
public final class CastAnimMap {
    private CastAnimMap() {}

    private static final Map<Skill, CastAnim> BASE = new EnumMap<>(Skill.class);
    private static final Map<BookSkill, CastAnim> BOOK = new EnumMap<>(BookSkill.class);
    private static final Map<BanMenhSkill, CastAnim> BAN_MENH = new EnumMap<>(BanMenhSkill.class);
    /** Pháp Tu: theo phím Q/E/R/T (6 nguyên tố dùng chung thế; màu/VFX theo nguyên tố do hiệu ứng lo). */
    private static final CastAnim[] PHAP_SLOT = {CastAnim.HAND_SEAL, CastAnim.SWORD_THRUST, CastAnim.CHANNEL, CastAnim.ULT_RAISE};
    /** Vạn Cốt Cộng Minh (phím R khi không dùng tuyệt kỹ Bản Mệnh). */
    public static final CastAnim RESONANCE = CastAnim.CHANNEL;

    static {
        // ---- 16 kỹ năng nền
        BASE.put(Skill.KIEM_1, CastAnim.SWORD_OVERHEAD);   // Trảm Khí
        BASE.put(Skill.KIEM_2, CastAnim.SWORD_SPIN);       // Liên Hoàn Trảm
        BASE.put(Skill.KIEM_ULT, CastAnim.CHANNEL);        // Kiếm Vực
        BASE.put(Skill.MA_1, CastAnim.CLAW_SWIPE);         // Huyết Trảo
        BASE.put(Skill.MA_2, CastAnim.ROAR);               // Ma Khí Hộ Thể
        BASE.put(Skill.MA_ULT, CastAnim.ULT_RAISE);        // Ma Thần Giáng Thế
        BASE.put(Skill.DAO_1, CastAnim.HAND_SEAL);         // Thánh Quang Tiễn
        BASE.put(Skill.DAO_2, CastAnim.HAND_SEAL);         // Bàn Phù
        BASE.put(Skill.DAO_ULT, CastAnim.ULT_RAISE);       // Thánh Giáng
        BASE.put(Skill.THE_1, CastAnim.STOMP);             // Chấn Địa
        BASE.put(Skill.THE_2, CastAnim.GUARD);             // Thiết Bố Sam
        BASE.put(Skill.THE_ULT, CastAnim.ULT_RAISE);       // Kim Thân
        BASE.put(Skill.PHAP_1, PHAP_SLOT[0]);
        BASE.put(Skill.PHAP_2, PHAP_SLOT[1]);
        BASE.put(Skill.PHAP_3, PHAP_SLOT[2]);
        BASE.put(Skill.PHAP_ULT, PHAP_SLOT[3]);
        // ---- 19 kỹ năng Bí Tịch / Công Pháp
        BOOK.put(BookSkill.PHA_KHONG_TRAM, CastAnim.SWORD_THRUST);
        BOOK.put(BookSkill.PHI_KIEM_TRUY_HON, CastAnim.HAND_SEAL);
        BOOK.put(BookSkill.KIEM_VU_HOA, CastAnim.CHANNEL);
        BOOK.put(BookSkill.THIEN_KIEM_GIANG, CastAnim.ULT_RAISE);
        BOOK.put(BookSkill.THANH_TAM_CHU, CastAnim.OPEN_PALMS);
        BOOK.put(BookSkill.THIEN_LOI_CHU, CastAnim.HAND_SEAL);
        BOOK.put(BookSkill.QUANG_TRAN, CastAnim.CHANNEL);
        BOOK.put(BookSkill.THIEN_PHAT, CastAnim.ULT_RAISE);
        BOOK.put(BookSkill.LONG_HO_QUYEN, CastAnim.PUNCH_HEAVY);
        BOOK.put(BookSkill.THIET_SON_KHAO, CastAnim.DASH_LUNGE);
        BOOK.put(BookSkill.BA_THE_CHAN, CastAnim.STOMP);
        BOOK.put(BookSkill.PHA_THIEN_QUYEN, CastAnim.UPPERCUT);
        BOOK.put(BookSkill.HUYET_TE_CHU, CastAnim.CLAW_SWIPE);
        BOOK.put(BookSkill.MA_ANH, CastAnim.DASH_LUNGE);
        BOOK.put(BookSkill.HUYET_TE_DAI, CastAnim.CHANNEL);
        BOOK.put(BookSkill.THON_THIEN_MA_CONG, CastAnim.ROAR);
        BOOK.put(BookSkill.LANG_BA_VI_BO, CastAnim.DASH_LUNGE);
        BOOK.put(BookSkill.HO_THAN_CUONG_KHI, CastAnim.GUARD);
        BOOK.put(BookSkill.HOI_XUAN_THUAT, CastAnim.OPEN_PALMS);
        // ---- 4 tuyệt kỹ Bản Mệnh
        BAN_MENH.put(BanMenhSkill.NHAT_KIEM, CastAnim.SWORD_OVERHEAD);
        BAN_MENH.put(BanMenhSkill.PHAN_XET, CastAnim.ULT_RAISE);
        BAN_MENH.put(BanMenhSkill.DIA_LIET, CastAnim.STOMP);
        BAN_MENH.put(BanMenhSkill.HUYET_HAI, CastAnim.ROAR);
    }

    public static CastAnim of(Skill s) { return s == null ? null : BASE.get(s); }
    public static CastAnim of(BookSkill s) { return s == null ? null : BOOK.get(s); }
    public static CastAnim of(BanMenhSkill s) { return s == null ? null : BAN_MENH.get(s); }
    /** Pháp Tu theo phím 0..3. */
    public static CastAnim phap(int slot) { return slot < 0 || slot > 3 ? null : PHAP_SLOT[slot]; }
}
