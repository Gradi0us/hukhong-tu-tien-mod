package com.hukhong.client;

import com.hukhong.HuKhong;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** The Gioi Tam Thuc: man hinh toi dan / sang lai va suong gan (4-6 o) thay Blindness. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class TamThucClient {
    private TamThucClient() {}

    public static final ResourceKey<Level> DIM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tam_thuc"));

    private static int mode = -1, total = 1, t;

    public static void fade(int m, int ticks) {
        boolean off = ClientConfig.screenEffectsOff();
        mode = m; total = Math.max(1, off ? Math.min(ticks, 15) : ticks); t = 0;
    }

    private static boolean inside() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension() == DIM;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (mode < 0) return;
        if (t < total) t++;
        if (mode == 1 && t >= total) mode = -1;
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre e) {
        if (mode < 0) return;
        float k = Math.min(1f, (t + e.getPartialTick().getGameTimeDeltaPartialTick(false)) / total);
        float a = mode == 0 ? k : 1f - k;
        if (ClientConfig.screenEffectsOff()) a *= 0.6f;
        int al = Math.round(Math.max(0f, Math.min(1f, a)) * 255);
        if (al <= 0) return;
        var g = e.getGuiGraphics();
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), al << 24);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (!inside()) return;
        float far = ClientConfig.screenEffectsOff() ? 8f : 5f;
        e.setNearPlaneDistance(0f);
        e.setFarPlaneDistance(far);
        e.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (!inside()) return;
        e.setRed(0f); e.setGreen(0f); e.setBlue(0f);
    }
}
