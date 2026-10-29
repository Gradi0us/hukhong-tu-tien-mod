package com.hukhong.phaptu.linhtruong;

import com.hukhong.HuKhong;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Đăng ký riêng của Linh Trượng (không đụng ModItems). Gọi {@link #register} từ HuKhong. Dùng lại component BAN_MENH_TYPE (= nguyên tố), BAN_MENH_STAGE, BOUND_TO. */
public final class LinhTruongRegistry {
    private LinhTruongRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);

    public static final DeferredItem<LinhTruongItem> LINH_TRUONG = ITEMS.register("linh_truong", LinhTruongItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.COMBAT) e.accept(LINH_TRUONG.get());
        });
    }
}
