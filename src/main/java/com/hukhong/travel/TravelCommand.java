package com.hukhong.travel;

import com.hukhong.city.CityDef;
import com.hukhong.society.SectDef;
import com.hukhong.story.flow.RequiredNpcs;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

import com.hukhong.HuKhong;

/** /hukhong travel <id|obj> (thu nghiem, bo qua kham pha + phi) va /hukhong quest npc (trieu hoi lai NPC tuyen chinh). Gop vao goc "hukhong". */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TravelCommand {
    private TravelCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hukhong")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("travel").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> {
                            List<String> ids = new ArrayList<>(List.of(TravelService.OBJ));
                            for (CityDef d : CityDef.ALL) ids.add(d.id());
                            for (SectDef d : SectDef.ALL) if (!d.hidden()) ids.add(d.id());
                            return SharedSuggestionProvider.suggest(ids, b);
                        })
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String err = TravelService.start(p, StringArgumentType.getString(c, "id"), true);
                            if (err != null) { c.getSource().sendFailure(Component.translatable(err)); return 0; }
                            return 1;
                        })))
                .then(Commands.literal("quest").then(Commands.literal("npc").executes(c ->
                        RequiredNpcs.resummon(c.getSource().getPlayerOrException()) ? 1 : 0))));
    }
}
