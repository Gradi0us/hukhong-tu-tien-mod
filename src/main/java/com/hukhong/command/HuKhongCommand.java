package com.hukhong.command;

import com.hukhong.age.AgeData;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /hukhong dump | setlevel <n> | addexp <n> | age  (permission 2). */
public final class HuKhongCommand {
    private HuKhongCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hukhong")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("dump").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    CultivationData d = CultivationService.get(p);
                    msg(c.getSource(), d.toString() + " expToNext=" + CultivationService.expToNext(d.level()));
                    return 1;
                }))
                .then(Commands.literal("setlevel").then(Commands.argument("n", IntegerArgumentType.integer(1, 100))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CultivationService.setLevel(p, IntegerArgumentType.getInteger(c, "n"));
                            msg(c.getSource(), "level=" + CultivationService.get(p).level());
                            return 1;
                        })))
                .then(Commands.literal("addexp").then(Commands.argument("n", LongArgumentType.longArg(1))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CultivationService.addExp(p, LongArgumentType.getLong(c, "n"));
                            CultivationData d = CultivationService.get(p);
                            msg(c.getSource(), "level=" + d.level() + " exp=" + d.exp());
                            return 1;
                        })))
                .then(Commands.literal("reroll").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    com.hukhong.charcreate.CharCreateService.reroll(p);
                    msg(c.getSource(), "reroll: " + CultivationService.get(p).roll());
                    return 1;
                }))
                .then(Commands.literal("age").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    AgeData a = p.getData(ModAttachments.AGE);
                    msg(c.getSource(), "age=" + a.ageYears() + " onlineTicks=" + a.onlineTicks()
                            + " months=" + a.onlineTicks() / AgeData.TICKS_PER_MONTH);
                    return 1;
                })));
    }

    private static void msg(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
    }
}
