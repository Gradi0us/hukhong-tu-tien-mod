package com.hukhong.client.objective;

import com.hukhong.HuKhong;
import com.hukhong.story.objective.ObjectiveData;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Trạng thái client của mục tiêu chính + phím L bật/tắt hiển thị. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ClientObjective {
    private ClientObjective() {}

    public static final KeyMapping TOGGLE = new KeyMapping("key.hukhong.objective_toggle", InputConstants.KEY_L, "key.categories.hukhong");

    private static ObjectiveData data = ObjectiveData.EMPTY;
    private static boolean visible = true;

    public static void set(ObjectiveData d) { data = d; }
    public static ObjectiveData data() { return data; }
    public static boolean visible() { return visible && !data.isEmpty(); }

    /** Đích trong cùng chiều với người chơi (hoặc dim không chỉ định), null nếu không có / đang ẩn. */
    public static BlockPos sameDimTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (!visible() || mc.level == null || data.target().isEmpty()) return null;
        if (data.dim().isPresent() && data.dim().get() != mc.level.dimension()) return null;
        return data.target().get();
    }

    public static void toast(Component msg) {
        Minecraft mc = Minecraft.getInstance();
        SystemToast.add(mc.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, msg, null);
    }

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent e) { e.register(TOGGLE); }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE.consumeClick()) {
            if (mc.player != null && mc.screen == null) visible = !visible;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { data = ObjectiveData.EMPTY; }
}
