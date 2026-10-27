package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry bí tịch: 1 item `skill_scroll`, kỹ năng nằm ở data component `skill_id`, model chọn theo phẩm (custom_model_data). */
public final class SkillBookRegistry {
    private SkillBookRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SKILL_ID =
            COMPONENTS.registerComponentType("skill_id",
                    b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredItem<SkillScrollItem> SCROLL = ITEMS.register("skill_scroll",
            () -> new SkillScrollItem(new Item.Properties().stacksTo(16)));

    public static ItemStack stack(BookSkill s) {
        ItemStack st = new ItemStack(SCROLL.get());
        st.set(SKILL_ID.get(), s.id);
        st.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(s.grade.ordinal() + 1));
        st.set(DataComponents.RARITY, switch (s.grade) {
            case PHAM -> Rarity.COMMON;
            case HUYEN -> Rarity.UNCOMMON;
            case DIA -> Rarity.RARE;
            case THIEN -> Rarity.EPIC;
        });
        return st;
    }

    /** Kỹ năng ghi trên bí tịch (null nếu không phải hoặc id lạ). */
    public static BookSkill skillOf(ItemStack st) {
        if (st.isEmpty() || !st.is(SCROLL.get())) return null;
        return BookSkill.byId(st.get(SKILL_ID.get()));
    }

    /** Gọi từ ModItems.register. */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) for (BookSkill s : BookSkill.values()) e.accept(stack(s));
        });
    }
}
