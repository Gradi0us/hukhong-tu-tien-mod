package com.hukhong.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Hiển thị cấp boss ở tên + thanh máu: "Hư Vương [Lv 30]". Thuần chuỗi, chạy được cả server lẫn client. */
public final class BossLevel {
    private BossLevel() {}

    public static String suffix(int level) { return " [Lv " + level + "]"; }

    public static MutableComponent tag(Component base, int level) {
        return base.copy().append(Component.literal(suffix(level)).withStyle(ChatFormatting.GOLD));
    }
}
