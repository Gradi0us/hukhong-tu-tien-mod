package com.hukhong.currency;

import com.hukhong.HuKhong;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Ba loai Tu Tinh. Gan bus qua ModItems.register. */
public final class CurrencyRegistry {
    private CurrencyRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);

    public static final DeferredItem<Item> HA = ITEMS.register("ha_pham_tu_tinh", () -> new Item(new Item.Properties().stacksTo(99)));
    public static final DeferredItem<Item> TRUNG = ITEMS.register("trung_pham_tu_tinh",
            () -> new Item(new Item.Properties().stacksTo(99).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> THUONG = ITEMS.register("thuong_pham_tu_tinh",
            () -> new Item(new Item.Properties().stacksTo(99).rarity(Rarity.RARE)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) { e.accept(HA.get()); e.accept(TRUNG.get()); e.accept(THUONG.get()); }
        });
    }
}
