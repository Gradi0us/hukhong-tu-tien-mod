package com.hukhong.society;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Data attachment tông môn của người chơi. sect="" = chưa vào tông. invite = thư mời đang chờ.
 * totalCh = CH tích lũy (xét bậc); wallet = CH khả dụng (trả phí Linh Khí Đài); cycle/cycleEarned = trần CH theo Chu Kỳ.
 * daiBi = số lần thắng Đại Bỉ (0..2, cửa ải lên Chân Truyền / Trưởng Lão); chain = bước chuỗi nhiệm vụ (0..2).
 */
public record SectData(String sect, int totalCh, String quest, int progress, String invite, long cycle, int cycleEarned,
                       int wallet, int daiBi, int chain) {
    public static final SectData DEFAULT = new SectData("", 0, "", 0, "", 0, 0, 0, 0, 0);

    public static final Codec<SectData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("sect", "").forGetter(SectData::sect),
            Codec.INT.optionalFieldOf("total_ch", 0).forGetter(SectData::totalCh),
            Codec.STRING.optionalFieldOf("quest", "").forGetter(SectData::quest),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(SectData::progress),
            Codec.STRING.optionalFieldOf("invite", "").forGetter(SectData::invite),
            Codec.LONG.optionalFieldOf("cycle", 0L).forGetter(SectData::cycle),
            Codec.INT.optionalFieldOf("cycle_earned", 0).forGetter(SectData::cycleEarned),
            Codec.INT.optionalFieldOf("wallet", -1).forGetter(SectData::wallet),
            Codec.INT.optionalFieldOf("dai_bi", 0).forGetter(SectData::daiBi),
            Codec.INT.optionalFieldOf("chain", 0).forGetter(SectData::chain)
    ).apply(i, (s, t, q, p, iv, c, ce, w, db, ch) -> new SectData(s, t, q, p, iv, c, ce, w < 0 ? t : w, db, ch)));

    public boolean member() { return !sect.isEmpty(); }

    public SectData withSect(String s) { return new SectData(s, totalCh, "", 0, "", cycle, cycleEarned, wallet, 0, 0); }
    public SectData withQuest(String q, int p) { return new SectData(sect, totalCh, q, p, invite, cycle, cycleEarned, wallet, daiBi, chain); }
    public SectData withProgress(int p) { return withQuest(quest, p); }
    public SectData withInvite(String s) { return new SectData(sect, totalCh, quest, progress, s, cycle, cycleEarned, wallet, daiBi, chain); }
    public SectData withWallet(int w) { return new SectData(sect, totalCh, quest, progress, invite, cycle, cycleEarned, Math.max(0, w), daiBi, chain); }
    public SectData withDaiBi(int n) { return new SectData(sect, totalCh, quest, progress, invite, cycle, cycleEarned, wallet, n, chain); }
    public SectData withChain(int n) { return new SectData(sect, totalCh, quest, progress, invite, cycle, cycleEarned, wallet, daiBi, n); }

    /** Cộng CH (đã áp trần), trả bản mới; tự reset bộ đếm khi sang Chu Kỳ mới. */
    public SectData withEarned(long curCycle, int ch) {
        int earned = curCycle == cycle ? cycleEarned : 0;
        return new SectData(sect, totalCh + ch, quest, progress, invite, curCycle, earned + ch, wallet + ch, daiBi, chain);
    }

    public int earnedIn(long curCycle) { return curCycle == cycle ? cycleEarned : 0; }
}
