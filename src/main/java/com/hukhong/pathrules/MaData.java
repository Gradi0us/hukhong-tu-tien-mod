package com.hukhong.pathrules;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Trạng thái Ma Tu (S2 2.4): Ác Tính, Huyết Tế, lộ, Ác Danh, quota ngày. Mutable, lưu qua copyOnDeath.
 * Trường transient (không lưu) ở cuối.
 */
public final class MaData {
    public double acTinh;          // 0..100
    public double huyetTe;         // 0..100 stack
    public boolean absorb = true;  // true = lộ Hấp Thu, false = Tu Luyện thường
    public double acDanh;          // Ác Danh (sao)
    public long day = -1;          // ngày game của bộ đếm quota
    public double dayGain;         // Huyết Tế đã nhận hôm nay
    public int countdown = -1;     // tick đếm ngược Tẩu Hỏa (-1 = chưa chạy)

    // transient
    public int lostTicks;          // Mất Kiểm Soát còn lại
    public int thanTicks;          // Ma Thần Giáng Thế còn lại
    public int hitSecond;          // số ÔUL nhận từ đòn trong giây hiện tại
    public int lastTier;           // tầng cảnh báo đã thông báo
    public float sentAc = -1; public int sentCd = -2; public boolean sentLost; public int sentHt = -1;

    public static final Codec<MaData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("ac_tinh", 0.0).forGetter(m -> m.acTinh),
            Codec.DOUBLE.optionalFieldOf("huyet_te", 0.0).forGetter(m -> m.huyetTe),
            Codec.BOOL.optionalFieldOf("absorb", true).forGetter(m -> m.absorb),
            Codec.DOUBLE.optionalFieldOf("ac_danh", 0.0).forGetter(m -> m.acDanh),
            Codec.LONG.optionalFieldOf("day", -1L).forGetter(m -> m.day),
            Codec.DOUBLE.optionalFieldOf("day_gain", 0.0).forGetter(m -> m.dayGain),
            Codec.INT.optionalFieldOf("countdown", -1).forGetter(m -> m.countdown)
    ).apply(i, (a, h, ab, ad, d, dg, cd) -> {
        MaData m = new MaData();
        m.acTinh = a; m.huyetTe = h; m.absorb = ab; m.acDanh = ad; m.day = d; m.dayGain = dg; m.countdown = cd;
        return m;
    }));

    /** Xóa sạch (Tẩu Hỏa Nhập Ma / bỏ đạo Ma), giữ lộ mặc định Hấp Thu. */
    public void reset() {
        acTinh = 0; huyetTe = 0; absorb = true; acDanh = 0; day = -1; dayGain = 0; countdown = -1;
        lostTicks = 0; thanTicks = 0; hitSecond = 0; lastTier = 0; sentAc = -1; sentCd = -2; sentLost = false; sentHt = -1;
    }
}
