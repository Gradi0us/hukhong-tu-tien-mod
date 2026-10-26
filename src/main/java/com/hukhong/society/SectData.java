package com.hukhong.society;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Data attachment tông môn của người chơi. sect="" = chưa vào tông. invite = thư mời đang chờ.
 * totalCh = CH tích lũy (xét bậc); cycle/cycleEarned = trần CH theo Chu Kỳ.
 */
public record SectData(String sect, int totalCh, String quest, int progress, String invite, long cycle, int cycleEarned) {
    public static final SectData DEFAULT = new SectData("", 0, "", 0, "", 0, 0);

    public static final Codec<SectData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("sect", "").forGetter(SectData::sect),
            Codec.INT.optionalFieldOf("total_ch", 0).forGetter(SectData::totalCh),
            Codec.STRING.optionalFieldOf("quest", "").forGetter(SectData::quest),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(SectData::progress),
            Codec.STRING.optionalFieldOf("invite", "").forGetter(SectData::invite),
            Codec.LONG.optionalFieldOf("cycle", 0L).forGetter(SectData::cycle),
            Codec.INT.optionalFieldOf("cycle_earned", 0).forGetter(SectData::cycleEarned)
    ).apply(i, SectData::new));

    public boolean member() { return !sect.isEmpty(); }

    public SectData withSect(String s) { return new SectData(s, totalCh, "", 0, "", cycle, cycleEarned); }
    public SectData withQuest(String q, int p) { return new SectData(sect, totalCh, q, p, invite, cycle, cycleEarned); }
    public SectData withProgress(int p) { return withQuest(quest, p); }
    public SectData withInvite(String s) { return new SectData(sect, totalCh, quest, progress, s, cycle, cycleEarned); }

    /** Cộng CH (đã áp trần), trả bản mới; tự reset bộ đếm khi sang Chu Kỳ mới. */
    public SectData withEarned(long curCycle, int ch) {
        int earned = curCycle == cycle ? cycleEarned : 0;
        return new SectData(sect, totalCh + ch, quest, progress, invite, curCycle, earned + ch);
    }

    public int earnedIn(long curCycle) { return curCycle == cycle ? cycleEarned : 0; }
}
