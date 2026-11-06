package com.hukhong.story.kyngo;

import com.hukhong.HuKhong;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /hkkyngo &lt;token&gt; &lt;lựa chọn&gt; — gọi bởi link bấm trong chat của Kỳ Ngộ. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class KyNgoCommand {
    private KyNgoCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hkkyngo")
                .then(Commands.argument("token", StringArgumentType.word())
                        .then(Commands.argument("choice", StringArgumentType.word())
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    KyNgoService.Result r = KyNgoService.resolve(p,
                                            StringArgumentType.getString(c, "token"),
                                            StringArgumentType.getString(c, "choice"));
                                    if (r != KyNgoService.Result.OK) {
                                        c.getSource().sendFailure(Component.translatable("hukhong.kyngo.err." + r.name().toLowerCase(java.util.Locale.ROOT)));
                                        return 0;
                                    }
                                    return 1;
                                }))));
    }
}
