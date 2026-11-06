package com.hukhong.story.nemesis;

import com.hukhong.HuKhong;
import com.hukhong.story.flow.FlowSteps;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /hknemesis c1|c4|c6|c9 (op): xem lại cảnh Dạ Khuyết. Không đổi tiến độ truyện nếu không ở đúng bước. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class NemesisCommand {
    private NemesisCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hknemesis").requires(s -> s.hasPermission(2))
                .then(Commands.argument("scene", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"c1", "c4", "c6", "c9"}, b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            FlowSteps.Step s = FlowSteps.get(StringArgumentType.getString(c, "scene") + "_nemesis");
                            if (s == null) { c.getSource().sendFailure(Component.literal("c1 | c4 | c6 | c9")); return 0; }
                            NemesisService.start(p, s);
                            return 1;
                        })));
    }
}
