package com.hukhong.story.freequest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** active = cấp Bình Cảnh đang làm quest (0 = không); kills = Hư Hóa đã hạ; rewarded = các cấp đã nhận đan; hinted = đã gợi ý lần đầu. */
public record FreeQuestData(int active, int kills, List<Integer> rewarded, boolean hinted) {
    public static final FreeQuestData EMPTY = new FreeQuestData(0, 0, List.of(), false);
    public static final Codec<FreeQuestData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("active", 0).forGetter(FreeQuestData::active),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(FreeQuestData::kills),
            Codec.INT.listOf().optionalFieldOf("rewarded", List.of()).forGetter(FreeQuestData::rewarded),
            Codec.BOOL.optionalFieldOf("hinted", false).forGetter(FreeQuestData::hinted)
    ).apply(i, FreeQuestData::new));

    public boolean wasRewarded(int level) { return rewarded.contains(level); }
    public FreeQuestData withActive(int a) { return new FreeQuestData(a, a == active ? kills : 0, rewarded, hinted); }
    public FreeQuestData withKills(int k) { return new FreeQuestData(active, k, rewarded, hinted); }
    public FreeQuestData withHinted() { return new FreeQuestData(active, kills, rewarded, true); }
    public FreeQuestData withRewarded(int level) {
        List<Integer> l = new ArrayList<>(rewarded);
        if (!l.contains(level)) l.add(level);
        return new FreeQuestData(active, kills, List.copyOf(l), hinted);
    }
}
