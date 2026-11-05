package com.hukhong.phapkhi;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** Đăng ký 75 Pháp Khí (15 loại x 5 phẩm) + hai data component (số lần còn lại, chế độ Kỹ Năng). */
public final class PhapKhiRegistry {
    private PhapKhiRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);

    /** Số lần dùng còn lại; thiếu = đầy (maxUses). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> USES =
            COMPONENTS.registerComponentType("phapkhi_uses", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** true = chế độ Kỹ Năng (phù Công); thiếu = Toàn Lực. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SKILL_MODE =
            COMPONENTS.registerComponentType("phapkhi_skill_mode", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private static final Map<PhapKhiKind, DeferredItem<PhapKhiItem>[]> BY_KIND = new EnumMap<>(PhapKhiKind.class);

    @SuppressWarnings("unchecked")
    private static void registerAll() {
        for (PhapKhiKind k : PhapKhiKind.values()) {
            DeferredItem<PhapKhiItem>[] arr = new DeferredItem[PhapKhiTier.values().length];
            for (PhapKhiTier t : PhapKhiTier.values()) {
                arr[t.ordinal()] = ITEMS.register(id(k, t), () -> new PhapKhiItem(k, t, new Item.Properties().stacksTo(1)));
            }
            BY_KIND.put(k, arr);
        }
    }

    static { registerAll(); }

    public static String id(PhapKhiKind k, PhapKhiTier t) { return k.id + "_" + t.id; }

    public static PhapKhiItem item(PhapKhiKind k, PhapKhiTier t) { return BY_KIND.get(k)[t.ordinal()].get(); }

    public static ItemStack stack(PhapKhiKind k, PhapKhiTier t) { return new ItemStack(item(k, t)); }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() != CreativeModeTabs.COMBAT) return;
            for (PhapKhiKind k : PhapKhiKind.values())
                for (PhapKhiTier t : PhapKhiTier.values()) e.accept(item(k, t));
        });
    }
}
