package com.hukhong.honcot;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** Đăng ký item Hồn Cốt (12), data component phẩm/roll, attachment 6 slot. Gọi từ ModItems.register. */
public final class HonCotRegistry {
    private HonCotRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    /** Phẩm 0 Tử / 1 Hắc / 2 Hồng. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BONE_TIER =
            COMPONENTS.registerComponentType("bone_tier", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Chất lượng roll 0..100. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BONE_ROLL =
            COMPONENTS.registerComponentType("bone_roll", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final Supplier<AttachmentType<BoneSlots>> BONE_SLOTS = ATTACHMENTS.register("bone_slots",
            () -> AttachmentType.builder(() -> BoneSlots.EMPTY).serialize(BoneSlots.CODEC).copyOnDeath().build());

    public static final Map<Bone, DeferredItem<HonCotItem>> BONE_ITEMS = new EnumMap<>(Bone.class);
    static {
        for (Bone b : Bone.values()) BONE_ITEMS.put(b, ITEMS.register(b.itemId(), () -> new HonCotItem(b)));
    }

    public static ItemStack stack(Bone b, int tier, int roll) {
        ItemStack s = new ItemStack(BONE_ITEMS.get(b).get());
        s.set(BONE_TIER.get(), HonCotMath.clampTier(tier));
        s.set(BONE_ROLL.get(), Math.max(0, Math.min(100, roll)));
        return s;
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                for (Bone b : Bone.values()) e.accept(stack(b, HonCotMath.TIER_HAC, 50));
            }
        });
    }
}
