package com.hukhong.zone.form;

import com.hukhong.zone.ZoneMath;

/** Ten rieng Hu Vuong (thuan): "[Danh hieu] dang Hu Vuong — ten rieng", ten rieng co dinh theo vi tri vung (vi tri sinh tu seed the gioi). */
public final class HvNames {
    private HvNames() {}

    public static final String[] PERSONAL = {
            "Mạc Trầm", "Tô Hàn", "Cổ Tịch", "Lăng Uyên", "Diệp Sương", "Hoắc Thiên", "Vân Ngạn", "Lục Cốt", "Tạ Minh", "Âu Dạ",
            "Ngụy Tàn", "Thanh Khuyết", "Bạch Mộ", "Đoạn Phong", "Kỷ Lăng", "Nghiêm Sát", "Tiêu Vong", "Hạ Thương", "Phù Dao", "Giang Tuyệt",
            "Mộ Hư", "Trì Ngọc", "Úc Tàn", "Quý Hoang", "Lạc Trầm", "Sở Cốt", "Dư Hàn", "Vệ Tịch", "Thượng Quan Diệt", "Đàm Uyên",
            "Hồ Minh Tàn", "Kiều Dạ", "Lôi Vong", "Phó Ảnh", "Ân Tuyết", "Viên Khuyết", "Bùi Thạch", "Tịch Dương", "Nhạc Tử", "Cung Hư"};

    /** Chi so ten, tat dinh theo (id, cx, cz). */
    public static int index(int zoneId, int cx, int cz) {
        long h = (cx * 73856093L) ^ (cz * 19349663L) ^ (zoneId * 83492791L);
        h ^= (h >>> 21);
        return (int) Math.floorMod(h, (long) PERSONAL.length);
    }

    public static String personal(int zoneId, int cx, int cz) { return PERSONAL[index(zoneId, cx, cz)]; }

    /** Danh hieu + ten dang + Hu Vuong + ten rieng. tier: 0 khong, 1 Tinh Anh, 2 Co. */
    public static String full(HvForm form, int tier, String personal) {
        String t = ZoneMath.tierLabel(tier);
        return (t.isEmpty() ? "" : t + " ") + form.vi + " Hư Vương — " + personal;
    }
}
