package com.hukhong;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(HuKhong.MODID)
public class HuKhong {
    public static final String MODID = "hukhong";

    public HuKhong(IEventBus modBus) {
        com.hukhong.spike.Spike.init(modBus);
    }
}
