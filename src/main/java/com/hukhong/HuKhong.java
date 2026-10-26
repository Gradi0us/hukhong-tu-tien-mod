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
        com.hukhong.onboarding.OnboardingAttachments.ATTACHMENTS.register(modBus);
        com.hukhong.item.ModItems.register(modBus);
        com.hukhong.entity.ModEntities.register(modBus);
        com.hukhong.vfx.ModParticles.register(modBus);
        com.hukhong.story.StoryRegistry.register(modBus);
        com.hukhong.chg.ChgRegistry.register(modBus);
        com.hukhong.endgame.EndgameRegistry.register(modBus);
        modBus.addListener(SyncPayload::register);
        modBus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> e.register(com.hukhong.gametest.HuKhongGameTests.class));
        NeoForge.EVENT_BUS.register(com.hukhong.item.PillDrops.class);
        NeoForge.EVENT_BUS.register(AgeService.class);
        NeoForge.EVENT_BUS.register(SyncPayload.class);
        NeoForge.EVENT_BUS.register(HuKhongCommand.class);
    }
}
