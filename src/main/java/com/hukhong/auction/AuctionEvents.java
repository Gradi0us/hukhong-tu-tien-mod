package com.hukhong.auction;

import com.hukhong.HuKhong;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Tick san Dau Gia Hanh (het han, NPC ky gui / mua lai) + lenh /thu, /daugia, /daugiahoi. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class AuctionEvents {
    private AuctionEvents() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % 200 != 61) return;
        try {
            AuctionService.tick(s, System.currentTimeMillis());
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().error("Auction tick failed: {}", ex.toString(), ex);
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        var d = e.getDispatcher();
        d.register(Commands.literal("thu").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            MailService.deliver(p);
            AuctionNet.sendMail(p);
            return 1;
        }));
        d.register(Commands.literal("daugia").requires(s -> s.hasPermission(2)).executes(c -> {
            AuctionNet.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        d.register(Commands.literal("daugiahoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("start").executes(c -> {
                    boolean ok = LiveAuction.start(c.getSource().getServer(), "manual-" + System.currentTimeMillis(), 30);
                    c.getSource().sendSystemMessage(net.minecraft.network.chat.Component.literal(ok ? "Live auction starting in 30s" : "Cannot start (already running or no hall built)"));
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("stop").executes(c -> { LiveAuction.stop(c.getSource().getServer()); return 1; })));
    }
}
