package com.hukhong.client.render.artifact;

import com.hukhong.HuKhong;
import net.minecraft.resources.ResourceLocation;

/** Texture layer pháp khí theo giai đoạn 1..6 (sinh bằng tools/gen_path_stages.py). */
public final class ArtifactTextures {
    private ArtifactTextures() {}

    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/entity/artifact/" + name + ".png");
    }

    private static ResourceLocation[] series(String prefix, String suffix) {
        ResourceLocation[] a = new ResourceLocation[7];
        for (int s = 1; s <= 6; s++) a[s] = rl(prefix + "_s" + s + suffix);
        a[0] = a[1];
        return a;
    }

    public static final ResourceLocation WINGS_BASIC =
            ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/entity/wings.png");
    private static final ResourceLocation[] WINGS = series("wings", "");
    private static final ResourceLocation[] WINGS_GLOW = series("wings", "_glow");
    private static final ResourceLocation[] THE_SKIN = series("the_skin", "");
    private static final ResourceLocation[] THE_GLOW = series("the_skin", "_glow");
    private static final ResourceLocation[] HORN = series("ma_horn", "");
    private static final ResourceLocation[] CLOUD = series("ma_cloud", "");

    private static int c(int s) { return Math.max(0, Math.min(6, s)); }

    public static ResourceLocation wings(int stage) { return WINGS[c(stage)]; }
    public static ResourceLocation wingsGlow(int stage) { return WINGS_GLOW[c(stage)]; }
    public static ResourceLocation theSkin(int stage) { return THE_SKIN[c(stage)]; }
    public static ResourceLocation theGlow(int stage) { return THE_GLOW[c(stage)]; }
    public static ResourceLocation horn(int stage) { return HORN[c(stage)]; }
    public static ResourceLocation cloud(int stage) { return CLOUD[c(stage)]; }
}
