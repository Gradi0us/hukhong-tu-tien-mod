package com.hukhong.craft;

import java.util.Set;

/** Bản sao sổ công thức phía client (cập nhật bởi RecipeBookNet.Sync). */
public final class ClientRecipeBook {
    private ClientRecipeBook() {}

    private static volatile Set<String> ids = Set.of();

    public static void set(Set<String> s) { ids = Set.copyOf(s); }

    public static boolean knows(String id) { return ids.contains(id); }
}
