package com.hukhong.society;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Lệnh Bước 14, gộp vào cây /hukhong (permission 2):
 * locate_city [id] · locate_sect [id] · sect join|leave|ch · city points.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SocietyCommands {
    private SocietyCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        var cityIds = CityDef.ALL.stream().map(CityDef::id).toList();
        var sectIds = SectDef.ALL.stream().map(SectDef::id).toList();
        e.getDispatcher().register(Commands.literal("hukhong").requires(s -> s.hasPermission(2))
                .then(Commands.literal("locate_city")
                        .executes(c -> CityService.locate(c.getSource(), null))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(cityIds, b))
                                .executes(c -> CityService.locate(c.getSource(), StringArgumentType.getString(c, "id")))))
                .then(Commands.literal("locate_sect")
                        .executes(c -> SectEvents.locate(c.getSource(), null))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(sectIds, b))
                                .executes(c -> SectEvents.locate(c.getSource(), StringArgumentType.getString(c, "id")))))
                .then(Commands.literal("sect")
                        .then(Commands.literal("join").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(sectIds, b))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    SectDef d = SectDef.byId(StringArgumentType.getString(c, "id"));
                                    if (d == null) { c.getSource().sendFailure(Component.translatable("hukhong.sect.unknown")); return 0; }
                                    p.setData(SocietyRegistry.SECT, SectService.get(p).withSect(d.id()));
                                    SectBuffs.refresh(p);
                                    return 1;
                                })))
                        .then(Commands.literal("leave").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            p.setData(SocietyRegistry.SECT, SectData.DEFAULT);
                            SectBuffs.refresh(p);
                            return 1;
                        }))
                        .then(Commands.literal("ch").then(Commands.argument("n", IntegerArgumentType.integer(0)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            SectData d = SectService.get(p);
                            p.setData(SocietyRegistry.SECT, d.withEarned(d.cycle(), IntegerArgumentType.getInteger(c, "n")));
                            SectBuffs.refresh(p);
                            return 1;
                        }))))
                .then(Commands.literal("city").then(Commands.literal("points")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            var ow = c.getSource().getServer().overworld();
                            CityDef d = CityService.nearest(ow, p.getX(), p.getZ(), 200);
                            if (d == null) { c.getSource().sendFailure(Component.translatable("hukhong.city.unknown")); return 0; }
                            return CityService.addPoints(p, ow, d, IntegerArgumentType.getInteger(c, "n"));
                        })))));
    }
}
