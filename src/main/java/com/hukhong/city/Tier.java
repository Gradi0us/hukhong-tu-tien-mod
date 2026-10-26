package com.hukhong.city;

/** Cấp thành/tông: Nhân < Địa < Thiên (canon §7, S6 §3.2: giá trị Địa x1,5, Thiên x2,5). */
public enum Tier {
    NHAN(1.0), DIA(1.5), THIEN(2.5);

    public final double mult;

    Tier(double mult) { this.mult = mult; }

    public String key() { return "hukhong.tier." + name().toLowerCase(java.util.Locale.ROOT); }
}
