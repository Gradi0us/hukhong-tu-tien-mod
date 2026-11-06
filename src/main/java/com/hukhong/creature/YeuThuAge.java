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

    /** Trọng số theo độ sâu trong Đại Sâm Lâm (0 = ngoài rừng / mép, 1 = lõi). */
    public static double[] weights(double depth) {
        double d = Math.max(0, Math.min(1, depth));
        if (d <= 0) return new double[]{60, 30, 9, 1, 0};
        return new double[]{
                Math.max(0, 40 - 40 * d),
                Math.max(5, 35 - 25 * d),
                20 + 15 * d,
                4 + 26 * d,
                d >= 0.7 ? 1 + 9 * (d - 0.7) / 0.3 : 0};
    }

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
