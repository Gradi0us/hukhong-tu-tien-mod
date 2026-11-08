package com.hukhong.storage;

import com.hukhong.HuKhong;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/** Nhẫn Trữ Vật: kho riêng mỗi người chơi (27 ô), giữ khi chết. */
public final class TruVatRegistry {
    private TruVatRegistry() {}

    public static final int SIZE = 27;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final DeferredItem<NhanTruVatItem> RING = ITEMS.register("nhan_tru_vat",
            () -> new NhanTruVatItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** Nội dung kho: luôn đúng SIZE ô; copyOnDeath nên không bao giờ rơi. */
    public static final Supplier<AttachmentType<NonNullList<ItemStack>>> STORE = ATTACHMENTS.register("tru_vat_kho",
            () -> AttachmentType.builder(() -> NonNullList.withSize(SIZE, ItemStack.EMPTY))
                    .serialize(ItemStack.OPTIONAL_CODEC.listOf().xmap(TruVatRegistry::fit, l -> (List<ItemStack>) l))
                    .copyOnDeath().build());

    /** Số nhẫn giữ lại khi chết, trả lại lúc hồi sinh. */
    public static final Supplier<AttachmentType<Integer>> KEPT_RINGS = ATTACHMENTS.register("tru_vat_kept",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.INT).copyOnDeath().build());

    static NonNullList<ItemStack> fit(List<ItemStack> l) {
        NonNullList<ItemStack> out = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(SIZE, l.size()); i++) out.set(i, l.get(i));
        return out;
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(RING.get());
        });
    }
}
