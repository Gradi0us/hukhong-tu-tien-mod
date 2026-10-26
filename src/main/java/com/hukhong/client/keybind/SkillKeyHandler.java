package com.hukhong.client.keybind;

import com.hukhong.HuKhong;
import com.hukhong.network.CastSkillPayload;
import com.hukhong.network.ToggleStancePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Xử lý phím ở đầu tick (trước handleKeybinds của vanilla).
 * Thế bật: nuốt click Q (vứt đồ) và E (túi đồ) vanilla, Q/E/R/T gửi CastSkillPayload.
 * Thế tắt: Q/E trả về vanilla (phím kỹ năng bị bỏ qua).
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class SkillKeyHandler {
    private SkillKeyHandler() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            while (SkillKeys.PROFILE.consumeClick()) { }
            while (SkillKeys.TOGGLE.consumeClick()) { }
            while (SkillKeys.PAGE.consumeClick()) { }
            for (var k : SkillKeys.SKILL) while (k.consumeClick()) { }
            return;
        }
        ClientSkillState.tick();
        boolean noScreen = mc.screen == null;
        while (SkillKeys.PROFILE.consumeClick()) {
            if (noScreen) mc.setScreen(new com.hukhong.client.screen.profile.ProfileScreen());
        }
        while (SkillKeys.TOGGLE.consumeClick()) {
            if (noScreen) PacketDistributor.sendToServer(new ToggleStancePayload());
        }
        while (SkillKeys.PAGE.consumeClick()) {
            if (noScreen && ClientSkillState.stance()) PacketDistributor.sendToServer(new com.hukhong.network.CyclePagePayload());
        }
        boolean on = ClientSkillState.stance() && noScreen;
        if (on) {
            while (mc.options.keyDrop.consumeClick()) { }
            while (mc.options.keyInventory.consumeClick()) { }
            for (var vk : mc.options.keyMappings) { // T = chat vanilla...: nuốt phím vanilla trùng phím kỹ năng
                boolean ours = vk == SkillKeys.PROFILE || vk == SkillKeys.TOGGLE || vk == SkillKeys.PAGE;
                for (var sk : SkillKeys.SKILL) if (vk == sk) ours = true;
                if (ours) continue;
                for (var sk : SkillKeys.SKILL) if (vk.same(sk)) { while (vk.consumeClick()) { } break; }
            }
        }
        for (int i = 0; i < 4; i++) {
            while (SkillKeys.SKILL[i].consumeClick()) {
                if (on) PacketDistributor.sendToServer(new CastSkillPayload(i));
            }
        }
    }
}
