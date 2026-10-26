package com.hukhong.client.story;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Phím J mở Nhật ký truyện. */
public final class StoryKeys {
    private StoryKeys() {}

    public static final KeyMapping JOURNAL =
            new KeyMapping("key.hukhong.journal", InputConstants.KEY_J, "key.categories.hukhong");

    @EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
    public static final class Reg {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent e) { e.register(JOURNAL); }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Pre e) {
            Minecraft mc = Minecraft.getInstance();
            while (JOURNAL.consumeClick()) {
                if (mc.player != null && mc.screen == null) mc.setScreen(new JournalScreen());
            }
        }
    }
}
