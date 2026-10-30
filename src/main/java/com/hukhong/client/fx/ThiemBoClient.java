package com.hukhong.client.fx;

import com.hukhong.client.keybind.SkillKeys;
import com.hukhong.phaptu.ThiemBoMath;
import com.hukhong.phaptu.ThiemBoPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Quang E "Thiểm Bộ" phía client: đọc phím E vật lý (E cũng là túi đồ vanilla), gửi nhấn/nhả, ép chạy nước rút khi giữ. */
public final class ThiemBoClient {
    private ThiemBoClient() {}

    private static boolean prev;
    private static int held;

    /** Gọi mỗi tick từ SkillKeyHandler. active = thế bật + trang Pháp Tu Quang + không mở màn hình. */
    public static void tickKeys(Minecraft mc, boolean active, boolean tho) {
        boolean down = active && mc.screen == null && PhapBeamClient.physicalDown(mc, SkillKeys.SKILL[1]);
        if (down && !prev) { PacketDistributor.sendToServer(new ThiemBoPayload(ThiemBoPayload.PRESS)); held = 0; }
        if (!down && prev) PacketDistributor.sendToServer(new ThiemBoPayload(ThiemBoPayload.RELEASE));
        prev = down;
        if (down) {
            held++;
            if (ThiemBoMath.isHold(held) && mc.player != null && !tho) mc.player.setSprinting(true); // Thổ giữ E = xuyên tường, không chạy nước rút
        } else held = 0;
    }
}
