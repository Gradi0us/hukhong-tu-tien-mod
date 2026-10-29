package com.hukhong.client.boss;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Phím bỏ qua cutscene boss (mặc định Space, đổi được trong Điều khiển). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class BossKeys {
    private BossKeys() {}

    public static final KeyMapping SKIP = new KeyMapping("key.hukhong.cutscene_skip", InputConstants.KEY_SPACE, "key.categories.hukhong");

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent e) { e.register(SKIP); }
}
