package com.hukhong.core;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Tư chất D..SSS. */
public enum Aptitude implements StringRepresentable {
    D, C, B, A, S, SS, SSS;

    public static final Codec<Aptitude> CODEC = StringRepresentable.fromEnum(Aptitude::values);

    @Override
    public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
