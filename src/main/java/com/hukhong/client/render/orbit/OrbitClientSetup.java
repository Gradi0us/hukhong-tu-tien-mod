package com.hukhong.client.render.orbit;

import com.hukhong.HuKhong;
import com.hukhong.banmenh.BanMenhKiemItem;
import com.hukhong.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Đăng ký property model "hukhong:stage" (từ data component giai đoạn) để model Bản Mệnh Kiếm đổi theo giai đoạn. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class OrbitClientSetup {
    private OrbitClientSetup() {}

    public static final ResourceLocation STAGE_PROP = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "stage");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> ItemProperties.register(ModItems.BAN_MENH_KIEM.get(), STAGE_PROP,
                (stack, level, entity, seed) -> BanMenhKiemItem.stageOf(stack)));
    }
}
