package com.hukhong.bicanh;

import com.hukhong.creature.CreatureKind;

import java.util.List;
import java.util.Random;

/**
 * Đa dạng Bí Cảnh (2026-11-06): mỗi lượt có 1 Chủ đề (bầy quái + boss riêng), 1 Biến Số, và mỗi tầng sóng có kiểu phòng riêng.
 * Thuần Java, chọn theo seed của lượt.
 */
public final class BiCanhVariety {
    private BiCanhVariety() {}

    /** Chủ đề: quái thường + boss. kinds rỗng = quái Hư (Hư Thi / Hư Cốt / Hư Chú), boss null = Hư Cốt khổng lồ. */
    public enum Theme {
        HU_KHONG(List.of(), null),
        YEU_LAM(CreatureKind.COMMON_BEASTS, null), // boss: Yêu Thú lâu năm nhất
        THACH_DONG(List.of(CreatureKind.THACH_QUY, CreatureKind.THIET_GIAP_TRI_CHU, CreatureKind.SA_TRUNG), CreatureKind.THACH_CU_NHAN),
        AM_MO(List.of(CreatureKind.AM_HON_SU, CreatureKind.QUAN_HON, CreatureKind.BANG_HON), CreatureKind.HUYET_YEU_VUONG),
        LANG_HUYET(List.of(CreatureKind.HU_LANG, CreatureKind.U_ANH_BAO, CreatureKind.HUYET_NHA_TRU), CreatureKind.HU_LANG_VUONG),
        MOC_YEU_LAM(CreatureKind.PLANTS, CreatureKind.VAN_NIEN_THU_VUONG), // 2026-11-06: rừng yêu thực vật
        HOA_NGUC(List.of(CreatureKind.HOA_TINH, CreatureKind.BAO_VIEM_TICH, CreatureKind.DOC_VAN_XA), CreatureKind.HUYET_YEU_VUONG);

        public final List<CreatureKind> kinds;
        public final CreatureKind boss;

        Theme(List<CreatureKind> kinds, CreatureKind boss) { this.kinds = kinds; this.boss = boss; }

        public String key() { return "hukhong.bicanh.theme." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** Biến Số của lượt: hệ số máu / sát thương / tốc độ quái, số rương thưởng thêm. */
    public enum Modifier {
        BINH_THUONG(1.0, 1.0, 0.0, 0),
        CUONG_BAO(1.0, 1.35, 0.0, 1),   // quái đánh đau hơn, thêm 1 phần thưởng
        KIEN_CO(1.5, 1.0, 0.0, 1),      // quái trâu hơn, thêm 1 phần thưởng
        THAN_TOC(1.0, 1.0, 0.25, 1),    // quái nhanh hơn
        PHUC_DUYEN(1.0, 1.0, 0.0, 2);   // may mắn: thêm 2 phần thưởng

        public final double hp, dmg, speed;
        public final int bonusLoot;

        Modifier(double hp, double dmg, double speed, int bonusLoot) { this.hp = hp; this.dmg = dmg; this.speed = speed; this.bonusLoot = bonusLoot; }

        public String key() { return "hukhong.bicanh.mod." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** Kiểu phòng sóng. */
    public enum RoomType {
        SONG,       // sóng thường
        TINH_ANH,   // ít quái hơn + 1 Tinh Anh rất trâu, dọn xong có rương nhỏ
        THU_TRAN,   // 3 đợt liên tiếp, mỗi đợt ít quái hơn
        LINH_TUYEN; // suối linh: không quái, hồi đầy máu + buff

        public String key() { return "hukhong.bicanh.room." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final int THU_TRAN_ROUNDS = 3;
    public static final double ELITE_HP = 4.0, ELITE_DMG = 1.5;

    public static Theme theme(long seed) {
        Theme[] v = Theme.values();
        return v[new Random(seed ^ 0x5EEDL).nextInt(v.length)];
    }

    public static Modifier modifier(long seed) {
        Random r = new Random(seed ^ 0xB1E7L);
        // Bình Thường hay gặp hơn một chút
        int k = r.nextInt(7);
        return k < 2 ? Modifier.BINH_THUONG : Modifier.values()[1 + (k - 2) % 4];
    }

    /** Kiểu phòng cho từng tầng sóng (index 1..waves); tầng 1 luôn là sóng thường, tối đa 1 Linh Tuyền và không ở tầng cuối. */
    public static RoomType[] roomTypes(long seed, int waves) {
        RoomType[] out = new RoomType[waves + 2];
        java.util.Arrays.fill(out, RoomType.SONG);
        Random r = new Random(seed ^ 0x7007L);
        boolean spring = false;
        for (int i = 2; i <= waves; i++) {
            int k = r.nextInt(100);
            if (k < 30) out[i] = RoomType.TINH_ANH;
            else if (k < 55) out[i] = RoomType.THU_TRAN;
            else if (k < 72 && !spring && i < waves) { out[i] = RoomType.LINH_TUYEN; spring = true; }
        }
        return out;
    }

    /** Số quái mỗi đợt theo kiểu phòng (n = số quái sóng thường). */
    public static int mobsFor(RoomType t, int n) {
        return switch (t) {
            case SONG -> n;
            case TINH_ANH -> Math.max(1, n / 2);
            case THU_TRAN -> Math.max(2, (int) Math.ceil(n * 0.6));
            case LINH_TUYEN -> 0;
        };
    }

    /** Năm tuổi Yêu Thú theo bậc Bí Cảnh (boss = +2 bậc, tối đa 100000 năm). */
    public static com.hukhong.creature.YeuThuAge beastAge(BiCanhTier t, boolean boss, Random r) {
        var ages = com.hukhong.creature.YeuThuAge.values();
        int base = Math.min(ages.length - 1, t.ordinal() / 2 + (r.nextInt(3) == 0 ? 1 : 0));
        return ages[Math.min(ages.length - 1, base + (boss ? 2 : 0))];
    }
}
