package com.hukhong.profession;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** Nhanh /hukhong nghe luyen_dan|phu_van|doan_tao (gan vao HuKhongCommand, quyen 2). */
public final class ProfessionCommand {
    private ProfessionCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("nghe")
                .then(Commands.literal("luyen_dan").executes(c -> run(c.getSource(), Profession.LUYEN_DAN)))
                .then(Commands.literal("phu_van").executes(c -> run(c.getSource(), Profession.PHU_VAN)))
                .then(Commands.literal("doan_tao").executes(c -> run(c.getSource(), Profession.DOAN_TAO)));
    }

    private static int run(CommandSourceStack src, Profession prof) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        return ProfessionService.choose(p, prof) ? 1 : 0;
    }
}
