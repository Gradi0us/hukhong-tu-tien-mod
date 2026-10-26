package com.hukhong.entity;

import com.hukhong.HuKhong;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.GeckoLibCache;

/** Chọn animation "skill" nếu file animation có, không thì fallback "attack". */
public final class HuAnim {
    private HuAnim() {}

    public static RawAnimation skillOrAttack(String id) {
        String skill = "animation." + id + ".skill";
        boolean has = false;
        try {
            var baked = GeckoLibCache.getBakedAnimations().get(
                    ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "animations/entity/" + id + ".animation.json"));
            has = baked != null && baked.getAnimation("animation." + id + ".skill") != null;
        } catch (Throwable ignored) {}
        return RawAnimation.begin().then(has ? skill : "animation." + id + ".attack", Animation.LoopType.PLAY_ONCE);
    }
}
