package com.hukhong.client.keybind;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Phím kỹ năng Q/E/R/T + G bật/tắt Thế Tu Luyện (mod bus). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class SkillKeys {
    private SkillKeys() {}

    private static final String CAT = "key.categories.hukhong";
    public static final KeyMapping[] SKILL = {
            new KeyMapping("key.hukhong.skill_1", InputConstants.KEY_Q, CAT),
            new KeyMapping("key.hukhong.skill_2", InputConstants.KEY_E, CAT),
            new KeyMapping("key.hukhong.skill_3", InputConstants.KEY_R, CAT),
            new KeyMapping("key.hukhong.skill_ultimate", InputConstants.KEY_T, CAT)
    };
    public static final KeyMapping PROFILE = new KeyMapping("key.hukhong.profile", InputConstants.KEY_K, CAT);
    public static final KeyMapping TOGGLE =
            new KeyMapping("key.hukhong.toggle_cultivation", InputConstants.KEY_G, CAT);

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent e) {
        for (KeyMapping k : SKILL) e.register(k);
        e.register(TOGGLE);
        e.register(PROFILE);
    }
}
