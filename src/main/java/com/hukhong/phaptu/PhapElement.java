package com.hukhong.phaptu;

import com.mojang.serialization.Codec;

import java.util.Locale;
import java.util.Random;

/** Sáu nguyên tố của Pháp Tu. Quang, Lôi, Hỏa, Thủy, Thổ, Kim (thứ tự = id ổn định, chỉ thêm cuối). */
public enum PhapElement {
    QUANG(0xFFE066), LOI(0x9CC4FF), HOA(0xFF6A2A), THUY(0x3FA9FF), THO(0xB08A4F), KIM(0xC9D3DE);

    public static final Codec<PhapElement> CODEC = Codec.STRING.xmap(PhapElement::byId, PhapElement::id);

    public final int rgb;

    PhapElement(int rgb) { this.rgb = rgb; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public int argb() { return 0xFF000000 | rgb; }

    public String langKey() { return "hukhong.phap.element." + id(); }

    public static PhapElement byId(String id) {
        if (id == null) return null;
        for (PhapElement e : values()) if (e.id().equals(id)) return e;
        return null;
    }

    /**
     * Nguyên tố roll lúc tạo nhân vật: xác định theo seed của CharRoll (server và client tính giống nhau, không cần gói tin).
     * Lần reroll (duy nhất 1 lần) luôn ra nguyên tố KHÁC lần đầu.
     */
    public static PhapElement roll(long seed, boolean rerolled) {
        Random r = new Random(seed ^ 0x5DEECE66DL ^ 0x70AF1AL);
        int first = r.nextInt(values().length);
        if (!rerolled) return values()[first];
        Random r2 = new Random(seed ^ 0x9E3779B97F4A7C15L);
        return values()[(first + 1 + r2.nextInt(values().length - 1)) % values().length];
    }
}
