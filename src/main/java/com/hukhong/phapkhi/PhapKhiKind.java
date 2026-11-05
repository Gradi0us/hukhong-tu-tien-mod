package com.hukhong.phapkhi;

/** 15 loại Pháp Khí: 7 Công (phù), 4 Hộ (phù), 4 Trận (bàn). Dùng được bởi MỌI đạo. */
public enum PhapKhiKind {
    KIEM("kiem_tinh_phu", "Kiếm Tinh Phù", Cat.ATTACK, 0xBFE4FF),
    HOA("hoa_diem_phu", "Hỏa Diễm Phù", Cat.ATTACK, 0xFF6A2A),
    LOI("loi_dinh_phu", "Lôi Đình Phù", Cat.ATTACK, 0x9CC4FF),
    BANG("han_bang_phu", "Hàn Băng Phù", Cat.ATTACK, 0x8FE6FF),
    THO("tho_nham_phu", "Thổ Nham Phù", Cat.ATTACK, 0xB08A4F),
    MA("ma_sat_phu", "Ma Sát Phù", Cat.ATTACK, 0xC01028),
    QUANG("thanh_quang_phu", "Thánh Quang Phù", Cat.ATTACK, 0xFFE066),
    HO_THAN("ho_than_phu", "Hộ Thân Phù", Cat.DEFENSE, 0xFFD866),
    KIM_CANG("kim_cang_phu", "Kim Cang Phù", Cat.DEFENSE, 0xE0B040),
    DON_KHONG("don_khong_phu", "Độn Không Phù", Cat.DEFENSE, 0x8FE6FF),
    HOI_XUAN("hoi_xuan_phu", "Hồi Xuân Phù", Cat.DEFENSE, 0x7FE06A),
    TU_LINH("tu_linh_tran_ban", "Tụ Linh Trận Bàn", Cat.ZONE, 0x7FE0FF),
    KHON("khon_tran_ban", "Khốn Trận Bàn", Cat.ZONE, 0xA050FF),
    SAT("sat_tran_ban", "Sát Trận Bàn", Cat.ZONE, 0xFF4030),
    HO_TRAN("ho_tran_ban", "Hộ Trận Bàn", Cat.ZONE, 0xFFD866);

    public enum Cat { ATTACK, DEFENSE, ZONE }

    public final String id, vi;
    public final Cat cat;
    public final int rgb;

    PhapKhiKind(String id, String vi, Cat cat, int rgb) { this.id = id; this.vi = vi; this.cat = cat; this.rgb = rgb; }

    public String langKey() { return "item.hukhong.phapkhi." + id; }

    public int maxUses(PhapKhiTier t) {
        return switch (cat) {
            case ATTACK -> PhapKhiTier.USES_ATTACK[t.ordinal()];
            case DEFENSE -> PhapKhiTier.USES_DEFENSE[t.ordinal()];
            case ZONE -> PhapKhiTier.USES_ZONE[t.ordinal()];
        };
    }

    public long price(PhapKhiTier t) {
        return switch (cat) {
            case ATTACK -> PhapKhiTier.PRICE_ATTACK[t.ordinal()];
            case DEFENSE -> PhapKhiTier.PRICE_DEFENSE[t.ordinal()];
            case ZONE -> PhapKhiTier.PRICE_ZONE[t.ordinal()];
        };
    }

    /** Hồi chiêu dùng chung cho cả 5 phẩm của loại này (tick). */
    public int cooldownTicks() {
        return switch (cat) {
            case ATTACK -> 30;
            case DEFENSE -> this == DON_KHONG ? 60 : 40;
            case ZONE -> 60;
        };
    }
}
