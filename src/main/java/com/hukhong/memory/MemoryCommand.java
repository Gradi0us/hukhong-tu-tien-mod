package com.hukhong.memory;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /hkkyuc list | give N | play N | reset — thử nghiệm Ký Ức (cần quyền op). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MemoryCommand {
    private MemoryCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hkkyuc").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MemoryData d = MemoryService.data(p);
                    for (MemoryLore.Memory m : MemoryLore.ALL) {
                        boolean has = d.has(m.id());
                        c.getSource().sendSuccess(() -> Tx.t("hukhong.memory.cmd.line", "[%s] %s - %s", "[%s] %s - %s",
                                has ? "x" : " ", MemoryLore.roman(m.id()), m.name().tr())
                                .withStyle(has ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), false);
                    }
                    c.getSource().sendSuccess(() -> Tx.t("hukhong.memory.cmd.count", "Đã thu thập: %s/%s", "Collected: %s/%s",
                            d.count(), MemoryLore.COUNT), false);
                    return d.count();
                }))
                .then(Commands.literal("give").then(Commands.argument("id", IntegerArgumentType.integer(1, MemoryLore.COUNT)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int id = IntegerArgumentType.getInteger(c, "id");
                    MemoryService.give(p, id);
                    c.getSource().sendSuccess(() -> Tx.t("hukhong.memory.cmd.given", "Đã trao Mảnh Ký Ức %s.", "Gave Memory Shard %s.",
                            MemoryLore.roman(id)), false);
                    return 1;
                })))
                .then(Commands.literal("play").then(Commands.argument("id", IntegerArgumentType.integer(0, MemoryLore.COUNT)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (MemoryScene.active(p)) {
                        c.getSource().sendFailure(Tx.t("hukhong.memory.busy", "Đang trong một ký ức khác, chưa thể nhập thêm.", "Another memory is still unfolding."));
                        return 0;
                    }
                    MemoryScene.start(p, IntegerArgumentType.getInteger(c, "id"), null); // xem thử, không ghi nhận
                    return 1;
                })))
                .then(Commands.literal("reset").executes(c -> {
                    MemoryService.reset(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Tx.t("hukhong.memory.cmd.reset", "Đã xóa hồ sơ Ký Ức.", "Memory record cleared."), false);
                    return 1;
                })));
    }
}
