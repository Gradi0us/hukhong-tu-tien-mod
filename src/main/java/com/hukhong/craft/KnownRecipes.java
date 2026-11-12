package com.hukhong.craft;

import com.mojang.serialization.Codec;

import java.util.List;
import java.util.Set;

/** Attachment: tập id công thức đã lĩnh hội (copyOnDeath). Bất biến: đổi bằng cách tạo bản mới. */
public record KnownRecipes(Set<String> ids) {
    public static final KnownRecipes EMPTY = new KnownRecipes(Set.of());

    public static final Codec<KnownRecipes> CODEC = Codec.STRING.listOf().xmap(
            l -> new KnownRecipes(Set.copyOf(l)),
            k -> List.copyOf(new java.util.TreeSet<>(k.ids())));
}
