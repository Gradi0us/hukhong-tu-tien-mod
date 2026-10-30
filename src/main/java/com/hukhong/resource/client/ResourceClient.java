package com.hukhong.resource.client;

import com.hukhong.HuKhong;
import com.hukhong.resource.ResourceRegistry;
import com.hukhong.world.season.Season;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Hook mau la theo mua: tuoi 0-1 cua linh thao (model tinted) doi mau theo {@link Season}. Client chi doc game time cua level. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ResourceClient {
    private ResourceClient() {}

    /** Mau la hien tai (client). */
    public static int foliage() {
        var lv = Minecraft.getInstance().level;
        return Season.tint(0xB4D8A0, lv == null ? Season.XUAN : Season.current(lv));
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Block e) {
        Block[] bs = ResourceRegistry.HERB_BLOCKS.values().stream().map(d -> (Block) d.get()).toArray(Block[]::new);
        e.register((state, level, pos, idx) -> foliage(), bs);
    }
}
