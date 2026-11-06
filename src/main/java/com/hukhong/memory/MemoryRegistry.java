package com.hukhong.memory;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Đăng ký Ký Ức Hư Không: item Mảnh Ký Ức, data component id (1..8), attachment hồ sơ người chơi. */
public final class MemoryRegistry {
    private MemoryRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    /** Id ký ức (1..8) trên Mảnh Ký Ức. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KY_UC_ID =
            COMPONENTS.registerComponentType("ky_uc_id", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<MemoryItem> SHARD = ITEMS.register("manh_ky_uc",
            () -> new MemoryItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));

    public static final Supplier<AttachmentType<MemoryData>> DATA = ATTACHMENTS.register("memory",
            () -> AttachmentType.builder(() -> MemoryData.EMPTY).serialize(MemoryData.CODEC).copyOnDeath().build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
    }

    /** Mảnh Ký Ức mang id cho trước. */
    public static ItemStack shard(int id) {
        ItemStack st = new ItemStack(SHARD.get());
        st.set(KY_UC_ID.get(), id);
        return st;
    }

    /** Id ký ức trên stack; 0 nếu không phải Mảnh Ký Ức hoặc thiếu id. */
    public static int idOf(ItemStack st) {
        if (!st.is(SHARD.get())) return 0;
        Integer v = st.get(KY_UC_ID.get());
        return v == null ? 0 : v;
    }
}
