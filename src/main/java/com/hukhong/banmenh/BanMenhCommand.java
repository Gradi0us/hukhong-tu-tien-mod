package com.hukhong.banmenh;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /hukhong summon_sword, và /summon_sword (không cần OP vì root /hukhong yêu cầu OP). */
@EventBusSubscriber(modid = com.hukhong.HuKhong.MODID)
public final class BanMenhCommand {
    private BanMenhCommand() {}

    private static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("summon_sword").executes(c -> {
            BanMenhService.summon(c.getSource().getPlayerOrException());
            return 1;
        });
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hukhong").then(node()));
        e.getDispatcher().register(node());
    }
}
