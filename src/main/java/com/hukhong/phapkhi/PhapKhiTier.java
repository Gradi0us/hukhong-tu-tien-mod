package com.hukhong.phapkhi;

/** Năm phẩm Pháp Khí: Phàm / Linh / Huyền / Địa / Thiên. Mọi số cân bằng theo phẩm nằm ở đây (một chỗ chỉnh). */
public enum PhapKhiTier {
    PHAM("pham", "Phàm", 0xAAAAAA),
    LINH("linh", "Linh", 0x55FF77),
    HUYEN("huyen", "Huyền", 0x5599FF),
    DIA("dia", "Địa", 0xB060FF),
    THIEN("thien", "Thiên", 0xFFAA00);

    public final String id, vi;
    public final int rgb;

    PhapKhiTier(String id, String vi, int rgb) { this.id = id; this.vi = vi; this.rgb = rgb; }

    public static PhapKhiTier of(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }

    /** Hệ số "Toàn Lực" x max(Công lực, Phép lực): Phàm 3x -> Thiên 12x. */
    public static final double[] FULL_COEF = {3, 5, 7.5, 10, 12};
    /** Hệ số "Kỹ Năng" mỗi mục tiêu / mỗi đòn (nhiều đòn, nhiều mục tiêu nên thấp hơn Toàn Lực). */
    public static final double[] SKILL_COEF = {1.2, 2.0, 3.0, 4.5, 6.0};
    /** Bán kính nổ Toàn Lực (block). */
    public static final double[] FULL_RADIUS = {2.5, 3.0, 3.5, 4.5, 6.0};
    /** Số lần dùng: công / hộ / trận. */
    public static final int[] USES_ATTACK = {3, 4, 6, 8, 10};
    public static final int[] USES_DEFENSE = {3, 5, 7, 10, 15};
    public static final int[] USES_ZONE = {2, 3, 4, 5, 6};
    /** Giá cửa hàng (Hạ phẩm Tử Tinh) cho Công / Hộ / Trận; bậc Uy Danh tối thiểu để mua. */
    public static final long[] PRICE_ATTACK = {300, 1500, 6000, 24000, 100000};
    public static final long[] PRICE_DEFENSE = {240, 1200, 5000, 20000, 90000};
    public static final long[] PRICE_ZONE = {360, 1800, 7200, 28000, 120000};
    public static final int[] MIN_RANK = {0, 1, 2, 3, 4};
    /** Xác suất rơi từ boss (theo cấp boss) / Bí Cảnh (theo cấp Bí Cảnh) đã chia ra phẩm ở PhapKhiDrops. */
}
