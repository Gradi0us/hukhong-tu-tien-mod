package com.hukhong;

import com.hukhong.age.AgeService;
import com.hukhong.command.HuKhongCommand;
import com.hukhong.core.ModAttachments;
import com.hukhong.network.SyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(HuKhong.MODID)
public class HuKhong {
    public static final String MODID = "hukhong";

    public HuKhong(IEventBus modBus) {
        ModAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(SyncPayload::register);
        NeoForge.EVENT_BUS.register(AgeService.class);
        NeoForge.EVENT_BUS.register(SyncPayload.class);
        NeoForge.EVENT_BUS.register(HuKhongCommand.class);
    }
}
