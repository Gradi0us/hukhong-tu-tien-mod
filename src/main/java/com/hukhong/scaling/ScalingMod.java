package com.hukhong.scaling;

import com.hukhong.HuKhong;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Điểm vào phụ của hệ thống Kiếp Số (không sửa HuKhong.java): đăng ký config, vật phẩm, payload, sự kiện. */
@Mod(HuKhong.MODID)
public final class ScalingMod {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredItem<Item> TAM_TINH = ITEMS.register("tam_tinh",
            () -> new CrystalItem(true, new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> LINH_TINH = ITEMS.register("linh_tinh",
            () -> new CrystalItem(false, new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE)));

    public ScalingMod(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ScalingConfig.SPEC, "hukhong-scaling.toml");
        ITEMS.register(bus);
        bus.addListener((RegisterPayloadHandlersEvent e) ->
                e.registrar("1").playToClient(KiepPayload.TYPE, KiepPayload.CODEC, KiepPayload::handle));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                e.accept(TAM_TINH.get());
                e.accept(LINH_TINH.get());
            }
        });
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> e.register(ScalingGameTests.class));
        NeoForge.EVENT_BUS.register(ScalingEvents.class);
    }
}
