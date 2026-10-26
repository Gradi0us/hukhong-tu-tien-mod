package com.hukhong.client.onboarding;

import com.hukhong.network.OnboardingHintPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** Client: toast + dòng chat cho gợi ý onboarding. */
public final class OnboardingClient {
    private OnboardingClient() {}

    private static final String[] COMPASS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};

    public static void show(OnboardingHintPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Component body;
        if (p.step() == 4 && p.hasCity()) {
            double dx = p.x() - mc.player.getX(), dz = p.z() - mc.player.getZ();
            double ang = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = Bắc, 90 = Đông
            int idx = Math.floorMod((int) Math.round(ang / 45.0), 8);
            body = Component.translatable("hukhong.onboard.4", Component.translatable("hukhong.compass." + COMPASS[idx]),
                    (int) Math.sqrt(dx * dx + dz * dz), p.x(), p.z());
        } else {
            body = Component.translatable("hukhong.onboard." + p.step() + (p.step() == 4 ? ".nocity" : ""));
        }
        Component title = Component.translatable("hukhong.onboard.title", p.step(), 5);
        SystemToast.addOrUpdate(mc.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, body);
        mc.player.sendSystemMessage(Component.empty().append(title).append(": ").append(body));
    }
}
