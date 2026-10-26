package com.hukhong.endgame;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** /hukhong endgame portal | enter | boss | choice | tier <n>  (gắn vào HuKhongCommand; bỏ qua mọi điều kiện). */
public final class EndgameCommand {
    private EndgameCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("endgame")
                .then(Commands.literal("portal").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel ow = c.getSource().getServer().getLevel(Level.OVERWORLD);
                    BlockPos g = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.blockPosition().offset(8, 0, 0));
                    if (p.serverLevel() != ow) { c.getSource().sendFailure(Component.literal("run in overworld")); return 0; }
                    EndgameService.openGateway(ow, g);
                    c.getSource().sendSuccess(() -> Component.literal("gateway at " + g.toShortString()), false);
                    return 1;
                }))
                .then(Commands.literal("enter").executes(c -> {
                    EndgameService.enter(c.getSource().getPlayerOrException(), true);
                    return 1;
                }))
                .then(Commands.literal("boss").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var b = EndgameService.spawnBoss(p.serverLevel(), p.blockPosition().offset(10, 0, 0));
                    c.getSource().sendSuccess(() -> Component.literal(b == null ? "spawn failed" : "boss spawned"), false);
                    return b == null ? 0 : 1;
                }))
                .then(Commands.literal("choice").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var q = com.hukhong.story.npc.DialogueService.quest(p);
                    if (!EndgameService.hasEnding(q))
                        com.hukhong.story.npc.DialogueService.setQuest(p, q.withFlag(EndgameService.F_PENDING));
                    EndgameService.promptChoice(p);
                    return 1;
                }))
                .then(Commands.literal("tier").then(Commands.argument("n", IntegerArgumentType.integer(EndgameKit.MIN_TIER, EndgameKit.MAX_TIER))
                        .executes(c -> {
                            int n = IntegerArgumentType.getInteger(c, "n");
                            WorldTier.force(c.getSource().getServer(), n);
                            c.getSource().sendSuccess(() -> Component.literal("world tier = " + WorldTier.get()
                                    + " (hp x" + WorldTier.hpMul() + ", dmg x" + WorldTier.dmgMul() + ", reward x" + WorldTier.rewardMul() + ")"), false);
                            return 1;
                        })));
    }
}
