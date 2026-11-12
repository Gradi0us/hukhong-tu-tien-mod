package com.hukhong.trungsinh;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Trạng thái Trùng Sinh của người chơi (attachment, copyOnDeath nên sống qua chết).
 * used: đã dùng đan (mỗi người 1 lần). confirmTick: game tick lần dùng đầu (-1 = chưa); dùng lại trong cửa sổ = xác nhận.
 * rebirthPending: đang chờ xác nhận chọn lại đạo sau Trùng Sinh (CharCreateService.confirm kiểm tra cờ này).
 */
public record TrungSinhData(boolean used, long confirmTick, boolean rebirthPending) {
    public static final TrungSinhData DEFAULT = new TrungSinhData(false, -1L, false);

    public static final Codec<TrungSinhData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("used", false).forGetter(TrungSinhData::used),
            Codec.LONG.optionalFieldOf("confirm_tick", -1L).forGetter(TrungSinhData::confirmTick),
            Codec.BOOL.optionalFieldOf("rebirth_pending", false).forGetter(TrungSinhData::rebirthPending)
    ).apply(i, TrungSinhData::new));

    public TrungSinhData withConfirmTick(long t) { return new TrungSinhData(used, t, rebirthPending); }

    public TrungSinhData withRebirthPending(boolean p) { return new TrungSinhData(used, confirmTick, p); }

    public TrungSinhData withUsed() { return new TrungSinhData(true, -1L, rebirthPending); }
}
