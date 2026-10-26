package com.hukhong.client;

import com.hukhong.HuKhong;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Rung camera nhẹ khi đột phá. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ClientShake {
    private ClientShake() {}

    private static int total, left;
    private static float strength;

    public static void start(int ticks, float s) { total = left = ticks; strength = s; }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (left <= 0) return;
        float k = (left - (float) e.getPartialTick()) / total;
        if (k < 0) k = 0;
        double t = (total - left + e.getPartialTick());
        e.setPitch(e.getPitch() + (float) Math.sin(t * 2.1) * strength * k);
        e.setYaw(e.getYaw() + (float) Math.cos(t * 1.7) * strength * k);
    }

    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e) {
        if (left > 0) left--;
    }
}
