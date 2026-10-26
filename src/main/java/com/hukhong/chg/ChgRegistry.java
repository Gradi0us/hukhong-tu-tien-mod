package com.hukhong.chg;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Đăng ký Bước 20: 4 entity Chấp Hành Giả (chg_1..4) + 4 vật liệu độc nhất (chg_core_1..4). */
public final class ChgRegistry {
    private ChgRegistry() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);

    private static final float[][] SIZE = {{1.9f, 3.8f}, {1.2f, 3.6f}, {1.1f, 3.5f}, {1.4f, 3.7f}};
    public static final List<DeferredHolder<EntityType<?>, EntityType<ChapHanhGia>>> BOSSES = new ArrayList<>();
    public static final List<DeferredItem<Item>> CORES = new ArrayList<>();

    static {
        for (int i = 1; i <= ChgKit.COUNT; i++) {
            String id = "chg_" + i;
            float[] sz = SIZE[i - 1];
            BOSSES.add(TYPES.register(id, () -> EntityType.Builder.of(ChapHanhGia::new, MobCategory.MONSTER)
                    .sized(sz[0], sz[1]).fireImmune().clientTrackingRange(12).build(id)));
            CORES.add(ITEMS.register("chg_core_" + i, () -> new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16))));
        }
    }

    public static EntityType<ChapHanhGia> type(int idx) { return BOSSES.get(idx - 1).get(); }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            for (int i = 1; i <= ChgKit.COUNT; i++) e.put(type(i), ChapHanhGia.attrs(i).build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) CORES.forEach(c -> e.accept(c.get()));
        });
    }
}
