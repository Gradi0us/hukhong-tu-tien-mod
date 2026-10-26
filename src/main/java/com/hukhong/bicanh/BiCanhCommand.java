package com.hukhong.bicanh;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** /hukhong bicanh <tier 1..5> | portal [tier] | leave  (gắn vào HuKhongCommand). Lệnh bỏ qua giới hạn cấp. */
public final class BiCanhCommand {
    private BiCanhCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("bicanh")
                .then(Commands.literal("leave").executes(c -> {
                    BiCanhManager.leave(c.getSource().getPlayerOrException(), "hukhong.bicanh.left");
                    return 1;
                }))
                .then(Commands.literal("portal")
                        .executes(c -> portal(c.getSource(), null))
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 5))
                                .executes(c -> portal(c.getSource(), BiCanhTier.byIndex(IntegerArgumentType.getInteger(c, "tier") - 1)))))
                .then(Commands.argument("tier", IntegerArgumentType.integer(1, 5)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BiCanhTier t = BiCanhTier.byIndex(IntegerArgumentType.getInteger(c, "tier") - 1);
                    List<ServerPlayer> party = p.serverLevel().getPlayers(o -> o != p && o.distanceTo(p) <= 8);
                    String err = BiCanhManager.enter(c.getSource().getServer(), p, t, party, true);
                    if (err != null) { c.getSource().sendFailure(Component.translatable(err, t.minLevel, t.maxLevel)); return 0; }
                    return 1;
                }));
    }

    private static int portal(CommandSourceStack src, BiCanhTier t) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        var pos = BiCanhEvents.spawnPortalNear(p.serverLevel(), p, t);
        if (pos == null) { src.sendFailure(Component.literal("no valid site")); return 0; }
        return 1;
    }
}
