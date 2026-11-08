package com.hukhong.creature;

import net.minecraft.util.RandomSource;

/**
 * Tu vi (năm tuổi) của Yêu Thú: 10 / 100 / 1.000 / 10.000 / 100.000 năm.
 * Năm càng cao: máu, sát thương, giáp, kích thước, EXP, Hồn Tinh tăng; từ 1.000 năm có thể rơi Hồn Cốt.
 * Màu vòng hào quang: trắng, vàng, tím, đen, đỏ.
 */
public enum YeuThuAge {
    //       năm      máu   công  giáp  scale  exp   màu        hồnCốt%  bậc hồn cốt
    Y10(10,         1.0,  1.0,  0,   1.00, 1.0,  0xF2F2F2, 0.00, 0),
    Y100(100,       1.8,  1.5,  2,   1.10, 2.0,  0xFFD84A, 0.00, 0),
    Y1000(1000,     3.5,  2.3,  5,   1.25, 4.0,  0xA64DFF, 0.03, com.hukhong.honcot.HonCotMath.TIER_TU),
    Y10000(10000,   8.0,  3.6,  10,  1.45, 9.0,  0x2A2A2A, 0.12, com.hukhong.honcot.HonCotMath.TIER_HAC),
    Y100000(100000, 22.0, 6.0,  18,  1.80, 25.0, 0xFF2A2A, 0.55, com.hukhong.honcot.HonCotMath.TIER_HONG);

    public final int years;
    public final double hpMul, dmgMul, armor, scale, expMul;
    public final int color;
    public final double boneChance;
    public final int boneTier;

    YeuThuAge(int years, double hpMul, double dmgMul, double armor, double scale, double expMul, int color, double boneChance, int boneTier) {
        this.years = years; this.hpMul = hpMul; this.dmgMul = dmgMul; this.armor = armor; this.scale = scale;
        this.expMul = expMul; this.color = color; this.boneChance = boneChance; this.boneTier = boneTier;
    }

    public static YeuThuAge of(int ord) { return values()[Math.max(0, Math.min(values().length - 1, ord))]; }

    /** Vùng rừng theo độ sâu: 0 = rìa ngoài, 1 = vùng giữa, 2 = vùng hạch tâm. */
    public static final double MID_DEPTH = 0.4, CORE_DEPTH = 0.75;
    public static int zone(double depth) { return depth >= CORE_DEPTH ? 2 : depth >= MID_DEPTH ? 1 : 0; }

    /** Trọng số bậc theo vùng (2026-11-08): rìa ngoài 10-100 năm (hiếm 1.000), vùng giữa 1.000-10.000 năm,
     *  hạch tâm hồn thú mạnh (vạn năm, thập vạn năm). Ngoài rừng = như rìa ngoài. */
    public static double[] weights(double depth) {
        return switch (zone(Math.max(0, Math.min(1, depth)))) {
            case 0 -> new double[]{70, 29, 1, 0, 0};
            case 1 -> new double[]{0, 15, 55, 30, 0};
            default -> new double[]{0, 0, 10, 55, 35};
        };
    }

    /** Năm lẻ trong bậc (vd 10 -> 10..99, 100 -> 100..999), nghiêng về đầu thấp; bậc 100.000 tới 999.999. */
    public int rollYears(double u) {
        double e = Math.pow(Math.max(0, Math.min(0.999999, u)), 1.8);
        return (int) Math.min(years * 10L - 1, Math.round(years * Math.pow(10, e)));
    }

    /** Bậc ứng với số năm cụ thể. */
    public static YeuThuAge forYears(int y) {
        YeuThuAge r = Y10;
        for (YeuThuAge a : values()) if (y >= a.years) r = a;
        return r;
    }

    /** Tỉ lệ sinh Hồn Thú Dị Chủng (shiny) theo vùng. */
    public static double shinyChance(double depth) { return new double[]{0.006, 0.010, 0.015}[zone(Math.max(0, Math.min(1, depth)))]; }

    public static YeuThuAge roll(double depth, RandomSource r) { return pick(weights(depth), r.nextDouble()); }

    public static YeuThuAge pick(double[] w, double u) {
        double sum = 0;
        for (double v : w) sum += v;
        double x = u * sum;
        for (int i = 0; i < w.length; i++) { x -= w[i]; if (x < 0) return values()[i]; }
        return values()[w.length - 1];
    }

    /** Khóa lang tên năm tuổi. */
    public String langKey() { return "hukhong.yeu_thu.age." + years; }
}
