package com.hukhong.auction;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Lang helper Dot 7c: key + fallback tieng Viet + ban tieng Anh (chi de script docs/research/lang-a7.txt trich xuat:
 * tools/extract_lang_a7.py quet cac loi goi Tx.t(...)). Khong dung dau nhay kep ben trong chuoi.
 */
public final class Tx {
    private Tx() {}

    public static MutableComponent t(String key, String vi, String en, Object... args) {
        return Component.translatableWithFallback(key, vi, args);
    }
}
