package com.hukhong.storage;

import com.hukhong.HuKhong;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Nhẫn Trữ Vật không rơi khi chết: gỡ khỏi đồ rơi, trả lại khi hồi sinh. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TruVatEvents {
    private TruVatEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        int[] n = {0};
        e.getDrops().removeIf(ie -> {
            if (!ie.getItem().is(TruVatRegistry.RING.get())) return false;
            n[0] += ie.getItem().getCount();
            return true;
        });
        if (n[0] > 0) sp.setData(TruVatRegistry.KEPT_RINGS.get(), sp.getData(TruVatRegistry.KEPT_RINGS.get()) + n[0]);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        int n = sp.getData(TruVatRegistry.KEPT_RINGS.get());
        if (n <= 0) return;
        sp.setData(TruVatRegistry.KEPT_RINGS.get(), 0);
        for (int i = 0; i < n; i++) {
            ItemStack r = new ItemStack(TruVatRegistry.RING.get());
            if (!sp.getInventory().add(r)) sp.drop(r, false);
        }
    }
}
