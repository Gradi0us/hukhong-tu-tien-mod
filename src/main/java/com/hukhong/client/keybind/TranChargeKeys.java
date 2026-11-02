package com.hukhong.client.keybind;

import com.hukhong.client.fx.PhapBeamClient;
import com.hukhong.network.TranChargePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sát Trận (Q trong thế Sát Thương): giữ Q để tụ lực. Gửi nhấn/nhả cho server; server giữ trạng thái và thả cột sáng. */
public final class TranChargeKeys {
    private TranChargeKeys() {}

    private static boolean prev;

    /** Gọi mỗi tick từ SkillKeyHandler. active = thế bật, đang ở Sát Trận Thế Sát Thương, không mở màn hình. */
    public static void tick(Minecraft mc, boolean active) {
        boolean down = active && mc.screen == null && PhapBeamClient.physicalDown(mc, SkillKeys.SKILL[0]);
        if (down && !prev) PacketDistributor.sendToServer(new TranChargePayload(TranChargePayload.PRESS));
        if (!down && prev) PacketDistributor.sendToServer(new TranChargePayload(TranChargePayload.RELEASE));
        prev = down;
    }
}
