package com.hukhong.pvp;

import com.hukhong.HuKhong;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /duel &lt;nguoi&gt; &lt;tien cuoc Ha&gt; [balance]  |  /duel accept | decline | bet &lt;a|b&gt; &lt;Ha&gt; | join | status
 * Cung gan duoi /hukhong duel (goc /hukhong yeu cau quyen 2, nen nguoi choi thuong dung /duel).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DuelCommand {
    private DuelCommand() {}

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String name) {
        return Commands.literal(name)
                .then(Commands.literal("accept").executes(c -> result(c.getSource(), DuelManager.accept(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("decline").executes(c -> {
                    boolean ok = DuelManager.decline(c.getSource().getPlayerOrException());
                    if (!ok) c.getSource().sendFailure(Component.translatable("hukhong.pvp.err.no_challenge"));
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("join").executes(c -> result(c.getSource(), Bracket.join(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("status").executes(c -> {
                    boolean open = DuelManager.open(c.getSource().getServer().overworld());
                    c.getSource().sendSuccess(() -> Component.translatable(open ? "hukhong.pvp.status.open" : "hukhong.pvp.err.closed"), false);
                    return 1;
                }))
                .then(Commands.literal("bet").then(Commands.argument("side", StringArgumentType.word()).then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(c -> {
                            String s = StringArgumentType.getString(c, "side").toLowerCase(java.util.Locale.ROOT);
                            if (!s.equals("a") && !s.equals("b")) { c.getSource().sendFailure(Component.translatable("hukhong.pvp.err.bet_side")); return 0; }
                            return result(c.getSource(), DuelManager.bet(c.getSource().getPlayerOrException(), s.equals("a") ? 1 : -1, LongArgumentType.getLong(c, "amount")));
                        }))))
                .then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("stake", LongArgumentType.longArg(1))
                        .executes(c -> challenge(c.getSource(), EntityArgument.getPlayer(c, "target"), LongArgumentType.getLong(c, "stake"), true))
                        .then(Commands.argument("balance", BoolArgumentType.bool())
                                .executes(c -> challenge(c.getSource(), EntityArgument.getPlayer(c, "target"), LongArgumentType.getLong(c, "stake"), BoolArgumentType.getBool(c, "balance"))))));
    }

    private static int challenge(CommandSourceStack src, ServerPlayer target, long stake, boolean balance) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int r = result(src, DuelManager.challenge(src.getPlayerOrException(), target, stake, balance));
        if (r == 1) src.sendSuccess(() -> Component.translatable("hukhong.pvp.challenge_sent", target.getDisplayName()), false);
        return r;
    }

    private static int result(CommandSourceStack src, String err) {
        if (err == null) return 1;
        src.sendFailure(Component.translatable(err));
        return 0;
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(tree("duel"));
        e.getDispatcher().register(Commands.literal("hukhong").requires(s -> s.hasPermission(2)).then(tree("duel")));
    }
}
