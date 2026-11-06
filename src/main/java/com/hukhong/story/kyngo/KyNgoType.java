package com.hukhong.story.kyngo;

import java.util.List;

/** Bốn loại Kỳ Ngộ. Mỗi loại có danh sách lựa chọn, gửi qua lệnh /hkkyngo &lt;token&gt; &lt;lựa chọn&gt;. */
public enum KyNgoType {
    LAO_NHAN("hukhong.kyngo.intro.lao", "help", "rob", "leave"),
    DONG_PHU("hukhong.kyngo.intro.dong", "leave"),
    THI_THE("hukhong.kyngo.intro.thi", "search", "leave"),
    LINH_THU("hukhong.kyngo.intro.linh", "free", "leave");

    public final String introKey;
    private final List<String> choices;

    KyNgoType(String introKey, String... choices) {
        this.introKey = introKey;
        this.choices = List.of(choices);
    }

    public List<String> choices() { return choices; }
}
