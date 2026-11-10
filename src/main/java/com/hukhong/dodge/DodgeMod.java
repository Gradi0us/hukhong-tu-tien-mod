package com.hukhong.dodge;

import com.hukhong.HuKhong;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Điểm vào phụ Lách Né / Thể Lực (attachment ở ModAttachments.DODGE, buff ở ModEffects.THAN_PHAP). */
@Mod(HuKhong.MODID)
public final class DodgeMod {
    public DodgeMod(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, DodgeConfig.SPEC, "hukhong-dodge.toml");
        bus.addListener((RegisterPayloadHandlersEvent e) -> DodgeNet.register(e.registrar("1")));
        NeoForge.EVENT_BUS.register(DodgeService.class);
    }
}
