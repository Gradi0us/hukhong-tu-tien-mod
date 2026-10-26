package com.hukhong.menu;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, HuKhong.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CoreAltarMenu>> CORE_ALTAR =
            MENUS.register("core_altar", () -> IMenuTypeExtension.create(CoreAltarMenu::new));
}
